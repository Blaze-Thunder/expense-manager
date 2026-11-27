package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetBooksScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val budgetBooks by viewModel.budgetBooks.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val currencySim = appSettings.currencyCode
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedBookForEdit by remember { mutableStateOf<BudgetBook?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val activeBooks = remember(budgetBooks) { budgetBooks.filter { !it.isClosed } }
    val closedBooks = remember(budgetBooks) { budgetBooks.filter { it.isClosed } }
    val defaultBook = remember(activeBooks) { activeBooks.find { it.isDefault } }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "Budget Books",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Book") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
                    Text(
                        "Track expenses for different contexts like trips, monthly home budget, or personal goals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
                    
                    BudgetSummaryCard(
                        activeCount = activeBooks.size,
                        closedCount = closedBooks.size,
                        defaultBookName = defaultBook?.name ?: "None"
                    )
                }
            }

            if (activeBooks.isNotEmpty()) {
                item {
                    SectionHeader("Active Books", modifier = Modifier.padding(horizontal = Dimens.SpacingLarge))
                }
                items(activeBooks) { book ->
                    BudgetBookCard(
                        book = book,
                        currencySim = currencySim,
                        onEdit = { selectedBookForEdit = it },
                        onSetDefault = { 
                            viewModel.setDefaultBook(it.id) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onToggleClosed = {
                            viewModel.toggleBookClosed(it.id) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onDelete = {
                            viewModel.deleteBudgetBook(it) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        }
                    )
                }
            }

            if (closedBooks.isNotEmpty()) {
                item {
                    SectionHeader("Closed Books", modifier = Modifier.padding(start = Dimens.SpacingLarge, end = Dimens.SpacingLarge, top = Dimens.SpacingLarge))
                }
                items(closedBooks) { book ->
                    BudgetBookCard(
                        book = book,
                        currencySim = currencySim,
                        onEdit = { selectedBookForEdit = it },
                        onSetDefault = {},
                        onToggleClosed = {
                            viewModel.toggleBookClosed(it.id) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onDelete = {
                            viewModel.deleteBudgetBook(it) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        BudgetBookEditorSheet(
            currencySymbol = currencySim,
            onDismiss = { showAddSheet = false },
            onSave = { name, limit, start, end ->
                viewModel.addBudgetBook(name, limit, currencySim)
                // Add functionality for start/end dates if addBudgetBook is updated
                if (start != null || end != null) {
                    // Logic to update dates after insertion if needed
                }
            }
        )
    }

    if (selectedBookForEdit != null) {
        BudgetBookEditorSheet(
            book = selectedBookForEdit,
            currencySymbol = currencySim,
            onDismiss = { selectedBookForEdit = null },
            onSave = { name, limit, start, end ->
                viewModel.updateBudgetBook(selectedBookForEdit!!.id, name, limit, currencySim, true, start, end)
            }
        )
    }
}

@Composable
fun BudgetSummaryCard(
    activeCount: Int,
    closedCount: Int,
    defaultBookName: String
) {
    PrimaryCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Dimens.SpacingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoStories, null, tint = MaterialTheme.colorScheme.primary)
            }
            
            Column {
                Text(
                    text = "System Default: $defaultBookName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$activeCount active, $closedCount archived books",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetBookCard(
    book: BudgetBook,
    currencySim: String,
    onEdit: (BudgetBook) -> Unit,
    onSetDefault: (BudgetBook) -> Unit,
    onToggleClosed: (BudgetBook) -> Unit,
    onDelete: (BudgetBook) -> Unit
) {
    val sdf = remember { SimpleDateFormat("d MMM yyyy", Locale.US) }
    
    SecondaryCard(
        modifier = Modifier
            .padding(horizontal = Dimens.SpacingLarge, vertical = 6.dp)
            .clickable { onEdit(book) }
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = book.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (book.isClosed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                        if (book.isDefault) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) {
                                Text("DEFAULT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (book.isClosed) {
                            Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                Text("CLOSED", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }

                    val dateRange = when {
                        book.startDate != null && book.endDate != null -> "${sdf.format(Date(book.startDate!!))} — ${sdf.format(Date(book.endDate!!))}"
                        book.startDate != null -> "From ${sdf.format(Date(book.startDate!!))}"
                        book.endDate != null -> "Until ${sdf.format(Date(book.endDate!!))}"
                        else -> null
                    }
                    if (dateRange != null) {
                        Text(dateRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!book.isClosed && !book.isDefault) {
                            DropdownMenuItem(
                                text = { Text("Set as Default") },
                                onClick = { onSetDefault(book); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Star, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (book.isClosed) "Re-open Book" else "Close Book") },
                            onClick = { onToggleClosed(book); showMenu = false },
                            leadingIcon = { Icon(if (book.isClosed) Icons.Default.LockOpen else Icons.Default.Lock, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Details") },
                            onClick = { onEdit(book); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        if (!book.isDefault) {
                            DropdownMenuItem(
                                text = { Text("Delete Book", color = MaterialTheme.colorScheme.error) },
                                onClick = { onDelete(book); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.SpacingMedium, vertical = Dimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                ) {
                    Icon(
                        imageVector = if (book.monthlyLimit != null) Icons.Default.AccountBalanceWallet else Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (book.monthlyLimit != null) "Monthly Budget: ${CurrencyUtils.formatAmount(book.monthlyLimit!!, currencySim)}" 
                               else "Spend tracking only",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetBookEditorSheet(
    book: BudgetBook? = null,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, limit: Double?, start: Long?, end: Long?) -> Unit
) {
    var name by remember { mutableStateOf(book?.name ?: "") }
    var limitStr by remember { mutableStateOf(book?.monthlyLimit?.toString() ?: "") }
    var startDate by remember { mutableStateOf(book?.startDate) }
    var endDate by remember { mutableStateOf(book?.endDate) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.SpacingLarge)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            Text(
                text = if (book == null) "Create New Book" else "Edit Budget Book",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Book Name") },
                placeholder = { Text("e.g., Vacation, Monthly Home, etc.") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = limitStr,
                onValueChange = { limitStr = it },
                label = { Text("Monthly Budget Limit (Optional)") },
                placeholder = { Text("Leave blank for spend tracking only") },
                prefix = { Text("$currencySymbol ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (limitStr.isNotEmpty()) IconButton(onClick = { limitStr = "" }) { Icon(Icons.Default.Clear, null) }
                }
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start Date", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = startDate?.let { sdf.format(Date(it)) } ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.clickable { showStartDatePicker = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("End Date", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = endDate?.let { sdf.format(Date(it)) } ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.clickable { showEndDatePicker = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.Event, null, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
            
            if (startDate != null && endDate != null && endDate!! < startDate!!) {
                Text("End date must be after start date", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val limit = limitStr.toDoubleOrNull()
                    if (name.isNotBlank()) {
                        onSave(name, limit, startDate, endDate)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && !(startDate != null && endDate != null && endDate!! < startDate!!)
            ) {
                Text(if (book == null) "Create Book" else "Save Changes", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    startDate = null
                    showStartDatePicker = false 
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    endDate = null
                    showEndDatePicker = false 
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
