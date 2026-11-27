package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.RepetitiveTransaction
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.data.model.GiftCard
import androidx.compose.foundation.shape.RoundedCornerShape
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecurringTransactionsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val recurringTransactions by viewModel.allRepetitiveTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val budgetBooks by viewModel.budgetBooks.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()

    var editingRep by remember { mutableStateOf<RepetitiveTransaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "Recurring Transactions",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { innerPadding ->
        if (recurringTransactions.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recurring transactions found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(start = Dimens.SpacingLarge, top = Dimens.SpacingLarge, end = Dimens.SpacingLarge, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                items(recurringTransactions) { rep ->
                    RecurringTransactionItem(
                        rep = rep,
                        categories = categories,
                        accounts = accounts,
                        budgetBooks = budgetBooks,
                        currencySymbol = appSettings.currencyCode,
                        onClick = { editingRep = rep },
                        onDelete = { viewModel.deleteRepetitiveTransaction(rep) }
                    )
                }
            }
        }

        if (showAddDialog) {
            // Reusing TransactionEntryScreen for adding recurring is possible but complex due to state
            // For now, let's assume the user uses the FAB in TransactionEntryScreen or we create a dedicated dialog
            // To simplify, I'll provide a basic Add dialog here or navigate back to TransactionEntry with a flag
            // But requirement said "ability to add, update, delete, rename"
            AddEditRecurringDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { /* Logic for adding */ },
                categories = categories,
                accounts = accounts,
                viewModel = viewModel
            )
        }

        if (editingRep != null) {
            AddEditRecurringDialog(
                rep = editingRep,
                onDismiss = { editingRep = null },
                onConfirm = { updated -> viewModel.updateRepetitiveTransaction(updated) },
                categories = categories,
                accounts = accounts,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun RecurringTransactionItem(
    rep: RepetitiveTransaction,
    categories: List<Category>,
    accounts: List<Account>,
    budgetBooks: List<BudgetBook>,
    currencySymbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val parentCat = categories.find { it.name == rep.category && it.parentId == null }
    val subCat = if (!rep.subCategory.isNullOrEmpty()) {
        categories.find { it.name == rep.subCategory && it.parentId == parentCat?.id }
            ?: categories.find { it.name == rep.subCategory && it.parentId != null }
    } else null
    
    val displayCat = subCat ?: parentCat ?: categories.find { it.name == rep.category }
    val account = accounts.find { it.id == rep.linkedAccountId }
    val toAccount = accounts.find { it.id == rep.toAccountId }
    val budgetBook = budgetBooks.find { it.id == rep.linkedBudgetBookId }

    SecondaryCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(Dimens.SpacingLarge).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            val colorInt = (displayCat?.color ?: 0xFF9E9E9E).toInt()
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(colorInt).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (rep.type == "TRANSFER") {
                    Icon(Icons.Default.SwapHoriz, null, tint = Color(colorInt), modifier = Modifier.size(20.dp))
                } else {
                    val iconName = displayCat?.iconName ?: "Label"
                    val isEmoji = iconName.isNotEmpty() && !CategoryUtils.iconList.contains(iconName)
                    if (isEmoji) {
                        Text(text = iconName, fontSize = 20.sp)
                    } else {
                        Icon(
                            imageVector = CategoryUtils.getCategoryIcon(iconName),
                            contentDescription = null,
                            tint = Color(colorInt),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(rep.description.ifEmpty { if (rep.type == "TRANSFER") "Transfer" else rep.category }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val categoryText = if (!rep.subCategory.isNullOrEmpty()) "${rep.category} • ${rep.subCategory}" else rep.category
                Text("$categoryText • ${CurrencyUtils.formatAmount(rep.amount, currencySymbol)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (toAccount != null) "${account?.name} → ${toAccount.name}" else account?.name ?: "Unknown Account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (budgetBook != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = budgetBook.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEditRecurringDialog(
    rep: RepetitiveTransaction? = null,
    onDismiss: () -> Unit,
    onConfirm: (RepetitiveTransaction) -> Unit,
    categories: List<Category>,
    accounts: List<Account>,
    viewModel: MainViewModel
) {
    var description by remember { mutableStateOf(rep?.description ?: "") }
    var amountStr by remember { mutableStateOf(rep?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(rep?.type ?: "EXPENSE") }
    var categoryName by remember { mutableStateOf(rep?.category ?: "") }
    var subCategoryName by remember { mutableStateOf(rep?.subCategory) }
    var accountId by remember { mutableLongStateOf(rep?.linkedAccountId ?: accounts.firstOrNull()?.id ?: -1L) }
    var toAccountId by remember { mutableLongStateOf(rep?.toAccountId ?: -1L) }
    var frequency by remember { mutableStateOf(rep?.frequency ?: "MONTHLY") }
    var interval by remember { mutableIntStateOf(rep?.interval ?: 1) }
    var dayOfWeek by remember { mutableStateOf(rep?.dayOfWeek) }
    var dayOfMonth by remember { mutableStateOf(rep?.dayOfMonth) }
    var reflectionTiming by remember { mutableStateOf(rep?.reflectionTiming ?: "ON_DATE") }
    var endDate by remember { mutableStateOf(rep?.endDate) }
    var occurrences by remember { mutableStateOf(rep?.occurrences) }
    var linkedGiftCardId by remember { mutableLongStateOf(rep?.linkedGiftCardId ?: -1L) }
    
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()
    var linkedBudgetBookId by remember { mutableLongStateOf(rep?.linkedBudgetBookId ?: activeBook?.id ?: -1L) }

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val allGiftCards by viewModel.allGiftCards.collectAsStateWithLifecycle()
    val budgetBooks by viewModel.budgetBooks.collectAsStateWithLifecycle()
    val currencySim = appSettings.currencyCode

    val descriptionFocusRequester = remember { FocusRequester() }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showBudgetBookDropdown by remember { mutableStateOf(false) }

    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }

    val parentCat = categories.find { it.name == categoryName && it.parentId == null }
    val subCat = if (subCategoryName != null) {
        categories.find { it.name == subCategoryName && it.parentId == parentCat?.id }
    } else null
    val displayCat = subCat ?: parentCat
    val dialogIconName = displayCat?.iconName ?: "Label"
    val isDialogIconEmoji = dialogIconName.isNotEmpty() && !CategoryUtils.iconList.contains(dialogIconName)
    val dialogIconColor = displayCat?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(dialogIconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDialogIconEmoji) {
                        Text(text = dialogIconName, fontSize = 20.sp)
                    } else {
                        Icon(CategoryUtils.getCategoryIcon(dialogIconName), null, tint = dialogIconColor, modifier = Modifier.size(20.dp))
                    }
                }
                Text(if (rep == null) "Add Recurring" else "Edit Recurring") 
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                OutlinedTextField(
                    value = description, 
                    onValueChange = { 
                        description = it
                        if (descriptionError != null) descriptionError = null
                    }, 
                    label = { Text("Description / Name") }, 
                    isError = descriptionError != null,
                    supportingText = { descriptionError?.let { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(descriptionFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = amountStr, 
                    onValueChange = { 
                        amountStr = it
                        if (amountError != null) amountError = null
                    }, 
                    label = { Text("Amount") }, 
                    prefix = { Text(currencySim) },
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (categoryName.isEmpty()) {
                                showCategoryDropdown = true
                            } else if (accountId == -1L) {
                                showAccountDropdown = true
                            } else {
                                descriptionFocusRequester.requestFocus()
                            }
                        }
                    )
                )
                
                SegmentedPeriodSelector(
                    modes = listOf("Expense", "Income"),
                    selectedMode = type.lowercase().replaceFirstChar { it.uppercase() },
                    onModeSelected = { type = it.uppercase() }
                )

                Column {
                    OutlinedTextField(
                        value = if (subCategoryName != null) "$categoryName > $subCategoryName" else categoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        isError = categoryError != null,
                        modifier = Modifier.fillMaxWidth().clickable { showCategoryDropdown = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = if (categoryError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            disabledLabelColor = if (categoryError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    categoryError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                }
                if (showCategoryDropdown) {
                    CategorySelectorSheet(
                        viewModel = viewModel,
                        type = type,
                        initialParentName = categoryName,
                        initialSubCategoryName = subCategoryName,
                        onSelect = { name: String, sub: String? ->
                            categoryName = name
                            subCategoryName = sub
                            categoryError = null
                            showCategoryDropdown = false
                            if (accountId == -1L) {
                                showAccountDropdown = true
                            } else {
                                descriptionFocusRequester.requestFocus()
                            }
                        },
                        onDismiss = { showCategoryDropdown = false }
                    )
                }

                // From Account Selection
                val selectedAccount = accounts.find { it.id == accountId }
                Column {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Account") },
                        isError = accountError != null,
                        leadingIcon = selectedAccount?.let { acc ->
                            { Icon(AccountUtils.getAccountIcon(acc.iconName), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth().clickable { showAccountDropdown = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = if (accountError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            disabledLabelColor = if (accountError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    accountError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                }

                if (showAccountDropdown) {
                    AccountSelectorSheet(
                        accounts = accounts,
                        viewModel = viewModel,
                        currencySim = currencySim,
                        selectedAccountId = accountId,
                        onSelect = {
                            accountId = it.id
                            accountError = null
                            showAccountDropdown = false
                            descriptionFocusRequester.requestFocus()
                        },
                        onDismiss = { showAccountDropdown = false }
                    )
                }

                // Budget Book Selection
                val selectedBook = budgetBooks.find { it.id == linkedBudgetBookId }
                OutlinedTextField(
                    value = selectedBook?.name ?: "All Budget Books",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Link to Budget Book") },
                    leadingIcon = { Icon(Icons.Default.Book, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth().clickable { showBudgetBookDropdown = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (showBudgetBookDropdown) {
                    ModalBottomSheet(
                        onDismissRequest = { showBudgetBookDropdown = false },
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                "Select Budget Book",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(Dimens.SpacingLarge)
                            )
                            LazyColumn {
                                item {
                                    ListItem(
                                        headlineContent = { Text("All Budget Books (Global)") },
                                        leadingContent = { Icon(Icons.Default.Public, null) },
                                        modifier = Modifier.clickable {
                                            linkedBudgetBookId = -1L
                                            showBudgetBookDropdown = false
                                        }
                                    )
                                }
                                items(budgetBooks) { book ->
                                    ListItem(
                                        headlineContent = { Text(book.name) },
                                        leadingContent = { Icon(Icons.Default.Book, null) },
                                        trailingContent = {
                                            val limit = book.monthlyLimit
                                            Text(
                                                if (limit != null) CurrencyUtils.formatAmount(limit, currencySim) else "Spend Tracking",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            linkedBudgetBookId = book.id
                                            showBudgetBookDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                RecurrenceOptionsView(
                    frequency = frequency,
                    onFrequencyChange = { frequency = it },
                    interval = interval,
                    onIntervalChange = { interval = it },
                    endDate = endDate,
                    onEndDateChange = { endDate = it },
                    occurrences = occurrences,
                    onOccurrencesChange = { occurrences = it },
                    dayOfWeek = dayOfWeek,
                    onDayOfWeekChange = { dayOfWeek = it },
                    dayOfMonth = dayOfMonth,
                    onDayOfMonthChange = { dayOfMonth = it },
                    reflectionTiming = reflectionTiming,
                    onReflectionTimingChange = { reflectionTiming = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                descriptionError = null
                amountError = null
                categoryError = null
                accountError = null

                val amount = amountStr.trim().toDoubleOrNull() ?: 0.0
                var hasError = false

                if (description.trim().isEmpty()) {
                    descriptionError = "Description is required"
                    hasError = true
                }
                if (amount <= 0) {
                    amountError = "Enter a valid amount greater than 0"
                    hasError = true
                }
                if (categoryName.isEmpty()) {
                    categoryError = "Select a category"
                    hasError = true
                }
                if (accountId == -1L) {
                    accountError = "Select an account"
                    hasError = true
                }

                if (!hasError) {
                    val newRep = if (rep == null) {
                        RepetitiveTransaction(
                            startDate = System.currentTimeMillis(),
                            amount = amount,
                            type = type,
                            category = categoryName,
                            subCategory = subCategoryName,
                            frequency = frequency,
                            interval = interval,
                            linkedAccountId = accountId,
                            toAccountId = if (type == "TRANSFER") toAccountId else null,
                            linkedBudgetBookId = linkedBudgetBookId,
                            description = description,
                            endDate = endDate,
                            occurrences = occurrences,
                            dayOfWeek = dayOfWeek,
                            dayOfMonth = dayOfMonth,
                            reflectionTiming = reflectionTiming,
                            linkedGiftCardId = if (linkedGiftCardId != -1L) linkedGiftCardId else null
                        )
                    } else {
                        rep.copy(
                            amount = amount,
                            type = type,
                            category = categoryName,
                            subCategory = subCategoryName,
                            frequency = frequency,
                            interval = interval,
                            linkedAccountId = accountId,
                            toAccountId = if (type == "TRANSFER") toAccountId else null,
                            linkedBudgetBookId = linkedBudgetBookId,
                            description = description,
                            endDate = endDate,
                            occurrences = occurrences,
                            dayOfWeek = dayOfWeek,
                            dayOfMonth = dayOfMonth,
                            reflectionTiming = reflectionTiming,
                            linkedGiftCardId = if (linkedGiftCardId != -1L) linkedGiftCardId else null
                        )
                    }
                    if (rep == null) {
                        viewModel.addRepetitiveTransaction(
                            newRep.startDate, newRep.amount, newRep.type, newRep.category, 
                            newRep.frequency, newRep.interval, newRep.linkedAccountId, newRep.linkedBudgetBookId,
                            newRep.description, newRep.endDate, newRep.occurrences, newRep.dayOfWeek, newRep.dayOfMonth, 
                            newRep.toAccountId, newRep.reflectionTiming, subCategory = subCategoryName,
                            linkedGiftCardId = newRep.linkedGiftCardId
                        )
                    } else {
                        onConfirm(newRep)
                    }
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
