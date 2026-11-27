package com.saik.expensemanager

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.saik.expensemanager.util.BiometricAuthHelper
import com.saik.expensemanager.ui.viewmodel.AppLockViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saik.expensemanager.ui.screens.*
import com.saik.expensemanager.ui.theme.MyApplicationTheme
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.data.model.displayBrandName

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val lockViewModel: AppLockViewModel = viewModel()
            val settings by viewModel.appSettings.collectAsStateWithLifecycle()

            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = remember(settings.theme, systemInDark) {
                when (settings.theme) {
                    "DARK" -> true
                    "LIGHT" -> false
                    else -> systemInDark
                }
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                colorTheme = settings.colorTheme,
            ) {
                AppRoot(viewModel = viewModel, lockViewModel = lockViewModel)
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: MainViewModel, lockViewModel: AppLockViewModel) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isUnlocked by lockViewModel.isUnlocked.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as FragmentActivity
    val biometricHelper = remember { BiometricAuthHelper(activity) }

    LaunchedEffect(settings.appLockEnabled, isLoading) {
        if (!isLoading) {
            lockViewModel.initUnlockState(settings.appLockEnabled)
        }
    }

    AppLockLifecycleObserver(
        isAppLockEnabled = settings.appLockEnabled
    ) { lockViewModel.lock() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoading) {
            if (!settings.hasCompletedOnboarding) {
                OnboardingScreen(
                    viewModel = viewModel
                ) { name, budget, currency ->
                    viewModel.completeOnboarding(name, budget, currency)
                }
            } else {
                val saveableStateHolder = rememberSaveableStateHolder()
                MainAppScaffold(
                    viewModel = viewModel,
                    saveableStateHolder = saveableStateHolder,
                ) { activity.finishAffinity() }
            }
        }

        if (isLoading) {
            LoadingScreen()
        } else if (settings.appLockEnabled && !isUnlocked) {
            LockScreen(
                settings = settings,
                onPinVerified = { lockViewModel.unlock() },
            ) {
                biometricHelper.authenticate(
                    onSuccess = { lockViewModel.unlock() },
                    onError = { /* no-op, stays on PIN screen */ }
                ) { /* no-op, let user retry */ }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading your finances...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val TransactionSaver = Saver<com.saik.expensemanager.data.model.Transaction?, Map<String, Any?>>(
    save = { transaction ->
        transaction?.let {
            mapOf(
                "id" to it.id,
                "dateTime" to it.dateTime,
                "amount" to it.amount,
                "type" to it.type,
                "category" to it.category,
                "subCategory" to it.subCategory,
                "description" to it.description,
                "linkedAccountId" to it.linkedAccountId,
                "toAccountId" to it.toAccountId,
                "linkedBudgetBookId" to it.linkedBudgetBookId,
                "repetitiveId" to it.repetitiveId,
                "repetitiveOccurrenceDate" to it.repetitiveOccurrenceDate,
                "isGeneratedFromRecurrence" to it.isGeneratedFromRecurrence,
                "linkedGiftCardId" to it.linkedGiftCardId,
                "isCreditCardPayment" to it.isCreditCardPayment
            )
        } ?: emptyMap()
    },
    restore = { map ->
        if (map.isEmpty()) null
        else com.saik.expensemanager.data.model.Transaction(
            id = (map["id"] as Number).toLong(),
            dateTime = (map["dateTime"] as Number).toLong(),
            amount = (map["amount"] as Number).toDouble(),
            type = map["type"] as String,
            category = map["category"] as String,
            subCategory = map["subCategory"] as String?,
            description = map["description"] as String? ?: "",
            linkedAccountId = (map["linkedAccountId"] as Number).toLong(),
            toAccountId = (map["toAccountId"] as? Number)?.toLong(),
            linkedBudgetBookId = (map["linkedBudgetBookId"] as Number).toLong(),
            repetitiveId = (map["repetitiveId"] as? Number)?.toLong(),
            repetitiveOccurrenceDate = (map["repetitiveOccurrenceDate"] as? Number)?.toLong(),
            isGeneratedFromRecurrence = map["isGeneratedFromRecurrence"] as? Boolean ?: false,
            linkedGiftCardId = (map["linkedGiftCardId"] as? Number)?.toLong(),
            isCreditCardPayment = map["isCreditCardPayment"] as? Boolean ?: false
        )
    }
)

@Composable
fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onToggleSkip: (Boolean) -> Unit,
) {
    var skipChecked by rememberSaveable { mutableStateOf(value = false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Exit App?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Are you sure you want to exit the Expense Manager?")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { 
                        skipChecked = !skipChecked
                        onToggleSkip(skipChecked)
                    }
                ) {
                    Checkbox(
                        checked = skipChecked,
                        onCheckedChange = { 
                            skipChecked = it
                            onToggleSkip(it)
                        }
                    )
                    Text(text = "Don't ask again", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Exit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

sealed class NavigationTab(val route: String, val title: String, val icon: ImageVector) {
    data object Home : NavigationTab("home", "Home", Icons.Default.Home)
    data object Transactions : NavigationTab("transactions", "Txns", Icons.AutoMirrored.Filled.ListAlt)
    data object Stats : NavigationTab("stats", "Stats", Icons.Default.PieChart)
    data object Accounts : NavigationTab("accounts", "Accounts", Icons.Default.AccountBalanceWallet)
    data object Settings : NavigationTab("settings", "Settings", Icons.Default.Settings)
    data object AccountsManagement : NavigationTab("accounts_management", "Accounts Management", Icons.Default.AccountBalanceWallet)
    data object CategoryManagement : NavigationTab("categories_management", "Categories", Icons.Default.Category)
    data object RecurringTransactions : NavigationTab("recurring_transactions", "Recurring Transactions", Icons.Default.Sync)
    data object BackupRestore : NavigationTab("backup_restore", "Backup & Restore", Icons.Default.Backup)
    data object CloudBackup : NavigationTab("cloud_backup", "Cloud Backup", Icons.Default.CloudUpload)
    data object GiftCards : NavigationTab("gift_cards", "Gift Cards", Icons.Default.CardGiftcard)
    data object SetupPin : NavigationTab("setup_pin", "Setup PIN", Icons.Default.Lock)
    data object AppLockSettings : NavigationTab("app_lock_settings", "App Lock Settings", Icons.Default.Security)
    data object VerifyPin : NavigationTab("verify_pin", "Verify PIN", Icons.Default.Lock)
    data object BudgetBooksManagement : NavigationTab("budget_books_management", "Budget Books", Icons.Default.Book)
    data object ManageTemplates : NavigationTab("manage_templates", "Manage Templates", Icons.Default.Style)
}

@Composable
fun OneUIBottomBar(
    tabs: List<NavigationTab>,
    activeTabRoute: String,
    onTabClick: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(50)
    val itemShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = pillShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(6.dp, pillShape, clip = false),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val selected = activeTabRoute == tab.route
                    val bgColor by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        label = "tab_bg",
                    )
                    val contentColor by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "tab_content",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(itemShape)
                            .background(bgColor)
                            .clickable { onTabClick(tab) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(tab.icon, tab.title, tint = contentColor, modifier = Modifier.size(22.dp))
                        Text(
                            tab.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel, 
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    onExit: () -> Unit,
) {
    var activeTabRoute by rememberSaveable { mutableStateOf(NavigationTab.Home.route) }
    var drillAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var autoExpandAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var drillGiftCardAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var shouldScrollToCreditCards by rememberSaveable { mutableStateOf(value = false) }
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(value = false) }

    var showTransactionEntry by rememberSaveable { mutableStateOf(value = false) }
    var transactionToEdit by rememberSaveable(stateSaver = TransactionSaver) {
        mutableStateOf<com.saik.expensemanager.data.model.Transaction?>(null)
    }
    var pinVerificationSuccessAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val tabs = listOf(
        NavigationTab.Home,
        NavigationTab.Transactions,
        NavigationTab.Stats,
        NavigationTab.Accounts,
        NavigationTab.Settings
    )

    val mainTabsRoutes = tabs.map { it.route }
    val pagerState = rememberPagerState { tabs.size }
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val selectedBudgetBookId by viewModel.selectedBudgetBookId.collectAsStateWithLifecycle()

    var lastMainTabRoute by rememberSaveable { mutableStateOf(NavigationTab.Home.route) }

    LaunchedEffect(activeTabRoute) {
        if (activeTabRoute in mainTabsRoutes) {
            lastMainTabRoute = activeTabRoute
        }
    }

    // Sync Pager with activeTabRoute (for clicks and external navigation)
    LaunchedEffect(activeTabRoute) {
        val targetPage = mainTabsRoutes.indexOf(activeTabRoute)
        if ((targetPage != -1) && (pagerState.currentPage != targetPage)) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync activeTabRoute with Pager index (for swiping)
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val route = mainTabsRoutes[pagerState.currentPage]
            if ((activeTabRoute != route) && (activeTabRoute in mainTabsRoutes)) {
                activeTabRoute = route
            }
        }
    }

    androidx.activity.compose.BackHandler {
        val isSubPage = ((activeTabRoute == NavigationTab.AccountsManagement.route) || 
            (activeTabRoute == NavigationTab.CategoryManagement.route) ||
            (activeTabRoute == NavigationTab.RecurringTransactions.route) ||
            (activeTabRoute == NavigationTab.BackupRestore.route) ||
            (activeTabRoute == NavigationTab.CloudBackup.route) ||
            (activeTabRoute == NavigationTab.AppLockSettings.route) ||
            (activeTabRoute == NavigationTab.VerifyPin.route) ||
            (activeTabRoute == NavigationTab.SetupPin.route) ||
            (activeTabRoute == NavigationTab.BudgetBooksManagement.route) ||
            (activeTabRoute == NavigationTab.ManageTemplates.route))
        
        when {
            showTransactionEntry -> {
                showTransactionEntry = false
                transactionToEdit = null
            }
            isSubPage -> {
                val subPagesNavigatingToSettings = listOf(
                    NavigationTab.AccountsManagement.route,
                    NavigationTab.CategoryManagement.route,
                    NavigationTab.RecurringTransactions.route,
                    NavigationTab.BackupRestore.route,
                    NavigationTab.CloudBackup.route,
                    NavigationTab.AppLockSettings.route,
                    NavigationTab.VerifyPin.route,
                    NavigationTab.SetupPin.route,
                    NavigationTab.BudgetBooksManagement.route,
                    NavigationTab.ManageTemplates.route
                )
                if (activeTabRoute in subPagesNavigatingToSettings) {
                    activeTabRoute = NavigationTab.Settings.route
                } else {
                    activeTabRoute = lastMainTabRoute
                }
            }
            activeTabRoute == NavigationTab.GiftCards.route -> {
                activeTabRoute = lastMainTabRoute
            }
            activeTabRoute != NavigationTab.Home.route -> {
                activeTabRoute = NavigationTab.Home.route
            }
            else -> {
                if (appSettings.skipExitConfirmation) {
                    onExit()
                } else {
                    showExitDialog = true
                }
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = { onExit() },
            onDismiss = { showExitDialog = false },
        ) { skip -> 
            viewModel.updateSettings(
                appSettings.theme, 
                appSettings.colorTheme,
                appSettings.language, 
                appSettings.budgetAlertThreshold, 
                skip
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (!showTransactionEntry) {
                OneUIBottomBar(
                    tabs = tabs,
                    activeTabRoute = activeTabRoute,
                    onTabClick = { tab ->
                        if (activeTabRoute != tab.route) {
                            when (tab.route) {
                                NavigationTab.Home.route -> viewModel.resetTransactionsCalendar()
                                NavigationTab.Transactions.route -> {
                                    viewModel.resetTransactionsCalendar()
                                    viewModel.setViewMode(com.saik.expensemanager.ui.viewmodel.ViewMode.DAILY)
                                }
                                NavigationTab.Stats.route -> {
                                    viewModel.resetStatsCalendar()
                                    viewModel.setStatsViewMode(0)
                                }
                            }
                            activeTabRoute = tab.route
                            if (tab.route != NavigationTab.Accounts.route) drillAccountId = null
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(bottom = if (showTransactionEntry) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            val isSubPage = activeTabRoute !in mainTabsRoutes
            
            AnimatedContent(
                targetState = if (isSubPage) activeTabRoute else "main_pager_layer",
                transitionSpec = {
                    val wasSub = initialState != "main_pager_layer"
                    val isSub = targetState != "main_pager_layer"
                    if (wasSub != isSub) {
                        if (isSub) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    } else {
                        // Snap transition for internal sub-page changes if any
                        fadeIn() togetherWith fadeOut()
                    }
                },
                label = "navigation_layer_transition"
            ) { targetStateKey ->
                val isSubMode = targetStateKey != "main_pager_layer"
                saveableStateHolder.SaveableStateProvider(targetStateKey) {
                    if (isSubMode) {
                        when (targetStateKey) {
                            NavigationTab.AccountsManagement.route -> {
                                AccountsManagementScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = NavigationTab.Settings.route }
                            }
                            NavigationTab.CategoryManagement.route -> {
                                CategoryManagementScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = NavigationTab.Settings.route }
                            }
                            NavigationTab.RecurringTransactions.route -> {
                                RecurringTransactionsScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = NavigationTab.Settings.route }
                            }
                            NavigationTab.BackupRestore.route -> {
                                BackupRestoreScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = NavigationTab.Settings.route }
                            }
                            NavigationTab.CloudBackup.route -> {
                                CloudBackupScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = NavigationTab.Settings.route }
                            }
                            NavigationTab.BudgetBooksManagement.route -> {
                                BudgetBooksScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = NavigationTab.Settings.route }
                            }
                            NavigationTab.AppLockSettings.route -> {
                                AppLockSettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { activeTabRoute = NavigationTab.Settings.route },
                                    onNavigateToSetupPin = { activeTabRoute = NavigationTab.SetupPin.route }
                                ) { onSuccess ->
                                    pinVerificationSuccessAction = onSuccess
                                    activeTabRoute = NavigationTab.VerifyPin.route
                                }
                            }
                            NavigationTab.VerifyPin.route -> {
                                VerifyPinScreen(
                                    viewModel = viewModel,
                                    onSuccess = {
                                        val action = pinVerificationSuccessAction
                                        pinVerificationSuccessAction = null
                                        action?.invoke()
                                        activeTabRoute = NavigationTab.AppLockSettings.route
                                    }
                                ) {
                                    pinVerificationSuccessAction = null
                                    activeTabRoute = NavigationTab.AppLockSettings.route
                                }
                            }
                            NavigationTab.SetupPin.route -> {
                                SetupPinScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = NavigationTab.AppLockSettings.route }
                            }
                            NavigationTab.GiftCards.route -> {
                                GiftCardsScreen(
                                    viewModel = viewModel,
                                    filterAccountId = drillGiftCardAccountId,
                                    onBack = { 
                                        activeTabRoute = lastMainTabRoute
                                        drillGiftCardAccountId = null
                                    }
                                ) { card ->
                                    val brandName = card.displayBrandName().replace(" Gift Card", "")
                                    val targetAccount = accounts.find { 
                                        (it.type == "WALLET") && it.name.contains(brandName, ignoreCase = true) 
                                    }
                                    
                                    transactionToEdit = com.saik.expensemanager.data.model.Transaction(
                                        amount = card.amount,
                                        type = "TRANSFER",
                                        category = "",
                                        dateTime = System.currentTimeMillis(),
                                        linkedAccountId = card.linkedAccountId,
                                        toAccountId = targetAccount?.id,
                                        description = "${card.displayBrandName()} Gift Card Load",
                                        linkedBudgetBookId = selectedBudgetBookId,
                                        linkedGiftCardId = card.id,
                                    )
                                    showTransactionEntry = true
                                }
                            }
                            NavigationTab.ManageTemplates.route -> {
                                ManageTemplatesScreen(
                                    viewModel = viewModel
                                ) { activeTabRoute = lastMainTabRoute }
                            }
                        }
                    } else {
                        val isTransactionsTab = activeTabRoute == NavigationTab.Transactions.route
                        val isAccountDetail = ((activeTabRoute == NavigationTab.Accounts.route) && (drillAccountId != null))
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = !isTransactionsTab && !isAccountDetail,
                        ) { pageIndex ->
                            when (tabs[pageIndex].route) {
                                NavigationTab.Home.route -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateToAccounts = { cardId: Long?, scrollToCc: Boolean ->
                                            autoExpandAccountId = cardId
                                            shouldScrollToCreditCards = scrollToCc
                                            activeTabRoute = NavigationTab.Accounts.route
                                        },
                                        onNavigateToGiftCards = {
                                            drillGiftCardAccountId = null
                                            activeTabRoute = NavigationTab.GiftCards.route
                                        },
                                        onNavigateToStats = { type ->
                                            viewModel.setStatsType(type)
                                            viewModel.setStatsViewMode(1) // Monthly
                                            viewModel.resetStatsCalendar()
                                            viewModel.setStatsFilterDateRange(null)
                                            activeTabRoute = NavigationTab.Stats.route
                                        },
                                        onAddTransaction = { prefill ->
                                            transactionToEdit = prefill
                                            showTransactionEntry = true
                                        },
                                        onManageTemplates = {
                                            activeTabRoute = NavigationTab.ManageTemplates.route
                                        }
                                    ) { tx ->
                                        transactionToEdit = tx
                                        showTransactionEntry = true
                                    }
                                }
                                NavigationTab.Transactions.route -> {
                                    TransactionsScreen(
                                        viewModel = viewModel,
                                        onAddTransaction = { prefill ->
                                            transactionToEdit = prefill
                                            showTransactionEntry = true
                                        },
                                        onManageTemplates = {
                                            activeTabRoute = NavigationTab.ManageTemplates.route
                                        }
                                    ) { tx ->
                                        transactionToEdit = tx
                                        showTransactionEntry = true
                                    }
                                }
                                NavigationTab.Stats.route -> {
                                    StatsScreen(
                                        viewModel = viewModel
                                    ) { tx ->
                                        transactionToEdit = tx
                                        showTransactionEntry = true
                                    }
                                }
                                NavigationTab.Accounts.route -> {
                                    AccountsScreen(
                                        viewModel = viewModel,
                                        initialAccountIdToDetail = drillAccountId,
                                        autoExpandAccountId = autoExpandAccountId,
                                        onBackToAccountsList = { returningAccountId ->
                                            drillAccountId = null
                                            if (returningAccountId != null) {
                                                autoExpandAccountId = null
                                                autoExpandAccountId = returningAccountId
                                            }
                                        },
                                        onNavigateToGiftCards = { accountId ->
                                            drillGiftCardAccountId = accountId
                                            activeTabRoute = NavigationTab.GiftCards.route
                                        },
                                        scrollToCreditCards = shouldScrollToCreditCards,
                                        onScrolledToCreditCards = { 
                                            shouldScrollToCreditCards = false
                                            autoExpandAccountId = null
                                        },
                                        onAutoScrollCompleted = {
                                            autoExpandAccountId = null
                                        },
                                        onEditTransaction = { tx ->
                                            transactionToEdit = tx
                                            showTransactionEntry = true
                                        },
                                        onAddTransaction = { prefill ->
                                            transactionToEdit = prefill
                                            showTransactionEntry = true
                                        }
                                    ) { acc ->
                                        drillAccountId = acc.id
                                    }
                                }
                                NavigationTab.Settings.route -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateToAccountsManagement = { activeTabRoute = NavigationTab.AccountsManagement.route },
                                        onNavigateToCategoryManagement = { activeTabRoute = NavigationTab.CategoryManagement.route },
                                        onNavigateToRecurringManagement = { activeTabRoute = NavigationTab.RecurringTransactions.route },
                                        onNavigateToBudgetBooksManagement = { activeTabRoute = NavigationTab.BudgetBooksManagement.route },
                                        onNavigateToBackupRestore = { activeTabRoute = NavigationTab.BackupRestore.route },
                                        onNavigateToCloudBackup = { activeTabRoute = NavigationTab.CloudBackup.route },
                                        onNavigateToGiftCards = { 
                                            drillGiftCardAccountId = null
                                            activeTabRoute = NavigationTab.GiftCards.route 
                                        },
                                        onNavigateToManageTemplates = {
                                            activeTabRoute = NavigationTab.ManageTemplates.route
                                        },
                                    ) { activeTabRoute = NavigationTab.AppLockSettings.route }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showTransactionEntry,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                TransactionEntryScreen(
                    viewModel = viewModel,
                    initialTransaction = transactionToEdit,
                ) {
                    showTransactionEntry = false
                    transactionToEdit = null
                }
            }
        }
    }
}
