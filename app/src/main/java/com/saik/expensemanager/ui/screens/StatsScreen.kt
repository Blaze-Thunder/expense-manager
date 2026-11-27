package com.saik.expensemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils
import com.saik.expensemanager.util.FuzzySearch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onEditTransaction: (Transaction) -> Unit = {}
) {
    val budgetBooks by viewModel.activeBudgetBooks.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()
    val selectedCal by viewModel.statsCalendar.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    val filterDateRange by viewModel.statsFilterDateRange.collectAsStateWithLifecycle()
    val filterAccountIds by viewModel.statsFilterAccountIds.collectAsStateWithLifecycle()
    val filterCategoryNames by viewModel.statsFilterCategoryNames.collectAsStateWithLifecycle()
    val filterTransactionTypes by viewModel.statsFilterTransactionTypes.collectAsStateWithLifecycle()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    val statsType by viewModel.statsType.collectAsStateWithLifecycle()
    val statsPeriodMode by viewModel.statsViewMode.collectAsStateWithLifecycle()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var statsSearchQuery by rememberSaveable { mutableStateOf("") }
    // Debounced query for fuzzy matching — 300ms after the user stops typing
    var debouncedStatsQuery by remember { mutableStateOf("") }
    LaunchedEffect(statsSearchQuery) {
        delay(300)
        debouncedStatsQuery = statsSearchQuery
    }

    var selectedDetailCategoryName by rememberSaveable { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(enabled = selectedDetailCategoryName != null) {
        selectedDetailCategoryName = null
    }

    val currencySim = activeBook?.currencyCode ?: appSettings.currencyCode

    val topPeriodText = remember(statsPeriodMode, selectedCal, filterDateRange, filterTransactionTypes) {
        if (filterDateRange != null) {
            val sdf = SimpleDateFormat("dd.MM.yy", Locale.US)
            return@remember "${sdf.format(Date(filterDateRange!!.first))} - ${sdf.format(Date(filterDateRange!!.second))}"
        }
        val firstDay = selectedCal.firstDayOfWeek
        val weekSdf = SimpleDateFormat("dd.MM.yy", Locale.US)
        when (statsPeriodMode) {
            0 -> {
                val start = (selectedCal.clone() as Calendar).apply { set(Calendar.DAY_OF_WEEK, firstDay) }
                val end = (start.clone() as Calendar).apply { add(Calendar.DATE, 6) }
                "${weekSdf.format(start.time)} - ${weekSdf.format(end.time)}"
            }
            1 -> SimpleDateFormat("MMMM yyyy", Locale.US).format(selectedCal.time)
            2 -> SimpleDateFormat("yyyy", Locale.US).format(selectedCal.time)
            3 -> "All Time"
            else -> ""
        }
    }

    val relevantTransactions = remember(allTransactions, statsPeriodMode, selectedCal, activeBook, filterDateRange, filterAccountIds, filterCategoryNames, filterTransactionTypes) {
        val bookId = activeBook?.id ?: return@remember emptyList<Transaction>()
        
        val filteredByBook = allTransactions.filter { tx -> (tx.linkedBudgetBookId == bookId || tx.linkedBudgetBookId == -1L) }
        
        val filteredByFilters = filteredByBook.filter { tx ->
            val matchesAccount = filterAccountIds.isEmpty() || filterAccountIds.contains(tx.linkedAccountId) || (tx.toAccountId != null && filterAccountIds.contains(tx.toAccountId))
            val matchesCategory = filterCategoryNames.isEmpty() || filterCategoryNames.contains(tx.category)
            val matchesType = filterTransactionTypes.isEmpty() || filterTransactionTypes.contains(tx.type)
            matchesAccount && matchesCategory && matchesType
        }

        if (filterDateRange != null) {
            return@remember filteredByFilters.filter { it.dateTime in filterDateRange!!.first..filterDateRange!!.second }
        }

        val targetYear = selectedCal.get(Calendar.YEAR)
        val targetMonth = selectedCal.get(Calendar.MONTH)
        val firstDay = selectedCal.firstDayOfWeek
        
        when (statsPeriodMode) {
            0 -> {
                val start = (selectedCal.clone() as Calendar).apply { set(Calendar.DAY_OF_WEEK, firstDay); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                val end = (start.clone() as Calendar).apply { add(Calendar.DATE, 6); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
                filteredByFilters.filter { tx -> tx.dateTime >= start.timeInMillis && tx.dateTime <= end.timeInMillis }
            }
            1 -> filteredByFilters.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
                txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.MONTH) == targetMonth
            }
            2 -> filteredByFilters.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
                txCal.get(Calendar.YEAR) == targetYear
            }
            3 -> filteredByFilters
            else -> emptyList()
        }
    }

    val typedTransactions = remember(relevantTransactions, statsType, debouncedStatsQuery) {
        val query = debouncedStatsQuery
        val matchingTxs = relevantTransactions.filter { tx -> tx.type == statsType }

        if (query.isBlank()) {
            matchingTxs
        } else {
            // Fuzzy matching with ranking: exact substring matches first, then fuzzy by score
            matchingTxs.map { tx ->
                tx to FuzzySearch.similarity(query, "${tx.description} ${tx.category}")
            }
            .filter { (_, score) -> score >= FuzzySearch.DEFAULT_THRESHOLD }
            .sortedWith(
                compareByDescending<Pair<Transaction, Double>> { it.second }
                    .thenByDescending { it.first.dateTime }
            )
            .map { it.first }
        }
    }

    val totalSum = remember(typedTransactions) { typedTransactions.sumOf { it.amount } }

    val categoryBreakdowns = remember(typedTransactions, categories, totalSum) {
        val groupedByMainCategory = typedTransactions.groupBy { tx ->
            val match = categories.find { it.name.lowercase() == tx.category.lowercase() }
            if (match?.parentId != null) {
                categories.find { it.id == match.parentId }?.name ?: tx.category
            } else {
                match?.name ?: tx.category
            }
        }
        groupedByMainCategory.map { (name, txs) ->
            val sum = txs.sumOf { it.amount }
            val match = categories.find { it.name.lowercase() == name.lowercase() }
            CategoryBreakdownItem(name, sum, if (totalSum > 0) sum / totalSum else 0.0, Color(match?.color ?: 0xFF90A4AE.toInt()))
        }.sortedByDescending { it.amount }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                if (selectedDetailCategoryName != null) {
                    StandardTopAppBar(
                        title = selectedDetailCategoryName!!,
                        navigationIcon = {
                            IconButton(onClick = { selectedDetailCategoryName = null }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
                            }
                        }
                    )
                }

                if (selectedDetailCategoryName == null) {
                    // Header controls moved to top, title removed
                    ContextBar(
                        activeBook = activeBook,
                        budgetBooks = budgetBooks,
                        periodLabel = topPeriodText,
                        onBookSelected = { viewModel.selectBudgetBook(it) },
                        onPrevClick = { viewModel.selectStatsOffset(statsPeriodMode, -1) },
                        onNextClick = { viewModel.selectStatsOffset(statsPeriodMode, 1) },
                        modifier = Modifier.padding(top = Dimens.SpacingSmall)
                    )
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    
                    SegmentedPeriodSelector(
                        modes = listOf("EXPENSE", "INCOME"),
                        selectedMode = statsType,
                        onModeSelected = { viewModel.setStatsType(it) }
                    )
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    
                    val periodModes = listOf("Weekly", "Monthly", "Yearly", "Total")
                    SegmentedPeriodSelector(
                        modes = periodModes,
                        selectedMode = periodModes[statsPeriodMode.coerceIn(0, 3)],
                        onModeSelected = { 
                            viewModel.setStatsViewMode(periodModes.indexOf(it))
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                    // Search and Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.SpacingLarge),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {
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
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = statsSearchQuery,
                                    onValueChange = { 
                                        statsSearchQuery = it
                                        if (it.isEmpty()) focusManager.clearFocus()
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (statsSearchQuery.isEmpty()) {
                                                Text(
                                                    "Search category or note...",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                if (statsSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { 
                                            statsSearchQuery = ""
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
                            onClearFilters = { viewModel.clearStatsFilters() },
                            onRemoveDate = { viewModel.setStatsFilterDateRange(null) },
                            onRemoveAccount = { id -> viewModel.setStatsFilterAccounts(filterAccountIds - id) },
                            onRemoveCategory = { name -> viewModel.setStatsFilterCategories(filterCategoryNames - name) },
                            onRemoveTransactionType = { type -> viewModel.setStatsFilterTransactionTypes(filterTransactionTypes - type) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                }
            }
        }
    ) { innerPadding ->
        var horizontalDragAmount by remember { mutableStateOf(0f) }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pointerInput(statsPeriodMode, selectedCal) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (horizontalDragAmount > 50) {
                                viewModel.selectStatsOffset(statsPeriodMode, -1)
                            } else if (horizontalDragAmount < -50) {
                                viewModel.selectStatsOffset(statsPeriodMode, 1)
                            }
                            horizontalDragAmount = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalDragAmount += dragAmount
                        }
                    )
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            if (selectedDetailCategoryName != null) {
                CategoryDetailView(
                    mainCatName = selectedDetailCategoryName!!,
                    typedTransactions = typedTransactions,
                    categories = categories,
                    accounts = accounts,
                    statsType = statsType,
                    currencySim = currencySim,
                    onTransactionClick = onEditTransaction
                )
            } else {
                if (categoryBreakdowns.isEmpty()) {
                    StatsEmptyState(statsType)
                } else {
                    PrimaryCard(modifier = Modifier.padding(bottom = Dimens.SpacingLarge)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
                        ) {
                            Text("Allocation Breakdown", style = MaterialTheme.typography.titleMedium)

                            InteractiveDonutChart(categoryBreakdowns, totalSum, currencySim, statsType)

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium), modifier = Modifier.fillMaxWidth()) {
                                categoryBreakdowns.forEach { cb ->
                                    CategoryBreakdownListItem(cb, currencySim, onClick = { selectedDetailCategoryName = cb.categoryName })
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            viewModel = viewModel,
            isStats = true,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun CategoryDetailView(
    mainCatName: String,
    typedTransactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    statsType: String,
    currencySim: String,
    onTransactionClick: (Transaction) -> Unit
) {
    val categoryDetailedTransactions = remember(typedTransactions, categories, mainCatName) {
        val targetLower = mainCatName.lowercase()
        typedTransactions.filter { tx ->
            val match = categories.find { it.name.lowercase() == tx.category.lowercase() }
            if (match != null) {
                match.name.lowercase() == targetLower || (match.parentId != null && categories.find { it.id == match.parentId }?.name?.lowercase() == targetLower)
            } else tx.category.lowercase() == targetLower
        }
    }

    val totalDetailedSum = categoryDetailedTransactions.sumOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)) {
        PrimaryCard {
            Column(
                modifier = Modifier.padding(Dimens.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(if (statsType == "INCOME") "Total Income" else "Total Spend", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = CurrencyUtils.formatAmount(totalDetailedSum, currencySim),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (statsType == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }

        val subCategoryBreakdowns = remember(categoryDetailedTransactions, categories, totalDetailedSum) {
            categoryDetailedTransactions.groupBy { it.category }.map { (name, txs) ->
                val sum = txs.sumOf { it.amount }
                val match = categories.find { it.name.lowercase() == name.lowercase() }
                CategoryBreakdownItem(name, sum, if (totalDetailedSum > 0) sum / totalDetailedSum else 0.0, Color(match?.color ?: 0xFF90A4AE.toInt()))
            }.sortedByDescending { it.amount }
        }

        if (subCategoryBreakdowns.isNotEmpty()) {
            SecondaryCard {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                    SectionHeader("Subcategory Breakdown", modifier = Modifier.padding(horizontal = 0.dp))
                    subCategoryBreakdowns.forEach { CategoryBreakdownListItem(it, currencySim) }
                }
            }
        }

        SectionHeader("Transactions")
        TransactionListSection(
            transactions = categoryDetailedTransactions,
            categories = categories,
            accounts = accounts,
            currencySim = currencySim,
            onTransactionClick = onTransactionClick
        )
    }
}

data class CategoryBreakdownItem(val categoryName: String, val amount: Double, val percentage: Double, val color: Color)

@Composable
fun InteractiveDonutChart(breakdowns: List<CategoryBreakdownItem>, totalSum: Double, currencySim: String, statsType: String) {
    Box(modifier = Modifier.size(200.dp).padding(12.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = 0f
            breakdowns.forEach { cb ->
                val sweep = (cb.percentage * 360f).toFloat()
                drawArc(color = cb.color, startAngle = startAngle, sweepAngle = sweep, useCenter = false, style = Stroke(width = 32f, cap = StrokeCap.Round), size = Size(size.width, size.height))
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Text(if (statsType == "INCOME") "Total Income" else "Total Spend", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(CurrencyUtils.formatAmount(totalSum, currencySim), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun CategoryBreakdownListItem(item: CategoryBreakdownItem, currencySim: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Column(modifier = modifier.padding(vertical = Dimens.SpacingSmall)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(item.color))
                Text(item.categoryName, style = MaterialTheme.typography.titleSmall)
                Badge(containerColor = item.color.copy(alpha = 0.12f), contentColor = item.color) {
                    Text(String.format(Locale.US, "%.1f%%", item.percentage * 100), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(2.dp))
                }
            }
            Text(CurrencyUtils.formatAmount(item.amount, currencySim), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
        LinearProgressIndicator(progress = { item.percentage.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = item.color, trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}

@Composable
fun StatsEmptyState(type: String) {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
            Icon(Icons.Default.PieChart, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
            Text("No ${type.lowercase()} records found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}
