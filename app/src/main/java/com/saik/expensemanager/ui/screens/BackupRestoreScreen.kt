package com.saik.expensemanager.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.documentfile.provider.DocumentFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.updateBackupFolder(it.toString())
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val csvData = viewModel.exportTransactionsToCSV()
                    outputStream.write(csvData.toByteArray())
                }
                Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val csvData = inputStream.bufferedReader().readText()
                    viewModel.importTransactionsFromCSV(csvData)
                }
                Toast.makeText(context, "Import successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    var availableBackups by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var isLoadingBackups by remember { mutableStateOf(false) }
    // Folder accessibility state: null = checking, empty string = not linked/inaccessible, non-empty = folder name
    var folderStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val refreshFolderStatus: () -> Unit = {
        scope.launch {
            folderStatus = withContext(Dispatchers.IO) {
                appSettings.backupFolderUri?.let { uriStr ->
                    try {
                        val root = DocumentFile.fromTreeUri(context, Uri.parse(uriStr))
                        if (root != null && root.canRead()) {
                            root.name ?: "Backup Folder"
                        } else {
                            "" // URI exists but folder inaccessible
                        }
                    } catch (e: Exception) {
                        "" // URI exists but folder inaccessible
                    }
                } ?: "" // No URI set
            }
        }
    }
    
    val refreshBackups = {
        scope.launch {
            isLoadingBackups = true
            val backups = withContext(Dispatchers.IO) {
                appSettings.backupFolderUri?.let { uriStr ->
                    try {
                        val root = DocumentFile.fromTreeUri(context, Uri.parse(uriStr))
                        if (root != null && root.canRead()) {
                            root.listFiles()
                                ?.filter { it.name?.startsWith("expense_backup_") == true }
                                ?.sortedByDescending { it.lastModified() }
                                ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            }
            availableBackups = backups
            isLoadingBackups = false
        }
    }

    LaunchedEffect(appSettings.backupFolderUri, appSettings.lastLocalBackupTime) {
        refreshFolderStatus()
        refreshBackups()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "Backup & Restore",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            // Quick Backup Card
            SecondaryCard {
                Column(
                    modifier = Modifier.padding(Dimens.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    val lastBackupStr = if (appSettings.lastLocalBackupTime > 0) {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(appSettings.lastLocalBackupTime))
                    } else "Never"
                    
                    Text("Last Backup: $lastBackupStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Button(
                        onClick = { 
                            if (appSettings.backupFolderUri == null) {
                                Toast.makeText(context, "Please link a backup folder first", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.triggerBackup(context)
                                Toast.makeText(context, "Backup initiated", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    kotlinx.coroutines.delay(500)
                                    refreshBackups()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderSpecial, null)
                            Text("Backup Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Automated Backup Config
            SectionHeader("Automation", modifier = Modifier.padding(horizontal = 0.dp))
            SecondaryCard {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Safely backup to local folder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = appSettings.localBackupEnabled,
                            onCheckedChange = { enabled ->
                                if (appSettings.backupFolderUri != null) {
                                    viewModel.updateLocalBackupEnabled(enabled)
                                } else {
                                    Toast.makeText(context, "Link a backup folder first", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (appSettings.localBackupEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpacingMedium))
                        Text("Backup Frequency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                        val frequencies = listOf(
                            "Daily" to 1,
                            "Weekly" to 7,
                            "Monthly" to 30
                        )

                        frequencies.forEach { (label, days) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appSettings.autoBackupFrequencyDays == days,
                                    onClick = {
                                        viewModel.updateAutoBackupFrequency(days)
                                    }
                                )
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Text(
                    "The latest 15 backups are kept; older ones are automatically removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall)
                )
            }

            // Linked Folder Card
            SecondaryCard {
                Row(
                    modifier = Modifier.padding(Dimens.SpacingLarge).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Backup Folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        when {
                            folderStatus == null -> {
                                Text("Checking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            folderStatus!!.isNotEmpty() -> {
                                Text(
                                    text = folderStatus!!,
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text("Linked & accessible", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                            }
                            appSettings.backupFolderUri != null -> {
                                Text("Folder not accessible", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            else -> {
                                Text("Not linked", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { folderLauncher.launch(null) },
                    modifier = Modifier.padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingMedium).fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (appSettings.backupFolderUri != null) "Change Folder" else "Link Folder", fontWeight = FontWeight.Bold)
                }
                
                if (appSettings.backupFolderUri != null) {
                    TextButton(onClick = { viewModel.updateBackupFolder(null) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Unlink Folder", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Export / Import Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                Button(
                    onClick = { exportLauncher.launch("expense_backup_${System.currentTimeMillis()}.csv") },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export")
                }
                Button(
                    onClick = { importLauncher.launch(arrayOf("text/csv")) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import")
                }
            }

            // Local Backups List
            SectionHeader("Local Backups", modifier = Modifier.padding(horizontal = 0.dp))
            SecondaryCard {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Saved Backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { refreshBackups() }) {
                            Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    if (appSettings.backupFolderUri == null) {
                        Text("Link a folder to view backups", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = Dimens.SpacingLarge))
                    } else if (isLoadingBackups) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpacingLarge), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (availableBackups.isEmpty()) {
                        Text("No backups found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = Dimens.SpacingLarge))
                    } else {
                        availableBackups.forEach { bFile ->
                            BackupFileItem(file = bFile) {
                                viewModel.restoreBackup(context, bFile.uri)
                                Toast.makeText(context, "Restoring...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BackupFileItem(file: DocumentFile, onRestore: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name ?: "Unknown", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(file.lastModified()))
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { showConfirm = true }) {
            Text("Restore", color = MaterialTheme.colorScheme.primary)
        }
    }
    
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Restore Backup?") },
            text = { Text("This will overwrite all current data with the contents of this backup. The app will restart.") },
            confirmButton = {
                Button(onClick = { onRestore(); showConfirm = false }) { Text("Confirm Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
