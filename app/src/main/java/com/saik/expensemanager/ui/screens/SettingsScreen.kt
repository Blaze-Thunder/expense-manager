package com.saik.expensemanager.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.CategoryAlert
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAccountsManagement: () -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToRecurringManagement: () -> Unit = {},
    onNavigateToBudgetBooksManagement: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToCloudBackup: () -> Unit = {},
    onNavigateToGiftCards: () -> Unit = {},
    onNavigateToManageTemplates: () -> Unit = {},
    onNavigateToAppLockSettings: () -> Unit = {}
) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val budgetBooks by viewModel.budgetBooks.collectAsStateWithLifecycle()
    val alerts by viewModel.allCategoryAlerts.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val csvData = reader.readText()
                    viewModel.importTransactionsFromCSV(csvData)
                }
                Toast.makeText(context, "Import successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val templateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val template = "Date,Amount,Type,Category,Subcategory,Description,AccountName,ToAccountName,BudgetBookName,IsRecurringTemplate,Frequency,Interval,ReflectionTiming,EndDate,Occurrences,DayOfWeek,DayOfMonth\n" +
                                 "2023-10-27 10:00:00,500.0,EXPENSE,Food,Groceries,Lunch,Cash,,Main Book,false,,,,,,,"
                    outputStream.write(template.toByteArray())
                }
                Toast.makeText(context, "Template saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save template: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showFooterDialogText by remember { mutableStateOf<String?>(null) }
    var footerDialogTitle by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { 
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            // SECTION 1: APP CONFIGURATION
            SectionHeader("App Configuration", modifier = Modifier.padding(horizontal = 0.dp))
            SecondaryCard {
                SettingsRow(
                    title = "Active Language",
                    subtitle = appSettings.language,
                    trailing = {
                        var langExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = langExpanded,
                            onExpandedChange = { langExpanded = it }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier
                                    .menuAnchor()
                                    .clickable { langExpanded = true }
                            ) {
                                Text(appSettings.language, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            ExposedDropdownMenu(
                                expanded = langExpanded,
                                onDismissRequest = { langExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("English", "Spanish", "French", "German", "Hindi").forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang, style = MaterialTheme.typography.bodyLarge) },
                                        onClick = { 
                                            viewModel.updateSettings(appSettings.theme, appSettings.colorTheme, lang, appSettings.budgetAlertThreshold, appSettings.skipExitConfirmation)
                                            langExpanded = false 
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    title = "Application Theme",
                    subtitle = appSettings.theme,
                    trailing = {
                        var themeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = themeExpanded,
                            onExpandedChange = { themeExpanded = it }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier
                                    .menuAnchor()
                                    .clickable { themeExpanded = true }
                            ) {
                                val displayTheme = remember(appSettings.theme) {
                                    appSettings.theme.lowercase().replaceFirstChar { it.uppercase() }
                                }
                                Text(displayTheme, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            ExposedDropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .widthIn(min = 140.dp)
                            ) {
                                listOf("System", "Light", "Dark").forEach { theme ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = theme, 
                                                style = MaterialTheme.typography.bodyLarge,
                                                softWrap = false
                                            ) 
                                        },
                                        onClick = { 
                                            viewModel.updateSettings(theme.uppercase(), appSettings.colorTheme, appSettings.language, appSettings.budgetAlertThreshold, appSettings.skipExitConfirmation)
                                            themeExpanded = false 
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    title = "Categories Management",
                    subtitle = "${categories.filter { it.parentId == null }.size} primary financial categories",
                    onClick = onNavigateToCategoryManagement
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    title = "Budget Books & Limits",
                    subtitle = "${budgetBooks.size} active books",
                    onClick = onNavigateToBudgetBooksManagement
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    title = "Skip Exit Confirmation",
                    subtitle = "Close app immediately without asking",
                    trailing = {
                        Switch(checked = appSettings.skipExitConfirmation, onCheckedChange = { viewModel.updateSettings(appSettings.theme, appSettings.colorTheme, appSettings.language, appSettings.budgetAlertThreshold, it) })
                    }
                )
            }

            // SECTION 2: ACCOUNTS MANAGEMENT
            SectionHeader("Accounts Management", modifier = Modifier.padding(horizontal = 0.dp))
            SecondaryCard {
                SettingsRow(
                    title = "Accounts",
                    subtitle = "Manage all accounts",
                    onClick = onNavigateToAccountsManagement
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    title = "Gift Cards Tracker",
                    subtitle = "Manage gift cards and vouchers",
                    onClick = onNavigateToGiftCards
                )
            }

            // SECTION 3: ALERTS & AUTOMATION
            SectionHeader("Alerts & Automation", modifier = Modifier.padding(horizontal = 0.dp))
            SecondaryCard {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Global Budget Alert Threshold", style = MaterialTheme.typography.titleMedium)
                        Text(String.format(Locale.US, "%.0f%%", appSettings.budgetAlertThreshold * 100), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Warn me when total monthly budget spending crosses this fraction of limit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = appSettings.budgetAlertThreshold.toFloat(), onValueChange = { viewModel.updateSettings(appSettings.theme, appSettings.colorTheme, appSettings.language, it.toDouble(), appSettings.skipExitConfirmation) }, valueRange = 0.5f..1.0f, steps = 5)
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    var showCategoryAlertEditor by remember { mutableStateOf(false) }
                    SettingsRow(
                        title = "Category Caps & Warning Alerts",
                        subtitle = "Set unique maximum spend limits for categories.",
                        onClick = { showCategoryAlertEditor = !showCategoryAlertEditor },
                        trailing = { Icon(if (showCategoryAlertEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) },
                        padding = 0.dp
                    )
                    if (showCategoryAlertEditor) {
                        CategoryAlertsEditorSection(allCategories = categories, alerts = alerts, currencySymbol = appSettings.currencyCode, onSaveAlert = { name, amt -> viewModel.setCategoryAlert(name, amt, true) }, onDeleteAlert = { viewModel.deleteCategoryAlert(it) })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsRow(
                        title = "Recurring Transactions",
                        subtitle = "Manage templates for automatic record generation",
                        onClick = onNavigateToRecurringManagement,
                        padding = 0.dp
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsRow(
                        title = "Quick Templates",
                        subtitle = "Manage templates for one-tap transaction entry",
                        onClick = onNavigateToManageTemplates,
                        padding = 0.dp
                    )
                }
            }

            // SECTION 4: DATA & SECURITY
            SectionHeader("Data & Security", modifier = Modifier.padding(horizontal = 0.dp))
            SecondaryCard {
                SettingsRow(
                    title = "Export Financial Records", 
                    subtitle = "Saves records to CSV format in storage.", 
                    onClick = { exportLauncher.launch("expense_manager_export_${System.currentTimeMillis()}.csv") }, 
                    trailing = { Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                SettingsRow(
                    title = "Import Transactions", 
                    subtitle = "Pick a CSV file to upload records.", 
                    onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv")) }, 
                    trailing = { Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.primary) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                SettingsRow(
                    title = "Download Import Template", 
                    subtitle = "Get the CSV structure for importing.", 
                    onClick = { templateLauncher.launch("transaction_import_template.csv") }, 
                    trailing = { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    title = "Cloud Automatic Backups", 
                    subtitle = "Sync records securely to cloud servers.", 
                    onClick = onNavigateToCloudBackup,
                    trailing = { Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(title = "Backup & Restore", subtitle = "Manage local backups and data restoration.", onClick = onNavigateToBackupRestore, trailing = { Icon(Icons.Default.BackupTable, null, tint = MaterialTheme.colorScheme.primary) })

                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                SettingsRow(
                    title = "App Lock",
                    subtitle = "Manage PIN and biometric security",
                    onClick = onNavigateToAppLockSettings
                )
            }

            // FOOTER
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpacingLarge), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                Text("Expense Tracker v2.6.5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FooterBrandLink("Terms", "Terms of Use Dialog", "Terms content...") { title, text -> footerDialogTitle = title; showFooterDialogText = text }
                    FooterBrandLink("Privacy", "Privacy Guidelines", "Privacy content...") { title, text -> footerDialogTitle = title; showFooterDialogText = text }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.SpacingExtraLarge))
        }
    }

    if (showFooterDialogText != null) {
        AlertDialog(onDismissRequest = { showFooterDialogText = null }, title = { Text(footerDialogTitle, fontWeight = FontWeight.Bold) }, text = { Text(showFooterDialogText!!) }, confirmButton = { Button(onClick = { showFooterDialogText = null }) { Text("OK") } })
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    padding: androidx.compose.ui.unit.Dp = Dimens.SpacingLarge
) {
    val modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = modifier.fillMaxWidth().padding(padding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke() ?: if (onClick != null) {
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        } else {
            // No default trailing content
        }
    }
}

@Composable
fun FooterBrandLink(label: String, title: String, dialogText: String, onTrigger: (String, String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onTrigger(title, dialogText) }.padding(Dimens.SpacingSmall))
}

@Composable
fun CategoryAlertsEditorSection(
    allCategories: List<Category>,
    alerts: List<CategoryAlert>,
    currencySymbol: String,
    onSaveAlert: (name: String, amount: Double) -> Unit,
    onDeleteAlert: (CategoryAlert) -> Unit
) {
    var selectedCategoryAlertName by remember { mutableStateOf("") }
    var limitInputStr by remember { mutableStateOf("") }
    val expenseCategories = remember(allCategories) { allCategories.filter { it.type == "EXPENSE" } }

    LaunchedEffect(expenseCategories) {
        if (expenseCategories.isNotEmpty() && selectedCategoryAlertName.isEmpty()) {
            selectedCategoryAlertName = expenseCategories.first().name
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium), modifier = Modifier.fillMaxWidth()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(Dimens.SpacingMedium), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                Text("Configure Spend Warning Alert Cap:", style = MaterialTheme.typography.titleSmall)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(value = selectedCategoryAlertName, onValueChange = {}, readOnly = true, label = { Text("Select Expense Category") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }, modifier = Modifier.fillMaxWidth().clickable { expanded = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline))
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        expenseCategories.forEach { cat -> DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategoryAlertName = cat.name; expanded = false }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = limitInputStr, onValueChange = { limitInputStr = it }, label = { Text("Threshold") }, prefix = { Text("$currencySymbol ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    Button(onClick = {
                        val limit = limitInputStr.toDoubleOrNull() ?: 0.0
                        if (selectedCategoryAlertName.isNotEmpty() && limit > 0.0) { onSaveAlert(selectedCategoryAlertName, limit); limitInputStr = "" }
                    }) { Text("Save") }
                }
            }
        }

        alerts.forEach { alert ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.SpacingSmall), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Text(alert.categoryName, style = MaterialTheme.typography.titleSmall)
                    Text("capped at $currencySymbol${alert.limitAmount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDeleteAlert(alert) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}
