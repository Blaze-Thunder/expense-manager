package com.saik.expensemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.saik.expensemanager.ui.viewmodel.MainViewModel

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel? = null,
    onFinishOnboarding: (name: String, limit: Double, currency: String) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedCurrency by remember { mutableStateOf("₹") }
    var budgetBookName by remember { mutableStateOf("Personal Budget") }
    var monthlyBudgetStr by remember { mutableStateOf("2000") }
    var budgetError by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel?.restoreBackup(context, it)
            Toast.makeText(context, "Restoring backup...", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            restoreLauncher.launch(arrayOf("*/*"))
        } else {
            Toast.makeText(context, "Permission denied. Cannot access backup file.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "onboarding_steps"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentStep) {
                        1 -> WelcomeStep(
                            onNext = { step = 2 },
                            onRestore = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    restoreLauncher.launch(arrayOf("*/*"))
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        )
                        2 -> CurrencyStep(
                            selectedCurrency = selectedCurrency,
                            onCurrencySelected = { selectedCurrency = it },
                            onNext = { step = 3 }
                        )
                        3 -> BudgetSetupStep(
                            bookName = budgetBookName,
                            onNameChange = {
                                budgetBookName = it
                                nameError = ""
                            },
                            budgetStr = monthlyBudgetStr,
                            onBudgetChange = {
                                monthlyBudgetStr = it
                                budgetError = ""
                            },
                            nameError = nameError,
                            budgetError = budgetError,
                            selectedCurrency = selectedCurrency,
                            onFinish = {
                                if (budgetBookName.trim().isEmpty()) {
                                    nameError = "Budget name cannot be empty"
                                    return@BudgetSetupStep
                                }
                                val bVal = monthlyBudgetStr.toDoubleOrNull()
                                if (bVal == null || bVal <= 0.0) {
                                    budgetError = "Enter a valid positive budget amount"
                                    return@BudgetSetupStep
                                }
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onFinishOnboarding(budgetBookName.trim(), bVal, selectedCurrency)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.WelcomeStep(onNext: () -> Unit, onRestore: () -> Unit) {
    Spacer(modifier = Modifier.weight(1f))
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Paid,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(64.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Welcome to Expense Tracker",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Your comprehensive personal finance assistant. Track expenses, manage credit card billing cycles, analyze spending statistics, and align with your savings goals.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.weight(1.2f))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("get_started_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }

        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("restore_backup_button"),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore from Backup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ColumnScope.CurrencyStep(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Spacer(modifier = Modifier.weight(0.5f))

    Text(
        text = "Choose Currency",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    )

    Text(
        text = "Select your default currency. All financial amounts across your budget books and account statements will use this symbol.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
    )

    val currencies = listOf(
        "₹" to "Indian Rupee (₹)",
        "$" to "US Dollar ($)",
        "€" to "Euro (€)",
        "£" to "British Pound (£)",
        "¥" to "Yen / Yuan / Peso (¥)",
        "A$" to "Australian Dollar (A$)",
        "C$" to "Canadian Dollar (C$)",
        "CHF" to "Swiss Franc (CHF)",
        "AED" to "UAE Dirham (AED)",
        "SGD" to "Singapore Dollar (SGD)",
        "₩" to "South Korean Won (₩)",
        "৳" to "Bangladeshi Taka (৳)"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        currencies.forEach { (symbol, label) ->
            val isSelected = selectedCurrency == symbol
            Card(
                onClick = { onCurrencySelected(symbol) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                     else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .testTag("currency_${symbol}_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("currency_next_button"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ColumnScope.BudgetSetupStep(
    bookName: String,
    onNameChange: (String) -> Unit,
    budgetStr: String,
    onBudgetChange: (String) -> Unit,
    nameError: String,
    budgetError: String,
    selectedCurrency: String,
    onFinish: () -> Unit
) {
    Spacer(modifier = Modifier.weight(0.3f))

    Text(
        text = "Setup First Budget",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    )

    Text(
        text = "Create your initial budget book. This represents the primary account container where your transactions are organized.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
    )

    OutlinedTextField(
        value = bookName,
        onValueChange = onNameChange,
        label = { Text("Budget Book Name") },
        placeholder = { Text("e.g. Personal Expenses") },
        isError = nameError.isNotEmpty(),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_name_input"),
        shape = RoundedCornerShape(12.dp)
    )
    if (nameError.isNotEmpty()) {
        Text(
            text = nameError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = budgetStr,
        onValueChange = onBudgetChange,
        label = { Text("Monthly Budget Amount") },
        placeholder = { Text("e.g. 1000") },
        prefix = { Text("$selectedCurrency ") },
        isError = budgetError.isNotEmpty(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_amount_input"),
        shape = RoundedCornerShape(12.dp)
    )
    if (budgetError.isNotEmpty()) {
        Text(
            text = budgetError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
        )
    }

    Spacer(modifier = Modifier.weight(0.8f))

    Button(
        onClick = onFinish,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("finish_onboarding_button"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Build Budget Book", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
