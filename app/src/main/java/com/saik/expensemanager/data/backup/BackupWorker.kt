package com.saik.expensemanager.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saik.expensemanager.data.db.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val backupType = inputData.getString("backup_type")
        val database = AppDatabase.getDatabase(applicationContext)
        val settingsDao = database.appSettingsDao()
        val settings = settingsDao.getAppSettings() ?: return Result.success()

        var anySucceeded = false
        var anyAttempted = false

        // 1. Local Backup
        if (((backupType == "local") || (backupType == null)) && 
            settings.localBackupEnabled && (settings.autoBackupFrequencyDays > 0) && (settings.backupFolderUri != null)) {
            
            anyAttempted = true
            try {
                // checkpoint is only needed for WAL mode, but we use TRUNCATE
                // AppDatabase.checkpoint(applicationContext) 
                
                val dbFile = applicationContext.getDatabasePath("expense_tracker_db")
                if (!dbFile.exists()) {
                    // db file doesn't exist - not a failure, just nothing to back up
                } else {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val backupFileName = "expense_backup_$timestamp.db"
                    
                    val root = DocumentFile.fromTreeUri(applicationContext, settings.backupFolderUri.toUri())
                    if ((root != null) && root.canWrite()) {
                        val backupFile = root.createFile("application/x-sqlite3", backupFileName)
                        
                        if (backupFile != null) {
                            applicationContext.contentResolver.openOutputStream(backupFile.uri)?.use { out ->
                                dbFile.inputStream().use { it.copyTo(out) }
                            }
                            
                            settingsDao.updateLastLocalBackupTime(System.currentTimeMillis())
                            anySucceeded = true
                            
                            // Cleanup old backups (keep only latest 15)
                            val files = root.listFiles()
                                .filter { it.name?.startsWith("expense_backup_") == true }
                                .sortedByDescending { it.lastModified() }
                            
                            if (files.size > 15) {
                                files.drop(15).forEach { it.delete() }
                            }
                        }
                    } else {
                        settingsDao.clearBackupFolderUri()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Cloud Backup
        if (((backupType == "cloud") || (backupType == null)) &&
            settings.cloudBackupEnabled && (settings.googleAccountEmail != null)) {

            anyAttempted = true
            val googleDriveService = GoogleDriveService(applicationContext, settings.googleAccountEmail)
            try {
                val dbFile = applicationContext.getDatabasePath("expense_tracker_db")
                if (dbFile.exists()) {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val backupName = "expense_backup_$timestamp.db"
                    
                    val tempFile = File(applicationContext.cacheDir, backupName)
                    dbFile.copyTo(tempFile, overwrite = true)

                    val description = "Automatic backup on $timestamp"
                    val fileId = googleDriveService.uploadBackup(tempFile, description)
                    
                    tempFile.delete()

                    if (fileId != null) {
                        settingsDao.updateLastBackupTime(System.currentTimeMillis())
                        googleDriveService.deleteOldBackups(15)
                        anySucceeded = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Final result handling:
        // - If nothing was enabled/attempted -> Success (nothing to do)
        // - If something was attempted and succeeded -> Success
        // - If something was attempted but failed -> Retry (up to 5 times)
        
        return when {
            !anyAttempted || anySucceeded -> Result.success()
            runAttemptCount < 5 -> Result.retry()
            else -> Result.failure()
        }
    }
}
