package com.saik.expensemanager.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.saik.expensemanager.data.backup.GoogleDriveService
import com.saik.expensemanager.ui.components.Dimens
import com.saik.expensemanager.ui.components.SecondaryCard
import com.saik.expensemanager.ui.components.SectionHeader
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .requestServerAuthCode("670380491283-jcs2hv7411t4s8qhvvhij64l4qjr0jvg.apps.googleusercontent.com")
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val loginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account?.email?.let { email ->
                viewModel.updateCloudBackupSettings(email = email)
                Toast.makeText(context, "Connected as $email", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    var cloudBackups by remember { mutableStateOf<List<com.google.api.services.drive.model.File>>(emptyList()) }
    var isLoadingBackups by remember { mutableStateOf(value = false) }

    val refreshBackups = {
        if (settings.googleAccountEmail != null) {
            scope.launch {
                isLoadingBackups = true
                val googleDriveService = GoogleDriveService(context, settings.googleAccountEmail!!)
                cloudBackups = googleDriveService.listBackups().sortedByDescending { it.createdTime?.value ?: 0L }
                isLoadingBackups = false
            }
        }
    }

    LaunchedEffect(settings.googleAccountEmail, settings.lastBackupTime) {
        refreshBackups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            // Google Account Section
            SecondaryCard {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("Google Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (settings.googleAccountEmail != null) {
                                Text(settings.googleAccountEmail!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Not connected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
                    Button(
                        onClick = {
                            if (settings.googleAccountEmail == null) {
                                loginLauncher.launch(googleSignInClient.signInIntent)
                            } else {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    viewModel.updateCloudBackupSettings(clearEmail = true, enabled = false)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (settings.googleAccountEmail == null) "Connect Google Drive" else "Disconnect Account")
                    }
                }
            }

            // Automatic Backup Section
            SecondaryCard {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Safely sync to cloud", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.cloudBackupEnabled,
                            onCheckedChange = { enabled ->
                                if (settings.googleAccountEmail != null) {
                                    viewModel.updateCloudBackupSettings(enabled = enabled)
                                } else {
                                    Toast.makeText(context, "Connect your Google account first", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (settings.cloudBackupEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpacingMedium))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Backup on any network", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("Allow backup on mobile data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.cloudBackupOnMetered,
                                onCheckedChange = { onMetered ->
                                    viewModel.updateCloudBackupSettings(onMetered = onMetered)
                                }
                            )
                        }
                    }
                }
            }

            // Frequency Selection
            if (settings.cloudBackupEnabled) {
                SecondaryCard {
                    Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
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
                                    selected = settings.autoBackupFrequencyDays == days,
                                    onClick = { 
                                        viewModel.updateCloudBackupSettings(frequencyDays = days)
                                    }
                                )
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            // Last Backup Status & Manual Backup
            SecondaryCard {
                Column(
                    modifier = Modifier.padding(Dimens.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val lastBackup = if (settings.lastBackupTime > 0) {
                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(settings.lastBackupTime))
                    } else "Never"
                    
                    Text("Last Cloud Backup: $lastBackup", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
                    
                    var isBackingUp by remember { mutableStateOf(false) }
                    
                    Button(
                        onClick = {
                            if (settings.googleAccountEmail != null) {
                                scope.launch {
                                    isBackingUp = true
                                    val success = viewModel.performManualCloudBackup(context)
                                    isBackingUp = false
                                    if (success) {
                                        Toast.makeText(context, "Backup successful", Toast.LENGTH_SHORT).show()
                                        refreshBackups()
                                    } else {
                                        Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Connect your Google account first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBackingUp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup now")
                        }
                    }
                }
            }

            // Cloud Backups List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Cloud Backups", modifier = Modifier.padding(horizontal = 0.dp))
                IconButton(onClick = { refreshBackups() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            SecondaryCard {
                if (isLoadingBackups) {
                    Box(modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingLarge), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (cloudBackups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingLarge), contentAlignment = Alignment.Center) {
                        Text("No backups found", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    Column(modifier = Modifier.padding(Dimens.SpacingMedium)) {
                        cloudBackups.forEach { file ->
                            CloudBackupItem(file = file) {
                                scope.launch {
                                    val success = viewModel.restoreFromCloud(context, file.id)
                                    if (!success) {
                                        Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudBackupItem(file: com.google.api.services.drive.model.File, onRestore: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name ?: "Unknown", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            val date = if (file.createdTime != null) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.createdTime.value))
            } else "Unknown date"
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { showConfirm = true }) {
            Text("Restore")
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Restore from Cloud?") },
            text = { Text("Current data will be replaced. The app will restart.") },
            confirmButton = {
                Button(onClick = { onRestore(); showConfirm = false }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
