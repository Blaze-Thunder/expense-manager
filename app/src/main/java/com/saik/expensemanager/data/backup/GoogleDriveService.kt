package com.saik.expensemanager.data.backup

import android.content.Context
import android.accounts.Account
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Collections

class GoogleDriveService(private val context: Context, private val accountEmail: String) {

    companion object {
        private val HTTP_TRANSPORT = NetHttpTransport()
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    }

    private val driveService: Drive by lazy {
        if (accountEmail.isBlank()) {
            throw IllegalArgumentException("Account email is blank")
        }
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context.applicationContext,
            Collections.singleton(DriveScopes.DRIVE_FILE),
        ).apply {
            selectedAccount = Account(accountEmail, "com.google")
        }

        Drive.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            credential,
        ).setApplicationName("Expense Manager").build()
    }

    private suspend fun getOrCreateBackupFolder(): String? = withContext(Dispatchers.IO) {
        try {
            // Check if folder exists
            val result = driveService.files().list()
                .setQ("name = 'Expense Manager' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            result.files?.firstOrNull()?.id?.let { return@withContext it }

            // Create new folder
            val folderMetadata = File().apply {
                name = "Expense Manager"
                mimeType = "application/vnd.google-apps.folder"
            }
            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()
            folder.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun uploadBackup(backupFile: java.io.File, description: String): String? = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateBackupFolder() ?: return@withContext null

            val fileMetadata = File().apply {
                name = backupFile.name
                mimeType = "application/x-sqlite3"
                this.description = description
                parents = listOf(folderId)
            }
            val mediaContent = FileContent("application/x-sqlite3", backupFile)

            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            file.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun listBackups(): List<File> = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateBackupFolder() ?: return@withContext emptyList()

            val result = driveService.files().list()
                .setQ("'$folderId' in parents and name contains 'expense_backup' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name, createdTime, description, size)")
                .setOrderBy("createdTime desc")
                .execute()
            result.files ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteOldBackups(maxBackups: Int = 15) = withContext(Dispatchers.IO) {
        try {
            val backups = listBackups() // Already sorted desc by createdTime
            if (backups.size > maxBackups) {
                val toDelete = backups.subList(maxBackups, backups.size)
                toDelete.forEach { file ->
                    driveService.files().delete(file.id).execute()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun downloadBackup(fileId: String, targetFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(targetFile).use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
