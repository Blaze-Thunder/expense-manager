package com.saik.expensemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.IntrinsicSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.ui.viewmodel.AccountSummary
import com.saik.expensemanager.ui.viewmodel.NetWorthSummary
import com.saik.expensemanager.ui.viewmodel.ViewMode
import com.saik.expensemanager.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    initialAccountIdToDetail: Long? = null,
    autoExpandAccountId: Long? = null,
    onBackToAccountsList: ((Long?) -> Unit)? = null,
    onNavigateToGiftCards: ((Long?) -> Unit)? = null,
    scrollToCreditCards: Boolean = false,
    onScrolledToCreditCards: () -> Unit = {},
    onAutoScrollCompleted: () -> Unit = {},
    onAddTransaction: (Transaction?) -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {},
    onAccountSelected: (Account) -> Unit = {},
) {
    var selectedAccountDetailId by rememberSaveable { mutableStateOf<Long?>(initialAccountIdToDetail) }
    var selectedPaymentHistoryAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()

    val selectedAccountDetail = remember(selectedAccountDetailId, accounts) {
        selectedAccountDetailId?.let { id -> accounts.find { it.id == id } }
    }
    val selectedPaymentHistoryAccount = remember(selectedPaymentHistoryAccountId, accounts) {
        selectedPaymentHistoryAccountId?.let { id -> accounts.find { it.id == id } }
    }

    androidx.activity.compose.BackHandler(enabled = (selectedAccountDetail != null) || (selectedPaymentHistoryAccount != null)) {
        val returningId = selectedPaymentHistoryAccountId ?: selectedAccountDetailId
        selectedPaymentHistoryAccountId = null
        selectedAccountDetailId = null
        onBackToAccountsList?.invoke(returningId)
    }

    LaunchedEffect(initialAccountIdToDetail) {
        if ((initialAccountIdToDetail != null) && (initialAccountIdToDetail != -1L)) {
            selectedAccountDetailId = initialAccountIdToDetail
        }
    }

    AnimatedContent(
        targetState = if (selectedPaymentHistoryAccount != null) "payment_history" else if (selectedAccountDetail != null) "detail" else "list",
        transitionSpec = {
            if (targetState == "list") {
                (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
            }
        },
        label = "accounts_navigation_view",
    ) { state ->
        when (state) {
            "list" -> {
                AccountsListView(
                    viewModel = viewModel, 
                    autoExpandAccountId = autoExpandAccountId,
                    highlightedAccountId = autoExpandAccountId,
                    onSelectAccount = { 
                        selectedAccountDetailId = it.id
                        onAccountSelected(it)
                    },
                    onSelectPaymentHistory = {
                        selectedPaymentHistoryAccountId = it.id
                    },
                    scrollToCreditCards = scrollToCreditCards,
                    onScrolledToCreditCards = onScrolledToCreditCards,
                    onAutoScrollCompleted = onAutoScrollCompleted,
                    onAddTransaction = onAddTransaction,
                    onNavigateToGiftCards = onNavigateToGiftCards ?: {}
                )
            }
            "detail" -> {
                selectedAccountDetail?.let { targetAccount ->
                    AccountDetailView(
                        account = targetAccount,
                        viewModel = viewModel,
                        accountBalance = accountBalances[targetAccount.id] ?: 0.0,
                        onBack = {
                            val returningId = selectedAccountDetailId
                            selectedAccountDetailId = null
                            onBackToAccountsList?.invoke(returningId)
                        },
                        onEditTransaction = onEditTransaction
                    )
                }
            }
            "payment_history" -> {
                selectedPaymentHistoryAccount?.let { targetAccount ->
                    PaymentHistoryScreen(
                        account = targetAccount,
                        viewModel = viewModel,
                        onBack = {
                            val returningId = selectedPaymentHistoryAccountId
                            selectedPaymentHistoryAccountId = null
                            onBackToAccountsList?.invoke(returningId)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsListView(
    viewModel: MainViewModel,
    autoExpandAccountId: Long? = null,
    highlightedAccountId: Long? = null,
    onSelectAccount: (Account) -> Unit,
    onSelectPaymentHistory: (Account) -> Unit = {},
    onNavigateToGiftCards: (Long?) -> Unit = {},
    scrollToCreditCards: Boolean = false,
    onScrolledToCreditCards: () -> Unit = {},
    onAutoScrollCompleted: () -> Unit = {},
    onAddTransaction: (Transaction?) -> Unit = {}
) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorthSummary.collectAsStateWithLifecycle()
    val groupTotals by viewModel.accountGroupTotals.collectAsStateWithLifecycle()
    val expandedSections by viewModel.expandedAccountSections.collectAsStateWithLifecycle()
    val collapsedAccountIds by viewModel.collapsedAccountIds.collectAsStateWithLifecycle()

    val currencySim = activeBook?.currencyCode ?: appSettings.currencyCode
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var activeHighlightId by remember { mutableStateOf<Long?>(null) }
    var processedAutoExpandId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(highlightedAccountId) {
        if (highlightedAccountId != null) {
            activeHighlightId = highlightedAccountId
            kotlinx.coroutines.delay(3000)
            activeHighlightId = null
        }
    }

    LaunchedEffect(autoExpandAccountId) {
        if (autoExpandAccountId == null) {
            processedAutoExpandId = null
        }
    }

    // Handle auto-expansion and scrolling
    LaunchedEffect(autoExpandAccountId, accounts) {
        val id = autoExpandAccountId ?: return@LaunchedEffect
        if (accounts.isEmpty() || id == processedAutoExpandId) return@LaunchedEffect

        val account = accounts.find { it.id == id }
        val sectionType = account?.type ?: "CREDIT_CARD"

        val updatedExpandedSections = if (!expandedSections.contains(sectionType)) {
            viewModel.toggleAccountSectionExpansion(sectionType)
            viewModel.expandedAccountSections.first { it.contains(sectionType) }
        } else {
            expandedSections
        }

        if (collapsedAccountIds.contains(id)) {
            viewModel.toggleAccountExpansion(id)
            viewModel.collapsedAccountIds.first { !it.contains(id) }
        }

        // Find index to scroll to
        val groupedAccounts = accounts.groupBy { it.type }
        val accountTypes = listOf("CASH", "WALLET", "BANK", "CREDIT_CARD", "GIFT_CARD")

        var targetIndex = -1
        var currentIndex = 0
        currentIndex++ // NetWorthCard

        for (type in accountTypes) {
            val list = groupedAccounts[type] ?: emptyList()
            if (list.isNotEmpty() || type == "GIFT_CARD") {
                if (type == sectionType) targetIndex = currentIndex
                currentIndex++ // Header

                if (updatedExpandedSections.contains(type)) {
                    if (list.isNotEmpty()) {
                        if (type == sectionType) targetIndex = currentIndex
                        currentIndex++ // Carousel item
                    } else if (type == "GIFT_CARD") {
                        if (type == sectionType) targetIndex = currentIndex
                        currentIndex++ // Empty text item
                    }
                }
            }
        }

        if (targetIndex != -1) {
            processedAutoExpandId = id
            lazyListState.animateScrollToItem(targetIndex)
            if (sectionType == "CREDIT_CARD") {
                onScrolledToCreditCards()
            } else {
                onAutoScrollCompleted()
            }
        }
    }

    LaunchedEffect(scrollToCreditCards, accounts) {
        if (scrollToCreditCards && autoExpandAccountId == null) {
            val groupedAccounts = accounts.groupBy { it.type }
            val accountTypes = listOf("CASH", "WALLET", "BANK", "CREDIT_CARD", "GIFT_CARD")
            var currentIndex = 1
            for (type in accountTypes) {
                if (type == "CREDIT_CARD") {
                    lazyListState.animateScrollToItem(currentIndex)
                    onScrolledToCreditCards()
                    break
                }
                val list = groupedAccounts[type] ?: emptyList()
                if ((list.isNotEmpty()) || (type == "GIFT_CARD")) {
                    currentIndex++
                    if (expandedSections.contains(type)) {
                        if (list.isNotEmpty() || type == "GIFT_CARD") currentIndex++
                    }
                }
            }
        }
    }

    val openAccounts = remember(accounts) { accounts.filter { !it.isClosed && it.isActive } }
    val groupedAccounts = remember(openAccounts) { openAccounts.groupBy { it.type } }

    Scaffold(
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
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = Dimens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            item { NetWorthCard(netWorth, currencySim) }
            
            val accountTypes = listOf(
                "CASH" to "Cash Wallets", 
                "WALLET" to "Digital Wallets", 
                "BANK" to "Bank Accounts", 
                "CREDIT_CARD" to "Credit Cards",
                "GIFT_CARD" to "Gift Cards"
            )

            accountTypes.forEach { (typeCode, typeTitle) ->
                val list = groupedAccounts[typeCode] ?: emptyList()
                val isExpanded = expandedSections.contains(typeCode)

                if ((list.isNotEmpty()) || (typeCode == "GIFT_CARD")) {
                    item(key = "header_$typeCode") {
                        CollapsibleAccountSectionHeader(
                            title = typeTitle,
                            isExpanded = isExpanded,
                            onToggle = { 
                                val expanding = !isExpanded
                                viewModel.toggleAccountSectionExpansion(typeCode)
                                if (expanding) {
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(100)
                                        var targetHeaderIndex = 0
                                        targetHeaderIndex++ // NetWorthCard (index 0)
                                        for ((code, _) in accountTypes) {
                                            val subList = groupedAccounts[code] ?: emptyList()
                                            if (subList.isNotEmpty() || code == "GIFT_CARD") {
                                                if (code == typeCode) {
                                                    lazyListState.animateScrollToItem(targetHeaderIndex)
                                                    break
                                                }
                                                targetHeaderIndex++ // Header
                                                val isCurrentlyExpanded = expandedSections.contains(code)
                                                if (isCurrentlyExpanded) {
                                                    if (subList.isNotEmpty() || code == "GIFT_CARD") targetHeaderIndex++
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            headerTrailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (typeCode != "CREDIT_CARD" && (groupTotals[typeCode] ?: 0.0) != 0.0) {
                                        Text(
                                            text = CurrencyUtils.formatAmount(groupTotals[typeCode] ?: 0.0, currencySim),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if ((groupTotals[typeCode] ?: 0.0) >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                    
                                    if (typeCode == "GIFT_CARD") {
                                        TextButton(onClick = { onNavigateToGiftCards(null) }) {
                                            Text("View all", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            },
                            alwaysVisibleContent = {
                                if (typeCode == "CREDIT_CARD") {
                                    val ccTotal by viewModel.creditCardTotalSummary.collectAsStateWithLifecycle()
                                    CreditCardGlobalSummary(
                                        totalOutstanding = ccTotal.first,
                                        totalPayable = ccTotal.second,
                                        currencySim = currencySim
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                                }
                            }
                        )
                    }

                    if (isExpanded) {
                        if (list.isEmpty() && typeCode == "GIFT_CARD") {
                            item {
                                Text(
                                    "No gift cards added. Track your vouchers and gift cards here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = Dimens.SpacingSmall)
                                )
                            }
                        } else if (list.isNotEmpty()) {
                            item(key = "carousel_$typeCode") {
                                val cardWidth = (LocalConfiguration.current.screenWidthDp.dp - Dimens.SpacingLarge * 2) * 0.85f
                                val horizontalScrollState = rememberScrollState()
                                val density = LocalDensity.current

                                LaunchedEffect(activeHighlightId, typeCode) {
                                    val targetId = activeHighlightId ?: return@LaunchedEffect
                                    val idx = list.indexOfFirst { it.id == targetId }
                                    if (idx < 0) return@LaunchedEffect
                                    kotlinx.coroutines.delay(120)
                                    val cardWidthPx = with(density) { cardWidth.toPx() }
                                    val spacingPx   = with(density) { 12.dp.toPx() }
                                    horizontalScrollState.animateScrollTo(((cardWidthPx + spacingPx) * idx).toInt())
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScrollState)
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    list.forEach { acc ->
                                        if (acc.isCreditCard) {
                                            CreditCardAccountCard(
                                                account = acc, 
                                                viewModel = viewModel, 
                                                currencySim = currencySim, 
                                                highlightedAccountId = activeHighlightId,
                                                modifier = Modifier
                                                    .width(cardWidth)
                                                    .fillMaxHeight(),
                                                onClick = { onSelectAccount(acc) },
                                                onPaymentHistoryClick = { onSelectPaymentHistory(acc) }
                                            ) { amount, accountId ->
                                                val prefilledTx = Transaction(
                                                    amount = amount,
                                                    type = "TRANSFER",
                                                    linkedAccountId = -1L, 
                                                    toAccountId = accountId, 
                                                    description = "Credit Card Payment",
                                                    dateTime = System.currentTimeMillis(),
                                                    linkedBudgetBookId = activeBook?.id ?: -1L,
                                                    category = "",
                                                    isCreditCardPayment = true
                                                )
                                                onAddTransaction(prefilledTx)
                                            }
                                        } else {
                                            AccountBalanceCard(
                                                account = acc, 
                                                viewModel = viewModel, 
                                                currencySim = currencySim, 
                                                highlightedAccountId = activeHighlightId,
                                                modifier = Modifier
                                                    .width(cardWidth)
                                                    .fillMaxHeight(),
                                                onClick = { 
                                                    if (acc.type == "GIFT_CARD") onNavigateToGiftCards(acc.id)
                                                    else onSelectAccount(acc)
                                                },
                                                onPaymentHistoryClick = { onSelectPaymentHistory(acc) }
                                            ) { amount, accountId ->
                                                val prefilledTx = Transaction(
                                                    amount = amount,
                                                    type = "TRANSFER",
                                                    linkedAccountId = -1L, 
                                                    toAccountId = accountId, 
                                                    description = "Credit Card Payment",
                                                    dateTime = System.currentTimeMillis(),
                                                    linkedBudgetBookId = activeBook?.id ?: -1L,
                                                    category = "",
                                                    isCreditCardPayment = true
                                                )
                                                onAddTransaction(prefilledTx)
                                            } 
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                            Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                            Text("No accounts found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun NetWorthCard(summary: NetWorthSummary, currencySim: String) {
    SecondaryCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Net Worth",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val netWorthColor = if (summary.netWorth >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            Text(
                text = CurrencyUtils.formatAmountWithSign(summary.netWorth, currencySim),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = netWorthColor
            )
            
            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Assets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyUtils.formatAmount(summary.totalAssets, currencySim),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                
                VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = Dimens.SpacingLarge))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Credit Dues", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyUtils.formatAmount(summary.totalCreditDues, currencySim),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountBalanceCard(
    account: Account,
    viewModel: MainViewModel,
    currencySim: String,
    modifier: Modifier = Modifier,
    highlightedAccountId: Long? = null,
    onClick: () -> Unit,
    onPaymentHistoryClick: () -> Unit = {},
    onMarkAsPaid: (Double, Long) -> Unit
) {
    val collapsedIds by viewModel.collapsedAccountIds.collectAsStateWithLifecycle()
    val isExpanded = !collapsedIds.contains(account.id)
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    val balance by viewModel.getAccountBalanceFlow(account.id).collectAsStateWithLifecycle(0.0)
    val insights by viewModel.computeCreditCardInsights(account).collectAsStateWithLifecycle(null)

    val isHighlighted = account.id == highlightedAccountId

    val cardColors = if (isHighlighted) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val cardBorder = if (isHighlighted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        )
    }

    SecondaryCard(
        modifier = modifier.clickable { viewModel.toggleAccountExpansion(account.id) },
        colors = cardColors,
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.SpacingLarge)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AccountUtils.getAccountIcon(account.iconName), 
                            null, 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            account.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            AccountUtils.formatAccountType(account.type),
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.widthIn(min = 80.dp)
                ) {
                    val finalColor = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val cleanBalance = balance
                    Text(
                        text = CurrencyUtils.formatAmountWithSign(if (account.isCreditCard) -cleanBalance else cleanBalance, currencySim),
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = finalColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                        textAlign = TextAlign.End
                    )
                    Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpacingSmall), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                if (account.isCreditCard) {
                    insights?.let { data ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            InsightRow("Cycle Window", "Day ${account.billingCycleStartDay} to ${account.billingCycleEndDay}")
                            InsightRow("Outstanding", CurrencyUtils.formatAmount(data.outstandingBalance, currencySim), color = MaterialTheme.colorScheme.error)
                            InsightRow(
                                label = "Payable Balance", 
                                value = CurrencyUtils.formatAmount(data.payableBalance, currencySim), 
                                color = if (data.payableBalance <= 0) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                            
                            val payAmount = if (data.payableBalance > 0) data.payableBalance else data.outstandingBalance
                            if (payAmount > 0) {
                                Button(
                                    onClick = { onMarkAsPaid(payAmount, account.id) },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(Dimens.SpacingSmall),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pay Now", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            
                            InsightRow("Current Cycle Spend", CurrencyUtils.formatAmount(data.currentCycleSpend, currencySim))
                            InsightRow("Paid This Cycle", CurrencyUtils.formatAmount(data.paidThisCycle, currencySim), color = Color(0xFF2E7D32))
                            InsightRow("Credits Applied", CurrencyUtils.formatAmount(data.creditsAppliedThisCycle, currencySim), color = Color(0xFF0288D1))
                            
                            if (account.trackFeeWaiver) {
                                FeeWaiverProgressBlock(data, currencySim)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                        Button(
                            onClick = onClick, 
                            modifier = Modifier.weight(1f), 
                            shape = RoundedCornerShape(Dimens.SpacingSmall),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Detailed Ledger", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                        OutlinedButton(
                            onClick = onPaymentHistoryClick, 
                            modifier = Modifier.weight(1f), 
                            shape = RoundedCornerShape(Dimens.SpacingSmall),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Payment History", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }

                    OutlinedButton(
                        onClick = { showAdjustBalanceDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpacingSmall),
                        shape = RoundedCornerShape(Dimens.SpacingSmall),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Balance, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adjust Balance", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))

                    val buttonLabel = if (account.type == "GIFT_CARD") "View all cards" else "Detailed Ledger"
                    Button(
                        onClick = onClick, 
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpacingSmall), 
                        shape = RoundedCornerShape(Dimens.SpacingSmall),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(buttonLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    if (account.type != "GIFT_CARD") {
                        OutlinedButton(
                            onClick = { showAdjustBalanceDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpacingSmall),
                            shape = RoundedCornerShape(Dimens.SpacingSmall),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Balance, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Adjust Balance", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    if (showAdjustBalanceDialog) {
        AdjustBalanceDialog(
            account = account,
            currentCalculatedBalance = balance,
            currencySim = currencySim,
            onDismiss = { showAdjustBalanceDialog = false },
            onConfirm = { targetBalance ->
                viewModel.correctAccountBalance(account, targetBalance)
            }
        )
    }
}

@Composable
fun InsightRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun FeeWaiverProgressBlock(data: com.saik.expensemanager.ui.viewmodel.CreditCardDetailedInsights, currencySim: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fee Waiver Progress", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (data.isFeeThresholdReached) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Text("Reached", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            } else {
                val progressPercent = if (data.feeWaiverThreshold > 0) ((data.feeWaiverProgress / data.feeWaiverThreshold) * 100).toInt().coerceIn(0, 100) else 0
                Text("$progressPercent%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        val progressFraction = if (data.feeWaiverThreshold > 0) (data.feeWaiverProgress / data.feeWaiverThreshold).coerceIn(0.0, 1.0) else 0.0
        LinearProgressIndicator(
            progress = { progressFraction.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = if (data.isFeeThresholdReached) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${CurrencyUtils.formatAmount(data.feeWaiverProgress, currencySim)} / ${CurrencyUtils.formatAmount(data.feeWaiverThreshold, currencySim)}",
                style = MaterialTheme.typography.labelSmall
            )
            if (!data.isFeeThresholdReached) {
                val remaining = maxOf(0.0, data.feeWaiverThreshold - data.feeWaiverProgress)
                Text(
                    text = "Spend ${CurrencyUtils.formatAmount(remaining, currencySim)} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CreditCardGlobalSummary(
    totalOutstanding: Double,
    totalPayable: Double,
    currencySim: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
    ) {
        SecondaryCard(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = Dimens.SpacingMedium, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Total Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = CurrencyUtils.formatAmount(totalOutstanding, currencySim),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Across all cards",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        SecondaryCard(
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = Dimens.SpacingMedium, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Total Payable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = CurrencyUtils.formatAmount(totalPayable, currencySim),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (totalPayable > 0) "Currently due" else "No payments due",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CreditCardAccountCard(
    account: Account,
    viewModel: MainViewModel,
    currencySim: String,
    modifier: Modifier = Modifier,
    highlightedAccountId: Long? = null,
    onClick: () -> Unit,
    onPaymentHistoryClick: () -> Unit = {},
    onMarkAsPaid: (Double, Long) -> Unit
) {
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    val balance by viewModel.getAccountBalanceFlow(account.id).collectAsStateWithLifecycle(0.0)
    val insights by viewModel.computeCreditCardInsights(account).collectAsStateWithLifecycle(null)

    val isHighlighted = account.id == highlightedAccountId

    val cardColors = if (isHighlighted) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val cardBorder = if (isHighlighted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        )
    }

    SecondaryCard(
        modifier = modifier,
        colors = cardColors,
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.SpacingMedium)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AccountUtils.getAccountIcon(account.iconName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Cycle: ${account.billingCycleStartDay}th–${account.billingCycleEndDay}th",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Dimens.SpacingSmall))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val outstanding = insights?.outstandingBalance ?: -balance
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyUtils.formatAmount(outstanding, currencySim),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (outstanding > 0) Color(0xFFC62828) else if (outstanding < 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (outstanding < 0) "Credit Balance" else "Outstanding",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            insights?.let { data ->
                // Balance Summary Row (Payable balance vs Current cycle spend)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Payable balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = CurrencyUtils.formatAmount(data.payableBalance, currencySim),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (data.payableBalance > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Current cycle spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = CurrencyUtils.formatAmount(data.currentCycleSpend, currencySim),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (data.currentCycleSpend < 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Fee Waiver Progress Block - Fixed height container for visual consistency across cards
                Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    if (account.trackFeeWaiver && account.feeWaiverThreshold != null && account.feeWaiverThreshold > 0) {
                        FeeWaiverProgressBlock(data, currencySim)
                    }
                }

                // Cycle Activity Metrics (Paid this cycle & Credits applied)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Paid this cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatAmount(data.paidThisCycle, currencySim),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Credits applied", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatAmount(data.creditsAppliedThisCycle, currencySim),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0288D1)
                            )
                        }
                    }
                }

                // Primary Payment Action
                val payAmount = if (data.payableBalance > 0) data.payableBalance else data.outstandingBalance
                Button(
                    onClick = { onMarkAsPaid(payAmount, account.id) },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = if (data.payableBalance > 0) "Pay Statement Due (${CurrencyUtils.formatAmount(data.payableBalance, currencySim)})" else "Make a payment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Secondary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Detailed Ledger", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onPaymentHistoryClick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Payment History", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showAdjustBalanceDialog = true }) {
                    Text("Adjust balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(
                    onClick = { showAdjustBalanceDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showAdjustBalanceDialog) {
        AdjustBalanceDialog(
            account = account,
            currentCalculatedBalance = balance,
            currencySim = currencySim,
            onDismiss = { showAdjustBalanceDialog = false },
            onConfirm = { targetBalance ->
                viewModel.correctAccountBalance(account, targetBalance)
            }
        )
    }
}

@Composable
fun StatementCol(
    label: String,
    amount: Double,
    color: Color,
    currencySim: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = CurrencyUtils.formatAmount(amount, currencySim),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailView(account: Account, viewModel: MainViewModel, onBack: () -> Unit, accountBalance: Double, onEditTransaction: (Transaction) -> Unit = {}) {
    val accountsCal by viewModel.accountsCalendar.collectAsStateWithLifecycle()
    val accountsViewMode by viewModel.accountsViewMode.collectAsStateWithLifecycle()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()

    val currencySim = activeBook?.currencyCode ?: appSettings.currencyCode

    var lastTime by remember { mutableLongStateOf(accountsCal.timeInMillis) }
    var slideDirection by remember { mutableIntStateOf(1) }

    LaunchedEffect(accountsCal.timeInMillis) {
        if (accountsCal.timeInMillis != lastTime) {
            slideDirection = if (accountsCal.timeInMillis > lastTime) 1 else -1
            lastTime = accountsCal.timeInMillis
        }
    }

    LaunchedEffect(account.id) {
        viewModel.setAccountsViewMode(ViewMode.DAILY, account)
    }

    val periodSummary by viewModel.computeAccountSummary(account.id, account).collectAsStateWithLifecycle(AccountSummary(0.0, 0.0, 0.0, emptyList()))
    val groupedSummary by viewModel.computeAccountGroupedSummary(account.id, account).collectAsStateWithLifecycle(emptyList())

    val displayTransactions = periodSummary.transactions

    // Daily closing balances for the balance display on date headers (full account balance,
    // not affected by any per-screen filters, computed chronologically oldest-to-newest).
    val dailyBalances by viewModel.getDailyBalancesForAccount(account.id)
        .collectAsStateWithLifecycle(emptyMap())

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                val periodLabel = when (accountsViewMode) {
                    ViewMode.DAILY -> SimpleDateFormat("MMM yyyy", Locale.US).format(accountsCal.time)
                    ViewMode.MONTHLY -> if (account.isCreditCard) SimpleDateFormat("yyyy", Locale.US).format(accountsCal.time) else SimpleDateFormat("yyyy", Locale.US).format(accountsCal.time)
                    ViewMode.YEARLY -> SimpleDateFormat("yyyy", Locale.US).format(accountsCal.time)
                    else -> ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    
                    Text(
                        account.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { viewModel.selectAccountsOffset(-1, account) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false
                        )
                        IconButton(
                            onClick = { viewModel.selectAccountsOffset(1, account) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                val modes = listOf("Daily", "Monthly", "Yearly")
                val selectedMode = when (accountsViewMode) {
                    ViewMode.DAILY -> "Daily"
                    ViewMode.MONTHLY -> "Monthly"
                    ViewMode.YEARLY -> "Yearly"
                    else -> "Monthly"
                }

                SegmentedPeriodSelector(
                    modes = modes,
                    selectedMode = selectedMode,
                    onModeSelected = { mode ->
                        val targetMode = when (mode) {
                            "Daily" -> ViewMode.DAILY
                            "Monthly" -> ViewMode.MONTHLY
                            "Yearly", "Annually" -> ViewMode.YEARLY
                            else -> ViewMode.MONTHLY
                        }
                        viewModel.setAccountsViewMode(targetMode, account)
                    }
                )

                // Statement Header for all account types
                Column(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall)) {
                    val sdfHeader = SimpleDateFormat("dd.MM.yyyy", Locale.US)
                    val (startLabel, rangeLabel) = when (accountsViewMode) {
                        ViewMode.DAILY -> {
                            if (account.isCreditCard) {
                                // accountsCal is already set to the cycle start day by the ViewModel
                                val cycleStart = accountsCal
                                val cycleEnd = (cycleStart.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.DAY_OF_YEAR, -1) }
                                "Statement" to "${sdfHeader.format(cycleStart.time)} ~ ${sdfHeader.format(cycleEnd.time)}"
                            } else {
                                val monthStart = (accountsCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                                val monthEnd = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.DAY_OF_YEAR, -1) }
                                "Statement Period" to "${sdfHeader.format(monthStart.time)} ~ ${sdfHeader.format(monthEnd.time)}"
                            }
                        }
                        ViewMode.MONTHLY, ViewMode.YEARLY -> {
                            if (account.isCreditCard) {
                                val firstMonth = (accountsCal.clone() as Calendar).apply { set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1) }
                                val cycleStart = (firstMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay) }
                                val cycleEnd = (cycleStart.clone() as Calendar).apply { add(Calendar.YEAR, 1); add(Calendar.MILLISECOND, -1) }
                                "Annual Period" to "${sdfHeader.format(cycleStart.time)} ~ ${sdfHeader.format(cycleEnd.time)}"
                            } else {
                                val yearStart = (accountsCal.clone() as Calendar).apply { set(Calendar.DAY_OF_YEAR, 1) }
                                val yearEnd = (yearStart.clone() as Calendar).apply { add(Calendar.YEAR, 1); add(Calendar.MILLISECOND, -1) }
                                "Annual Period" to "${sdfHeader.format(yearStart.time)} ~ ${sdfHeader.format(yearEnd.time)}"
                            }
                        }
                        else -> "" to ""
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(rangeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatementCol(
                            label = "Income",
                            amount = periodSummary.totalIncome,
                            color = Color(0xFF2E7D32),
                            currencySim = currencySim,
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        )
                        StatementCol(
                            label = "Expense",
                            amount = periodSummary.totalExpense,
                            color = Color(0xFFC62828),
                            currencySim = currencySim,
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        val isCc = account.isCreditCard
                        val isPositive = if (isCc) accountBalance >= 0 else accountBalance >= 0

                        StatementCol(
                            label = if (isCc) {
                                if (isPositive) "Credit Balance" else "Outstanding"
                            } else "Balance",
                            amount = if (isCc) kotlin.math.abs(accountBalance) else accountBalance,
                            color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828),
                            currencySim = currencySim,
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        )
                        /*
                        StatementCol(
                            label = "Total",
                            amount = periodSummary.netBalance,
                            color = MaterialTheme.colorScheme.onSurface,
                            currencySim = currencySim,
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        )

                         */
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        }
    ) { innerPadding ->
        var horizontalDragAmount by remember { mutableFloatStateOf(0f) }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pointerInput(accountsViewMode, accountsCal) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (horizontalDragAmount > 100) {
                                viewModel.selectAccountsOffset(-1)
                            } else if (horizontalDragAmount < -100) {
                                viewModel.selectAccountsOffset(1)
                            }
                            horizontalDragAmount = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        horizontalDragAmount += dragAmount
                    }
                }
        ) {
            AnimatedContent(
                targetState = accountsCal.timeInMillis,
                transitionSpec = {
                    if (slideDirection > 0) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "accounts_detail_period_transition"
            ) { time ->
                println("Accounts detail time: $time")
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
                ) {
                    SectionHeader(
                        when (accountsViewMode) {
                            ViewMode.DAILY -> "Transactions"
                            ViewMode.MONTHLY -> "Monthly Breakdown"
                            else -> ""
                        }, 
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                    
                    if (accountsViewMode == ViewMode.MONTHLY) {
                        if (groupedSummary.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No records", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                items(groupedSummary) { item ->
                                    val isSelected = (item.label == SimpleDateFormat("MMM", Locale.US).format(accountsCal.time))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(horizontal = Dimens.SpacingLarge, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text(item.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(item.dateRange, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        
                                        Row(modifier = Modifier.weight(3f), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                                Text(CurrencyUtils.formatAmount(item.income, currencySim, false), color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                                Text(CurrencyUtils.formatAmount(item.expense, currencySim, false), color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1.2f)) {
                                                Text(CurrencyUtils.formatAmount(item.net, currencySim, false), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    } else if (accountsViewMode == ViewMode.YEARLY) {
                        // Display only summary of the year (already in the header), so list is empty as requested
                        Box(modifier = Modifier.fillMaxWidth().weight(1f))
                    } else {
                        if (displayTransactions.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No records", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                                    item {
                                        TransactionListSection(
                                            transactions = displayTransactions,
                                            categories = categories,
                                            accounts = accounts,
                                            currencySim = currencySim,
                                            onTransactionClick = onEditTransaction,
                                            showDailyBalances = dailyBalances
                                        )
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
fun CollapsibleAccountSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    headerTrailing: @Composable () -> Unit = {},
    alwaysVisibleContent: @Composable () -> Unit = {}
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
            
            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            headerTrailing()
        }

        alwaysVisibleContent()
    }
}
