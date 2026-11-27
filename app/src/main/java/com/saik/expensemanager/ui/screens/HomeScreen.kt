package com.saik.expensemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.AppSettings
import com.saik.expensemanager.data.model.CardSpendStatus
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAccounts: (Long?, Boolean) -> Unit = { _, _ -> },
    onNavigateToGiftCards: () -> Unit = {},
    onNavigateToStats: (String) -> Unit = {},
    onAddTransaction: (Transaction?) -> Unit = {},
    onManageTemplates: () -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {}
) {
    val budgetBooks by viewModel.activeBudgetBooks.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()
    val transactions by viewModel.homeFilteredTransactions.collectAsStateWithLifecycle()
    val currentMonthSummary by viewModel.currentMonthSummary.collectAsStateWithLifecycle()
    val totalAmountOwed by viewModel.totalAmountOwed.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val giftCards by viewModel.allGiftCards.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()

    var showTemplatesSheet by rememberSaveable { mutableStateOf(false) }
    var showSaveTemplateSheet by rememberSaveable { mutableStateOf(false) }

    val currencySim = activeBook?.currencyCode ?: appSettings.currencyCode

    LaunchedEffect(Unit) {
        viewModel.resetTransactionsCalendar()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
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
                    
                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            PrimaryCard(modifier = Modifier.animateContentSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpacingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Net Balance this Period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    val netVal = currentMonthSummary.netBalance
                    Text(
                        text = CurrencyUtils.formatAmountWithSign(netVal, currencySim),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netVal >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickStatusItem(Icons.Default.ArrowCircleUp, "Income", currentMonthSummary.totalIncome, Color(0xFF2E7D32), currencySim, onClick = { onNavigateToStats("INCOME") })
                        VerticalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.height(40.dp))
                        QuickStatusItem(Icons.Default.ArrowCircleDown, "Expenses", currentMonthSummary.totalExpense, MaterialTheme.colorScheme.error, currencySim, onClick = { onNavigateToStats("EXPENSE") })
                    }

                    activeBook?.let { book ->
                        val expenses = currentMonthSummary.totalExpense
                        val budget = book.monthlyLimit
                        Spacer(Modifier.height(Dimens.SpacingLarge))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(Dimens.SpacingMedium))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Budget (${book.name})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (budget != null && budget > 0) {
                                    val percentageUsed = expenses / budget
                                    val colorUsed = if (expenses > budget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    Badge(containerColor = colorUsed.copy(alpha = 0.12f), contentColor = colorUsed) {
                                        Text(String.format(Locale.US, "%.1f%%", percentageUsed * 100),
                                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (budget != null && budget > 0) {
                                val percentageUsed = expenses / budget
                                val colorUsed = if (expenses > budget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                LinearProgressIndicator(
                                    progress = { minOf(percentageUsed.toFloat(), 1.0f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = colorUsed,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    text = "of ${CurrencyUtils.formatAmount(budget, currencySim)} limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "No monthly limit set for this book",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                // Total Money I Owe
                SecondaryCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAccounts(null, true) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingMedium),
                        // Fix 3: Center-align both items in the card
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            // Fix 3: Center the icon and label row
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CreditCardOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                            Text(
                                text = "Total Money I Owe",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatAmount(totalAmountOwed, currencySim),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val availableGiftCards = giftCards.filter {
                    !it.isUsed && (it.purchaseDate + it.validityDays * 24L * 60 * 60 * 1000) > System.currentTimeMillis()
                }
                val availableValue = availableGiftCards.sumOf { it.amount }
                // Gift Cards
                SecondaryCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToGiftCards() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                            Text(
                                text = "Gift Cards (${availableGiftCards.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatAmount(availableValue, currencySim),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            val isAccountsHomeExpanded by viewModel.isAccountsHomeExpanded.collectAsStateWithLifecycle()
            AccountsHomeModule(
                accounts = accounts,
                accountBalances = accountBalances,
                appSettings = appSettings,
                isExpanded = isAccountsHomeExpanded,
                onToggleExpand = { viewModel.toggleAccountsHomeExpansion() },
                currencySim = currencySim,
                onAccountClick = { accountId ->
                    onNavigateToAccounts(accountId, false)
                },
                onSeeAllClick = { onNavigateToAccounts(null, false) },
                onSaveCategories = { newCategories ->
                    viewModel.updateHomeAccountCategories(newCategories)
                }
            )

            val pendingCardAlerts by viewModel.pendingCardAlerts.collectAsStateWithLifecycle()
            val isThresholdAlertsExpanded by viewModel.isThresholdAlertsExpanded.collectAsStateWithLifecycle()
            ThresholdAlertCarousel(
                pendingCards = pendingCardAlerts,
                isExpanded = isThresholdAlertsExpanded,
                onToggleExpand = { viewModel.toggleThresholdAlertsExpansion() },
                currencySim = currencySim,
                onCardClick = { cardId ->
                    onNavigateToAccounts(cardId, true)
                },
                onSeeAllClick = { onNavigateToAccounts(null, false) }
            )

            SectionHeader("Recent this month", modifier = Modifier.padding(top = 0.dp, bottom = 0.dp))
            
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                TransactionListSection(
                    transactions = transactions.take(10),
                    categories = categories,
                    accounts = accounts,
                    currencySim = currencySim,
                    onTransactionClick = onEditTransaction
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp))
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
}

@Composable
fun ThresholdAlertCarousel(
    pendingCards: List<CardSpendStatus>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currencySim: String,
    onCardClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (pendingCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "✅ All spend thresholds met!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("⚠️", modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "Threshold Alerts (${pendingCards.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onSeeAllClick) {
                    Text("See all ›", style = MaterialTheme.typography.labelLarge)
                }
            }
            
            AnimatedVisibility(visible = isExpanded) {
                LazyRow(
                    contentPadding = PaddingValues(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pendingCards) { card ->
                        AlertTile(card, currencySim, onClick = { onCardClick(card.cardId) })
                    }
                }
            }
        }
    }
}

@Composable
fun AlertTile(card: CardSpendStatus, currencySim: String, onClick: () -> Unit) {
    val progress = if (card.threshold > 0) (card.currentSpend / card.threshold).coerceIn(0.0, 1.0) else 0.0
    val color = when {
        progress < 0.3 -> Color(0xFFC62828) // Red
        progress < 0.7 -> Color(0xFFEF6C00) // Orange
        else -> Color(0xFF2E7D32) // Green
    }
    
    val remaining = card.threshold - card.currentSpend

    SecondaryCard(
        modifier = Modifier
            .width(160.dp)
            .height(90.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = card.cardName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = color,
                        trackColor = color.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${CurrencyUtils.formatAmount(remaining, currencySim)} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (card.daysLeftInCycle != null && card.daysLeftInCycle in 1..30) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "⏳ ${card.daysLeftInCycle}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.QuickStatusItem(
    icon: ImageVector,
    label: String,
    amount: Double,
    color: Color,
    currencySim: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(Dimens.SpacingSmall))
            .clickable { onClick() }
            .padding(vertical = Dimens.SpacingSmall, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyUtils.formatAmount(amount, currencySim),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun AccountsHomeModule(
    accounts: List<Account>,
    accountBalances: Map<Long, Double>,
    appSettings: AppSettings,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currencySim: String,
    onAccountClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit,
    onSaveCategories: (Set<String>) -> Unit
) {
    var showCustomizeDialog by rememberSaveable { mutableStateOf(false) }

    val enabledCategories = remember(appSettings.homeAccountCategories) {
        if (appSettings.homeAccountCategories.isBlank()) emptySet()
        else appSettings.homeAccountCategories.split(",").toSet()
    }

    val categoryOrder = remember(appSettings.accountTypeOrder) {
        if (appSettings.accountTypeOrder.isBlank()) {
            listOf("CASH", "WALLET", "BANK", "CREDIT_CARD", "GIFT_CARD")
        } else {
            appSettings.accountTypeOrder.split(",")
        }
    }

    val displayedAccounts = remember(accounts, enabledCategories, categoryOrder) {
        val activeAccounts = accounts.filter { it.isActive && !it.isClosed }
        val result = mutableListOf<Account>()

        categoryOrder.forEach { categoryCode ->
            if (enabledCategories.contains(categoryCode)) {
                val group = activeAccounts.filter { it.type == categoryCode }
                result.addAll(group)
            }
        }

        val unhandledTypes = enabledCategories.filter { cat -> !categoryOrder.contains(cat) }
        unhandledTypes.forEach { catCode ->
            val group = activeAccounts.filter { it.type == catCode }
            result.addAll(group)
        }

        result
    }

    val lazyListState = rememberLazyListState()

    val activeCategoryTitle by remember(displayedAccounts, lazyListState) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || displayedAccounts.isEmpty()) {
                ""
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closestItem = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                } ?: visibleItems.first()
                val account = displayedAccounts.getOrNull(closestItem.index)
                account?.let { getHomeCategoryTitle(it.type) } ?: ""
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse Accounts" else "Expand Accounts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = onSeeAllClick) {
                Text("See all ›", style = MaterialTheme.typography.labelLarge)
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.SpacingSmall)
            ) {
                if (enabledCategories.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.SpacingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No accounts selected for Home",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { showCustomizeDialog = true }) {
                            Text("Customize", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else if (displayedAccounts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.SpacingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No active accounts in selected categories",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { showCustomizeDialog = true }) {
                            Text("Customize", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.SpacingSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeCategoryTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { showCustomizeDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Customize",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                        contentPadding = PaddingValues(bottom = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = displayedAccounts,
                            key = { it.id }
                        ) { account ->
                            AccountHomeCard(
                                account = account,
                                balance = accountBalances[account.id] ?: account.currentBalance,
                                currencySim = currencySim,
                                onClick = { onAccountClick(account.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeDialog) {
        val predefinedTypes = listOf(
            "BANK" to "Bank Accounts",
            "CREDIT_CARD" to "Credit Cards",
            "CASH" to "Cash Wallets",
            "WALLET" to "Digital Wallets",
            "GIFT_CARD" to "Gift Cards"
        )
        val customTypes = remember(accounts) {
            accounts.map { it.type }.distinct().filter { type ->
                predefinedTypes.none { it.first == type }
            }.map { type ->
                type to AccountUtils.formatAccountType(type)
            }
        }
        val allCategoryPairs = predefinedTypes + customTypes

        CustomizeHomeAccountsDialog(
            enabledCategories = enabledCategories,
            allCategories = allCategoryPairs,
            onDismiss = { showCustomizeDialog = false },
            onSave = { selected ->
                onSaveCategories(selected)
                showCustomizeDialog = false
            }
        )
    }
}

@Composable
fun AccountHomeCard(
    account: Account,
    balance: Double,
    currencySim: String,
    onClick: () -> Unit
) {
    val formattedBalance = remember(balance, currencySim) {
        if (balance >= 0) {
            CurrencyUtils.formatAmount(balance, currencySim)
        } else {
            "-${CurrencyUtils.formatAmount(kotlin.math.abs(balance), currencySim)}"
        }
    }

    val balanceTextStyle = when {
        formattedBalance.length > 14 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        formattedBalance.length > 10 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
    }

    val balanceColor = when {
        balance > 0 -> Color(0xFF2E7D32)
        balance < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    SecondaryCard(
        modifier = Modifier
            .width(170.dp)
            .height(88.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.SpacingMedium, vertical = Dimens.SpacingSmall),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formattedBalance,
                style = balanceTextStyle,
                color = balanceColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CustomizeHomeAccountsDialog(
    enabledCategories: Set<String>,
    allCategories: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(enabledCategories) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Display on Home",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allCategories.forEach { (code, name) ->
                    val isChecked = selected.contains(code)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (isChecked) selected - code else selected + code
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + code else selected - code
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(selected)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getHomeCategoryTitle(typeCode: String): String {
    return when (typeCode) {
        "BANK" -> "BANK ACCOUNTS"
        "CREDIT_CARD" -> "CREDIT CARDS"
        "CASH" -> "CASH WALLETS"
        "WALLET" -> "DIGITAL WALLETS"
        "GIFT_CARD" -> "GIFT CARDS"
        else -> AccountUtils.formatAccountType(typeCode).uppercase(Locale.ROOT)
    }
}
