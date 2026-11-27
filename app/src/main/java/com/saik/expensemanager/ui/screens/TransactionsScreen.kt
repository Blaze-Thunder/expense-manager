package com.saik.expensemanager.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDialog as ComposeDatePickerDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.ui.viewmodel.ViewMode
import com.saik.expensemanager.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onAddTransaction: (Transaction?) -> Unit = {},
    onManageTemplates: () -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {}
) {
    val budgetBooks by viewModel.activeBudgetBooks.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()
    val selectedCal by viewModel.transactionsCalendar.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()

    val filterDateRange by viewModel.filterDateRange.collectAsStateWithLifecycle()
    val filterAccountIds by viewModel.filterAccountIds.collectAsStateWithLifecycle()
    val filterCategoryNames by viewModel.filterCategoryNames.collectAsStateWithLifecycle()
    val filterTransactionTypes by viewModel.filterTransactionTypes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val currentViewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var selectedDayForSheet by remember { mutableStateOf<Calendar?>(null) }
    val focusRequester = remember { FocusRequester() }

    var scrollToDateMs by remember { mutableStateOf<Long?>(null) }

    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showSaveTemplateSheet by remember { mutableStateOf(false) }

    val currencySim = activeBook?.currencyCode ?: appSettings.currencyCode

    var lastTime by remember { mutableLongStateOf(selectedCal.timeInMillis) }
    var slideDirection by remember { mutableIntStateOf(1) }

    LaunchedEffect(selectedCal.timeInMillis) {
        if (selectedCal.timeInMillis != lastTime) {
            slideDirection = if (selectedCal.timeInMillis > lastTime) 1 else -1
            lastTime = selectedCal.timeInMillis
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Period Label and Navigation in a row at the top (Title Bar removed)
                val periodLabel = remember(currentViewMode, selectedCal) {
                    val firstDay = selectedCal.firstDayOfWeek
                    val weekSdf = SimpleDateFormat("dd.MM.yy", Locale.US)
                    when (currentViewMode) {
                        ViewMode.DAILY, ViewMode.CALENDAR -> SimpleDateFormat("MMMM yyyy", Locale.US).format(selectedCal.time)
                        ViewMode.MONTHLY, ViewMode.YEARLY -> SimpleDateFormat("yyyy", Locale.US).format(selectedCal.time)
                        ViewMode.WEEKLY -> {
                            val start = (selectedCal.clone() as Calendar).apply { set(Calendar.DAY_OF_WEEK, firstDay) }
                            val end = (start.clone() as Calendar).apply { add(Calendar.DATE, 6) }
                            "${weekSdf.format(start.time)} - ${weekSdf.format(end.time)}"
                        }
                        else -> ""
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BudgetBookSelector(
                        activeBook = activeBook,
                        budgetBooks = budgetBooks,
                        onBookSelected = { viewModel.selectBudgetBook(it) }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.selectTransactionsOffsetByPeriod(currentViewMode.name, -1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewModel.selectTransactionsOffsetByPeriod(currentViewMode.name, 1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // View Mode Selector
                SegmentedPeriodSelector(
                    modes = ViewMode.values()
                        .filter { it != ViewMode.CYCLE }
                        .map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    selectedMode = currentViewMode.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                    onModeSelected = { viewModel.setViewMode(ViewMode.valueOf(it.uppercase())) }
                )

                // Search and Filter below View Modes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                ) {
                    // Sleek rectangular search bar
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Search, 
                                null, 
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    viewModel.setSearchQuery(it)
                                    if (it.isEmpty()) focusManager.clearFocus()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search notes or category...", 
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { 
                                        viewModel.setSearchQuery("")
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    Surface(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val filterActive = filterDateRange != null || filterAccountIds.isNotEmpty() || filterCategoryNames.isNotEmpty() || filterTransactionTypes.isNotEmpty()
                            BadgedBox(badge = { if (filterActive) Badge() }) {
                                Icon(
                                    Icons.Default.FilterList, 
                                    contentDescription = "Filter", 
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val filterActive = filterDateRange != null || filterAccountIds.isNotEmpty() || filterCategoryNames.isNotEmpty() || filterTransactionTypes.isNotEmpty()
                if (filterActive) {
                    ActiveFilterChipsRow(
                        filterDateRange = filterDateRange,
                        filterAccountIds = filterAccountIds,
                        filterCategoryNames = filterCategoryNames,
                        filterTransactionTypes = filterTransactionTypes,
                        accounts = accounts,
                        onClearFilters = { viewModel.clearFilters() },
                        onRemoveDate = { viewModel.setFilterDateRange(null) },
                        onRemoveAccount = { id -> viewModel.setFilterAccounts(filterAccountIds - id) },
                        onRemoveCategory = { name -> viewModel.setFilterCategories(filterCategoryNames - name) },
                        onRemoveTransactionType = { type -> viewModel.setFilterTransactionTypes(filterTransactionTypes - type) }
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        },
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (activeBook != null) {
                StaticTransactionFabs(
                    onNewTransaction = { onAddTransaction(null) },
                    onFromTemplate = { showTemplatesSheet = true }
                )
            }
        }
    ) { innerPadding ->
        var horizontalDragAmount by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pointerInput(currentViewMode, selectedCal, searchQuery) {
                    if (searchQuery.isNotEmpty()) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (horizontalDragAmount > 100) {
                                viewModel.selectTransactionsOffsetByPeriod(currentViewMode.name, -1)
                            } else if (horizontalDragAmount < -100) {
                                viewModel.selectTransactionsOffsetByPeriod(currentViewMode.name, 1)
                            }
                            horizontalDragAmount = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            horizontalDragAmount += dragAmount
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = selectedCal.timeInMillis,
                transitionSpec = {
                    if (slideDirection > 0) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "transactions_period_transition"
            ) { targetTime ->
                // Using targetTime to satisfy the lint check for unused parameter in AnimatedContent
                val currentTargetTime = targetTime
                
                if (searchQuery.isNotEmpty()) {
                    SearchResultsView(
                        transactions = transactions,
                        currencySim = currencySim,
                        categories = categories,
                        accounts = accounts,
                        onEditTransaction = onEditTransaction
                    )
                } else {
                    when (currentViewMode) {
                        ViewMode.DAILY -> DailyListView(
                            transactions = transactions,
                            currencySim = currencySim,
                            categories = categories,
                            accounts = accounts,
                            onEdit = onEditTransaction,
                            initialScrollDate = scrollToDateMs,
                            onScrolled = { scrollToDateMs = null }
                        )
                        ViewMode.WEEKLY -> WeeklyView(transactions, selectedCal, currencySim, categories, accounts, onEdit = onEditTransaction, onPrev = { viewModel.selectOffsetByPeriod("WEEKLY", -1) }, onNext = { viewModel.selectOffsetByPeriod("WEEKLY", 1) })
                        ViewMode.CALENDAR -> CalendarView(selectedCal, allTransactions.filter { it.linkedBudgetBookId == activeBook?.id || it.linkedBudgetBookId == -1L }, currencySim, onDateClick = { cal ->
                            selectedDayForSheet = cal
                        })
                        ViewMode.MONTHLY -> MonthlyRedesignView(allTransactions.filter { it.linkedBudgetBookId == activeBook?.id || it.linkedBudgetBookId == -1L }, currencySim, selectedCal)
                        ViewMode.YEARLY -> YearlyRedesignView(allTransactions.filter { it.linkedBudgetBookId == activeBook?.id || it.linkedBudgetBookId == -1L }, currencySim, selectedCal)
                        ViewMode.CYCLE -> {}
                    }
                }
            }
        }
    }

    if (selectedDayForSheet != null) {
        val dateMs = selectedDayForSheet!!.timeInMillis
        val dayTxs = allTransactions.filter { 
            val itCal = Calendar.getInstance().apply { timeInMillis = it.dateTime }
            (it.linkedBudgetBookId == activeBook?.id || it.linkedBudgetBookId == -1L) &&
            itCal.get(Calendar.YEAR) == selectedDayForSheet!!.get(Calendar.YEAR) &&
            itCal.get(Calendar.DAY_OF_YEAR) == selectedDayForSheet!!.get(Calendar.DAY_OF_YEAR)
        }
        
        DayDetailBottomSheet(
            dateMs = dateMs,
            transactions = dayTxs,
            currencySim = currencySim,
            categories = categories,
            accounts = accounts,
            onDismiss = { selectedDayForSheet = null },
            onEditTransaction = onEditTransaction
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            viewModel = viewModel,
            isStats = false,
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showTemplatesSheet) {
        QuickTemplatesBottomSheet(
            templates = allTemplates,
            currencySim = currencySim,
            categories = categories,
            accounts = accounts,
            logInstantly = appSettings.logTemplatesInstantly,
            onLogInstantlyChange = { viewModel.updateLogTemplatesInstantly(it) },
            onDismiss = { showTemplatesSheet = false },
            onTemplateSelected = { template ->
                showTemplatesSheet = false
                if (appSettings.logTemplatesInstantly) {
                    viewModel.logTransactionFromTemplate(template)
                } else {
                    onAddTransaction(viewModel.getPrefillFromTemplate(template))
                }
            },
            onNewTemplate = {
                showTemplatesSheet = false
                showSaveTemplateSheet = true
            },
            onManageTemplates = {
                showTemplatesSheet = false
                onManageTemplates()
            }
        )
    }

    if (showSaveTemplateSheet) {
        SaveTemplateBottomSheet(
            viewModel = viewModel,
            onDismiss = { showSaveTemplateSheet = false },
            onConfirm = { template ->
                viewModel.addTemplate(
                    name = template.name,
                    amount = template.amount,
                    type = template.type,
                    category = template.category,
                    subCategory = template.subCategory,
                    accountId = template.linkedAccountId,
                    toAccountId = template.toAccountId,
                    budgetBookId = template.linkedBudgetBookId,
                    description = template.description,
                    iconName = template.iconName,
                    emojiValue = template.emojiValue,
                    colorHex = template.colorHex
                )
                showSaveTemplateSheet = false
            },
            accounts = accounts,
            categories = categories,
            budgetBooks = budgetBooks,
            currencySim = currencySim
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    dateMs: Long,
    transactions: List<Transaction>,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dayName = SimpleDateFormat("EEEE", Locale.US).format(Date(dateMs))
    val fullDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(dateMs))
    val dayNum = SimpleDateFormat("d", Locale.US).format(Date(dateMs))

    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(dayNum, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(dayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(fullDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    CurrencyUtils.formatAmountWithSign(income - expense, "", if (income - expense >= 0) "INCOME" else "EXPENSE"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (income - expense >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                SummaryStatCard("INCOME", income, Color(0xFF2E7D32).copy(alpha = 0.1f), Color(0xFF2E7D32), currencySim, Modifier.weight(1f))
                SummaryStatCard("EXPENSE", expense, MaterialTheme.colorScheme.error.copy(alpha = 0.1f), MaterialTheme.colorScheme.error, currencySim, Modifier.weight(1f))
                SummaryStatCard("NET", income - expense, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), MaterialTheme.colorScheme.secondary, currencySim, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

            // Transactions list
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions on this day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                    items(transactions) { tx ->
                        TransactionCard(tx, currencySim, categories, accounts) { 
                            onDismiss()
                            onEditTransaction(tx) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(label: String, amount: Double, bgColor: Color, contentColor: Color, currencySim: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val icon = when(label) {
                    "INCOME" -> Icons.Default.TrendingUp
                    "EXPENSE" -> Icons.Default.TrendingDown
                    else -> Icons.Default.HorizontalRule
                }
                Icon(icon, null, modifier = Modifier.size(12.dp), tint = contentColor)
                Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor, fontWeight = FontWeight.Bold)
            }
            Text(
                CurrencyUtils.formatAmount(amount, currencySim).replace(".00", ""),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFilterChipsRow(
    filterDateRange: Pair<Long, Long>?,
    filterAccountIds: Set<Long>,
    filterCategoryNames: Set<String>,
    filterTransactionTypes: Set<String> = emptySet(),
    accounts: List<Account>,
    onClearFilters: () -> Unit,
    onRemoveDate: () -> Unit,
    onRemoveAccount: (Long) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onRemoveTransactionType: (String) -> Unit = {}
) {
    if (filterDateRange == null && filterAccountIds.isEmpty() && filterCategoryNames.isEmpty() && filterTransactionTypes.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputChip(
            selected = true,
            onClick = onClearFilters,
            label = { Text("Clear All", style = MaterialTheme.typography.labelMedium) },
            trailingIcon = { Icon(Icons.Default.ClearAll, null, modifier = Modifier.size(14.dp)) }
        )

        filterDateRange?.let {
            val sdf = SimpleDateFormat("MMM d", Locale.US)
            InputChip(
                selected = true,
                onClick = onRemoveDate,
                label = { Text("${sdf.format(Date(it.first))} - ${sdf.format(Date(it.second))}", style = MaterialTheme.typography.labelMedium) },
                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
            )
        }

        filterAccountIds.forEach { id ->
            val name = accounts.find { it.id == id }?.name ?: "Acc"
            InputChip(
                selected = true,
                onClick = { onRemoveAccount(id) },
                label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
            )
        }

        filterCategoryNames.forEach { name ->
            InputChip(
                selected = true,
                onClick = { onRemoveCategory(name) },
                label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
            )
        }

        filterTransactionTypes.forEach { type ->
            InputChip(
                selected = true,
                onClick = { onRemoveTransactionType(type) },
                label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium) },
                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
            )
        }
    }
}

@Composable
fun DailyListView(
    transactions: List<Transaction>,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    onEdit: (Transaction) -> Unit,
    initialScrollDate: Long? = null,
    onScrolled: (() -> Unit)? = null
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val grouped = remember(transactions) {
        transactions.groupBy { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTime }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.mapValues { (_, txs) ->
            txs.sortedWith(
                compareBy<Transaction> { it.isGeneratedFromRecurrence }
                    .thenByDescending { it.dateTime }
            )
        }.toList().sortedByDescending { it.first }
    }

    LaunchedEffect(initialScrollDate, grouped) {
        if (initialScrollDate != null && grouped.any { it.first == initialScrollDate }) {
            var index = 0
            for ((date, txs) in grouped) {
                if (date == initialScrollDate) break
                index += 1 + txs.size
            }
            listState.animateScrollToItem(index)
            onScrolled?.invoke()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        grouped.forEach { (dateMs, txs) ->
            item {
                val dayTotal = txs.sumOf { 
                    when (it.type) {
                        "INCOME" -> it.amount
                        "EXPENSE" -> -it.amount
                        else -> 0.0
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US).format(Date(dateMs)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = CurrencyUtils.formatAmountWithSign(dayTotal, ""),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (dayTotal > 0) Color(0xFF2E7D32) else if (dayTotal < 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            items(txs, key = { it.id }) { tx ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically { it / 2 }
                ) {
                    TransactionCard(tx, currencySim, categories, accounts) { onEdit(tx) }
                }
            }
        }
    }
}

@Composable
fun WeeklyView(
    transactions: List<Transaction>,
    selectedCal: Calendar,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    onEdit: (Transaction) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column {
        DailyListView(transactions, currencySim, categories, accounts, onEdit)
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    income: Double,
    expense: Double,
    isFuture: Boolean,
    currencySim: String,
    onClick: () -> Unit
) {
    val netAmount = income - expense
    val amountColor = when {
        netAmount > 0 -> if (isFuture) Color(0xFF2E7D32).copy(alpha = 0.6f) else Color(0xFF2E7D32)
        netAmount < 0 -> if (isFuture) Color(0xFFC62828).copy(alpha = 0.6f) else Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surface
            )
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        // Day number
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (isToday) MaterialTheme.colorScheme.primary 
                    else if (isCurrentMonth) MaterialTheme.colorScheme.onSurface 
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Amount
        if (income != 0.0 || expense != 0.0) {
            Text(
                text = CurrencyUtils.formatAmountWithSign(netAmount, "").replace(".00", ""),
                style = if (kotlin.math.abs(netAmount) > 9999) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
fun CalendarView(
    selectedCal: Calendar,
    transactions: List<Transaction>,
    currencySim: String,
    onDateClick: (Calendar) -> Unit
) {
    val monthCal = (selectedCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOfMonthWeek = monthCal.get(Calendar.DAY_OF_WEEK)
    val firstDayOfWeekSetting = monthCal.firstDayOfWeek
    
    // Calculate the start date of the 42-cell grid
    val gridStartCal = (monthCal.clone() as Calendar).apply { 
        val offset = (firstDayOfMonthWeek - firstDayOfWeekSetting + 7) % 7
        add(Calendar.DAY_OF_MONTH, -offset)
    }

    val txByDayKey = transactions.groupBy { 
        val c = Calendar.getInstance().apply { timeInMillis = it.dateTime }
        "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.DAY_OF_MONTH)}"
    }

    val today = Calendar.getInstance()
    val currentMonth = selectedCal.get(Calendar.MONTH)
    val currentYear = selectedCal.get(Calendar.YEAR)

    Column(modifier = Modifier.fillMaxSize()) {
        // Day headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.SpacingSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            days.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(42) { i ->
                val cellCal = (gridStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
                val d = cellCal.get(Calendar.DAY_OF_MONTH)
                val m = cellCal.get(Calendar.MONTH)
                val y = cellCal.get(Calendar.YEAR)
                
                val key = "$y-$m-$d"
                val dayTxs = txByDayKey[key] ?: emptyList()
                val income = dayTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = dayTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                
                val isCurrentMonth = m == currentMonth && y == currentYear
                val isSelected = isCurrentMonth && selectedCal.get(Calendar.DAY_OF_MONTH) == d
                val isToday = today.get(Calendar.YEAR) == y && 
                              today.get(Calendar.MONTH) == m && 
                              today.get(Calendar.DAY_OF_MONTH) == d
                
                val isFuture = cellCal.after(today)

                CalendarDayCell(
                    day = d,
                    isCurrentMonth = isCurrentMonth,
                    isSelected = isSelected,
                    isToday = isToday,
                    income = income,
                    expense = expense,
                    isFuture = isFuture,
                    currencySim = currencySim,
                    onClick = {
                        onDateClick(cellCal)
                    }
                )
            }
        }
    }
}

@Composable
fun MonthlyRedesignView(transactions: List<Transaction>, currencySim: String, selectedCal: Calendar) {
    val currentYear = selectedCal.get(Calendar.YEAR)
    val today = Calendar.getInstance()
    val isCurrentYear = currentYear == today.get(Calendar.YEAR)
    
    val yearTxs = remember(transactions, currentYear, today) {
        transactions.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTime }
            cal.get(Calendar.YEAR) == currentYear && it.dateTime <= today.timeInMillis
        }
    }

    val totalInc = remember(yearTxs) { yearTxs.filter { it.type == "INCOME" }.sumOf { it.amount } }
    val totalExp = remember(yearTxs) { yearTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount } }

    val monthsToShow = remember(currentYear, today, yearTxs) {
        val lastMonth = if (isCurrentYear) today.get(Calendar.MONTH) else 11
        (lastMonth downTo 0).map { monthIndex ->
            val monthCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, monthIndex)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startMs = monthCal.timeInMillis
            val endMs = (monthCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            val monthTxs = yearTxs.filter { it.dateTime in startMs..endMs }
            Pair(startMs, monthTxs)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(), 
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(monthsToShow) { (monthMs, txs) ->
                ExpandableMonthCard(monthMs, txs, currencySim)
            }
        }
    }
}

@Composable
fun ExpandableMonthCard(monthMs: Long, transactions: List<Transaction>, currencySim: String) {
    var expanded by remember { mutableStateOf(false) }
    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    
    val monthCal = Calendar.getInstance().apply { timeInMillis = monthMs }
    val monthNum = monthCal.get(Calendar.MONTH) + 1
    val lastDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SimpleDateFormat("MMMM", Locale.US).format(Date(monthMs)), 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$monthNum.1 - $monthNum.$lastDay",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatAmount(income, currencySim),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(expense, currencySim),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(income - expense, currencySim),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        if (expanded) {
            val weekRanges = remember(monthMs) {
                val ranges = mutableListOf<Pair<Long, Long>>()
                val cal = Calendar.getInstance().apply {
                    timeInMillis = monthMs
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val m = cal.get(Calendar.MONTH)
                val firstDayOfWeek = cal.firstDayOfWeek

                while (cal.get(Calendar.MONTH) == m) {
                    val start = cal.timeInMillis
                    val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val daysUntilEndOfWeek = (firstDayOfWeek + 6 - currentDayOfWeek + 7) % 7
                    
                    val endCal = cal.clone() as Calendar
                    endCal.add(Calendar.DAY_OF_MONTH, daysUntilEndOfWeek)
                    
                    if (endCal.get(Calendar.MONTH) != m) {
                        endCal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    endCal.set(Calendar.HOUR_OF_DAY, 23)
                    endCal.set(Calendar.MINUTE, 59)
                    endCal.set(Calendar.SECOND, 59)
                    endCal.set(Calendar.MILLISECOND, 999)
                    
                    ranges.add(start to endCal.timeInMillis)
                    
                    cal.timeInMillis = endCal.timeInMillis
                    cal.add(Calendar.MILLISECOND, 1)
                }
                ranges.reversed()
            }
            
            weekRanges.forEach { (startMs, endMs) ->
                val weekTxs = transactions.filter { it.dateTime in startMs..endMs }
                if (weekTxs.isNotEmpty()) {
                    val wIncome = weekTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val wExpense = weekTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val sdf = SimpleDateFormat("dd.MM", Locale.US)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            .padding(horizontal = Dimens.SpacingLarge, vertical = 10.dp), 
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${sdf.format(Date(startMs))} ~ ${sdf.format(Date(endMs))}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(CurrencyUtils.formatAmount(wIncome, currencySim).replace(".00", ""), color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(CurrencyUtils.formatAmount(wExpense, currencySim).replace(".00", ""), color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                                Text("Total ${CurrencyUtils.formatAmount(wIncome - wExpense, currencySim).replace(".00", "")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
fun YearlyRedesignView(transactions: List<Transaction>, currencySim: String, selectedCal: Calendar) {
    val currentYear = selectedCal.get(Calendar.YEAR)
    val today = Calendar.getInstance()
    
    val yearTxs = remember(transactions, currentYear, today) {
        transactions.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTime }
            cal.get(Calendar.YEAR) == currentYear && it.dateTime <= today.timeInMillis
        }
    }

    val totalInc = remember(yearTxs) { yearTxs.filter { it.type == "INCOME" }.sumOf { it.amount } }
    val totalExp = remember(yearTxs) { yearTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount } }

    Column(modifier = Modifier.fillMaxSize().padding(Dimens.SpacingLarge), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)) {
        PrimaryCard {
            Column(modifier = Modifier.padding(Dimens.SpacingLarge), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Year $currentYear Summary", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SummaryItem("Income", totalInc, Color(0xFF2E7D32), currencySim)
                    SummaryItem("Expense", totalExp, Color(0xFFC62828), currencySim)
                    SummaryItem("Net", totalInc - totalExp, MaterialTheme.colorScheme.primary, currencySim)
                }
            }
        }
    }
}

@Composable
fun ExpandableYearMonthCard(month: Int, transactions: List<Transaction>, currencySim: String) {
    var expanded by remember { mutableStateOf(false) }
    val inc = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val exp = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val monthName = SimpleDateFormat("MMMM", Locale.US).format(Calendar.getInstance().apply { set(Calendar.MONTH, month) }.time)

    SecondaryCard(modifier = Modifier.animateContentSize()) {
        Column(modifier = Modifier.padding(Dimens.SpacingMedium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(monthName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("+$currencySim${inc.toInt()}", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelSmall)
                        Text("-$currencySim${exp.toInt()}", color = Color(0xFFC62828), style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                }
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpacingSmall))
                transactions.forEach { tx ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tx.description.ifEmpty { tx.category }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(
                            text = CurrencyUtils.formatAmountWithSign(tx.amount, "", tx.type).replace(".00", ""),
                            color = if (tx.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: Double, color: Color, currencySim: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(CurrencyUtils.formatAmount(value, currencySim).replace(".00", ""), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SearchResultsView(
    transactions: List<Transaction>,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    onEditTransaction: (Transaction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TransactionListSection(
            transactions = transactions,
            categories = categories,
            accounts = accounts,
            currencySim = currencySim,
            onTransactionClick = onEditTransaction,
            showDateHeaders = true,
            modifier = Modifier.padding(bottom = 80.dp)
        )
    }
}

