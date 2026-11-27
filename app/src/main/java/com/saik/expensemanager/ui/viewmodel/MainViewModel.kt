package com.saik.expensemanager.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saik.expensemanager.data.db.AppDatabase
import com.saik.expensemanager.data.model.*
import com.saik.expensemanager.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds
import com.saik.expensemanager.util.FuzzySearch
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.work.*
import androidx.work.workDataOf
import com.saik.expensemanager.data.backup.BackupWorker
import com.saik.expensemanager.data.backup.GoogleDriveService
import com.saik.expensemanager.data.model.CardSpendStatus
import com.saik.expensemanager.data.model.CardSpendStatusRaw
import java.util.concurrent.TimeUnit
import kotlin.math.*

enum class ViewMode { DAILY, WEEKLY, CALENDAR, MONTHLY, YEARLY, CYCLE }

data class CreditCardDetailedInsights(
    val currentCycleSpend: Double,
    val paidThisCycle: Double,
    val creditsAppliedThisCycle: Double,
    val totalReductionThisCycle: Double,
    val outstandingBalance: Double,
    val payableBalance: Double,
    val feeWaiverProgress: Double, // eligible spend
    val feeWaiverThreshold: Double,
    val isFeeThresholdReached: Boolean,
    val feeCycleDaysRemaining: Int,
    val lastPaymentDate: Long?,
    val cycleStartDate: Long,
    val cycleEndDate: Long,
    val isGracePeriod: Boolean = false
)

data class PaymentHistoryItem(
    val id: Long,
    val date: Long,
    val amount: Double,
    val type: String, // "PAYMENT" or "CREDIT"
    val category: String, // "Cashback", "Refund", or Source Account Name
    val note: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    // Settings
    val appSettings: StateFlow<AppSettings>
    
    // Core loaded streams
    val budgetBooks: StateFlow<List<BudgetBook>>
    val allTransactions: StateFlow<List<Transaction>>
    val allAccounts: StateFlow<List<Account>>
    val allCategories: StateFlow<List<Category>>
    val allCategoryAlerts: StateFlow<List<CategoryAlert>>
    val allRepetitiveTransactions: StateFlow<List<RepetitiveTransaction>>
    val allGiftCards: StateFlow<List<GiftCard>>
    val allTemplates: StateFlow<List<TransactionTemplate>>

    // Selection States for Filtering UI
    private val _selectedBudgetBookId = MutableStateFlow<Long>(-1L)
    val selectedBudgetBookId = _selectedBudgetBookId.asStateFlow()

    // Decoupled calendar states for specific screens
    private val _transactionsCalendar = MutableStateFlow<Calendar>(Calendar.getInstance())
    val transactionsCalendar = _transactionsCalendar.asStateFlow()

    private val _statsCalendar = MutableStateFlow<Calendar>(Calendar.getInstance())
    val statsCalendar = _statsCalendar.asStateFlow()

    private val _accountsCalendar = MutableStateFlow<Calendar>(Calendar.getInstance())
    val accountsCalendar = _accountsCalendar.asStateFlow()

    private val _filterDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val filterDateRange = _filterDateRange.asStateFlow()

    private val _filterAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val filterAccountIds = _filterAccountIds.asStateFlow()

    private val _filterCategoryNames = MutableStateFlow<Set<String>>(emptySet())
    val filterCategoryNames = _filterCategoryNames.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterTransactionTypes = MutableStateFlow<Set<String>>(emptySet())
    val filterTransactionTypes = _filterTransactionTypes.asStateFlow()

    private val _statsFilterDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val statsFilterDateRange = _statsFilterDateRange.asStateFlow()

    private val _statsFilterAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val statsFilterAccountIds = _statsFilterAccountIds.asStateFlow()

    private val _statsFilterCategoryNames = MutableStateFlow<Set<String>>(emptySet())
    val statsFilterCategoryNames = _statsFilterCategoryNames.asStateFlow()

    private val _statsFilterTransactionTypes = MutableStateFlow<Set<String>>(emptySet())
    val statsFilterTransactionTypes = _statsFilterTransactionTypes.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.DAILY)
    val viewMode = _viewMode.asStateFlow()

    private val _accountsViewMode = MutableStateFlow(ViewMode.MONTHLY)
    val accountsViewMode = _accountsViewMode.asStateFlow()

    private val _accountsCycleOffset = MutableStateFlow(0)
    val accountsCycleOffset = _accountsCycleOffset.asStateFlow()

    private val _collapsedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val collapsedAccountIds = _collapsedAccountIds.asStateFlow()

    private val _expandedAccountSections = MutableStateFlow<Set<String>>(emptySet())
    val expandedAccountSections = _expandedAccountSections.asStateFlow()

    private val _isThresholdAlertsExpanded = MutableStateFlow(false)
    val isThresholdAlertsExpanded = _isThresholdAlertsExpanded.asStateFlow()

    private val _isAccountsHomeExpanded = MutableStateFlow(false)
    val isAccountsHomeExpanded = _isAccountsHomeExpanded.asStateFlow()

    private val _statsViewMode = MutableStateFlow(0) // 0: Weekly, 1: Monthly, 2: Total
    val statsViewMode = _statsViewMode.asStateFlow()

    private val _statsType = MutableStateFlow("EXPENSE") // "EXPENSE" or "INCOME"
    val statsType = _statsType.asStateFlow()

    fun setStatsType(type: String) {
        _statsType.value = type
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val pendingCardAlerts: StateFlow<List<CardSpendStatus>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(
            budgetBookDao = db.budgetBookDao(),
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            categoryDao = db.categoryDao(),
            appSettingsDao = db.appSettingsDao(),
            categoryAlertDao = db.categoryAlertDao(),
            repetitiveTransactionDao = db.repetitiveTransactionDao(),
            giftCardDao = db.giftCardDao(),
            transactionTemplateDao = db.transactionTemplateDao()
        )

        appSettings = repository.appSettings
            .map { it ?: AppSettings() }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

        budgetBooks = repository.allBudgetBooks
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTransactions = repository.allTransactions
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allAccounts = repository.allAccounts
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allCategories = repository.allCategories
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allCategoryAlerts = repository.allCategoryAlerts
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allRepetitiveTransactions = repository.allRepetitiveTransactions
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allGiftCards = repository.allGiftCards
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTemplates = repository.allTemplates
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        pendingCardAlerts = repository.pendingCardAlertsRaw
            .map { list ->
                list.map { raw ->
                    val feeEndMs = (Calendar.getInstance().apply { timeInMillis = raw.feeCycleStartDate; add(Calendar.YEAR, 1) }).timeInMillis
                    val daysLeft = ((feeEndMs - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                    CardSpendStatus(
                        cardId = raw.cardId,
                        cardName = raw.cardName,
                        threshold = raw.threshold,
                        currentSpend = raw.currentSpend,
                        daysLeftInCycle = if (daysLeft >= 0) daysLeft else null
                    )
                }.sortedWith(compareBy<CardSpendStatus> { it.daysLeftInCycle ?: Int.MAX_VALUE }
                    .thenByDescending { it.threshold - it.currentSpend })
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Automatically select the first budget book once available (only once)
        viewModelScope.launch {
            budgetBooks.filter { it.isNotEmpty() }.first().let { list ->
                if (_selectedBudgetBookId.value == -1L) {
                    _selectedBudgetBookId.value = list.first().id
                }
            }
        }

        // Ensure default categories are setup on first run
        viewModelScope.launch {
            repository.populateDefaultCategoriesIfNeeded()
            
            // Clean up future recurring transactions and generate due ones in one pass
            repository.deleteFutureGeneratedTransactions(System.currentTimeMillis())

            generateRepetitiveTransactions()
            
            _isLoading.value = false
            
            ensureBackupScheduled(getApplication())
        }
    }

    // Navigation triggers
    fun selectBudgetBook(id: Long) {
        _selectedBudgetBookId.value = id
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setStatsViewMode(mode: Int) {
        _statsViewMode.value = mode
    }

    fun toggleAccountExpansion(accountId: Long) {
        val current = _collapsedAccountIds.value
        _collapsedAccountIds.value = if (current.contains(accountId)) current - accountId else current + accountId
    }

    fun toggleAccountSectionExpansion(sectionType: String) {
        val current = _expandedAccountSections.value
        _expandedAccountSections.value = if (current.contains(sectionType)) current - sectionType else current + sectionType
    }

    fun toggleThresholdAlertsExpansion() {
        _isThresholdAlertsExpanded.value = !_isThresholdAlertsExpanded.value
    }

    fun toggleAccountsHomeExpansion() {
        _isAccountsHomeExpanded.value = !_isAccountsHomeExpanded.value
    }

    fun updateHomeAccountCategories(categories: Set<String>) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            repository.updateAppSettings(current.copy(homeAccountCategories = categories.joinToString(",")))
        }
    }

    // SCREEN-SPECIFIC CALENDAR ACTIONS
    
    // Transactions
    fun setTransactionsMonthYear(month: Int, year: Int) {
        val current = _transactionsCalendar.value.clone() as Calendar
        current.set(Calendar.MONTH, month)
        current.set(Calendar.YEAR, year)
        _transactionsCalendar.value = current
    }

    fun selectTransactionsOffset(offset: Int) {
        val current = _transactionsCalendar.value.clone() as Calendar
        current.add(Calendar.MONTH, offset)
        _transactionsCalendar.value = current
    }

    fun selectTransactionsOffsetByPeriod(period: String, offset: Int) {
        val current = _transactionsCalendar.value.clone() as Calendar
        when (period.uppercase()) {
            "DAILY", "CALENDAR" -> current.add(Calendar.MONTH, offset)
            "WEEKLY" -> current.add(Calendar.WEEK_OF_YEAR, offset)
            "MONTHLY", "YEARLY" -> current.add(Calendar.YEAR, offset)
            else -> current.add(Calendar.MONTH, offset)
        }
        _transactionsCalendar.value = current
    }

    fun resetTransactionsCalendar() {
        _transactionsCalendar.value = Calendar.getInstance()
    }

    // Stats
    fun resetStatsCalendar() {
        _statsCalendar.value = Calendar.getInstance()
    }

    fun selectStatsOffset(mode: Int, offset: Int) {
        val current = _statsCalendar.value.clone() as Calendar
        when (mode) {
            0 -> current.add(Calendar.WEEK_OF_YEAR, offset)
            1 -> current.add(Calendar.MONTH, offset)
            2 -> current.add(Calendar.YEAR, offset)
            else -> {} // Total mode does not offset
        }
        _statsCalendar.value = current
    }

    // Accounts
    fun selectAccountsOffsetByPeriod(period: String, offset: Int) {
        val current = _accountsCalendar.value.clone() as Calendar
        when (period.uppercase()) {
            "WEEKLY" -> current.add(Calendar.WEEK_OF_YEAR, offset)
            "YEARLY" -> current.add(Calendar.YEAR, offset)
            else -> current.add(Calendar.MONTH, offset)
        }
        _accountsCalendar.value = current
    }

    // CRUD Actions
    fun addBudgetBook(name: String, budget: Double?, currency: String) {
        viewModelScope.launch {
            val isFirst = budgetBooks.value.isEmpty()
            val id = repository.insertBudgetBook(BudgetBook(name = name, monthlyLimit = budget, currencyCode = currency, isDefault = isFirst))
            if (_selectedBudgetBookId.value == -1L) {
                _selectedBudgetBookId.value = id
            }
        }
    }

    fun updateBudgetBook(id: Long, name: String, budget: Double?, currency: String, active: Boolean, startDate: Long? = null, endDate: Long? = null) {
        viewModelScope.launch {
            val existing = budgetBooks.value.find { it.id == id } ?: return@launch
            repository.updateBudgetBook(existing.copy(name = name, monthlyLimit = budget, currencyCode = currency, isActive = active, startDate = startDate, endDate = endDate))
        }
    }

    fun setDefaultBook(bookId: Long, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val book = budgetBooks.value.find { it.id == bookId }
            if (book?.isClosed == true) {
                onError("A closed book cannot be set as default.")
                return@launch
            }
            repository.setDefaultBudgetBook(bookId)
        }
    }

    fun toggleBookClosed(bookId: Long, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val book = budgetBooks.value.find { it.id == bookId } ?: return@launch
            if (!book.isClosed && book.isDefault) {
                onError("Default book cannot be closed. Set another book as default first.")
                return@launch
            }
            repository.updateBudgetBook(book.copy(isClosed = !book.isClosed))
        }
    }

    fun updateBookDates(bookId: Long, startDate: Long?, endDate: Long?, onError: (String) -> Unit = {}) {
        if (startDate != null && endDate != null && endDate < startDate) {
            onError("End date must be after start date.")
            return
        }
        viewModelScope.launch {
            val book = budgetBooks.value.find { it.id == bookId } ?: return@launch
            repository.updateBudgetBook(book.copy(startDate = startDate, endDate = endDate))
        }
    }

    fun deleteBudgetBook(budgetBook: BudgetBook, onError: (String) -> Unit = {}) {
        if (budgetBook.isDefault) {
            onError("Set another book as default before deleting this one.")
            return
        }
        viewModelScope.launch {
            repository.deleteBudgetBook(budgetBook)
            // If the deleted book was selected, reset selection
            if (_selectedBudgetBookId.value == budgetBook.id) {
                val books = repository.allBudgetBooks.first()
                _selectedBudgetBookId.value = books.find { !it.isClosed }?.id ?: books.firstOrNull()?.id ?: -1L
            }
        }
    }

    fun addAccount(
        name: String, 
        type: String, 
        linkedBookId: Long, 
        isCc: Boolean = false, 
        cycleStart: Int = 1, 
        cycleEnd: Int = 28, 
        iconName: String = "AccountBalanceWallet", 
        initialBalance: Double = 0.0,
        trackFeeWaiver: Boolean = false,
        annualFeeAmount: Double? = null,
        feeWaiverThreshold: Double? = null,
        feeCycleStartDate: Long? = null,
        feeCycleType: String? = null,
        feeThresholdEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertAccount(
                Account(
                    name = name,
                    type = type,
                    associatedBudgetBookId = linkedBookId,
                    isCreditCard = isCc || type == "CREDIT_CARD",
                    billingCycleStartDay = cycleStart,
                    billingCycleEndDay = cycleEnd,
                    initialBalance = initialBalance,
                    currentBalance = initialBalance,
                    isActive = true,
                    iconName = iconName,
                    trackFeeWaiver = trackFeeWaiver,
                    annualFeeAmount = annualFeeAmount,
                    feeWaiverThreshold = feeWaiverThreshold,
                    feeCycleStartDate = feeCycleStartDate,
                    feeCycleType = feeCycleType,
                    feeThresholdEnabled = feeThresholdEnabled
                )
            )
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun updateAccountOrder(orderedAccounts: List<Account>) {
        viewModelScope.launch {
            repository.updateAccounts(orderedAccounts.mapIndexed { index, account -> 
                account.copy(displayOrder = index) 
            })
        }
    }

    fun updateAccountTypeOrder(order: List<String>) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            repository.updateAppSettings(current.copy(accountTypeOrder = order.joinToString(",")))
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addGiftCard(brand: String, customBrandName: String?, code: String, amount: Double, purchaseDate: Long, validityDays: Int, note: String?) {
        viewModelScope.launch {
            repository.addGiftCard(brand, customBrandName, code, amount, purchaseDate, validityDays, note, _selectedBudgetBookId.value)
        }
    }

    fun markGiftCardUsed(giftCardId: Long) {
        viewModelScope.launch {
            repository.markGiftCardUsed(giftCardId)
        }
    }

    fun markGiftCardUnused(giftCardId: Long) {
        viewModelScope.launch {
            repository.markGiftCardUnused(giftCardId)
        }
    }

    fun updateGiftCard(card: GiftCard) {
        viewModelScope.launch {
            repository.updateGiftCard(card)
        }
    }

    fun deleteGiftCard(card: GiftCard) {
        viewModelScope.launch {
            repository.deleteGiftCard(card)
        }
    }

    // Template Actions
    fun addTemplate(
        name: String,
        amount: Double,
        type: String,
        category: String,
        subCategory: String? = null,
        accountId: Long,
        toAccountId: Long? = null,
        budgetBookId: Long,
        description: String = "",
        iconName: String = "Star",
        emojiValue: String? = null,
        colorHex: String? = null
    ) {
        viewModelScope.launch {
            val templates = allTemplates.value
            val nextOrder = (templates.maxByOrNull { it.displayOrder }?.displayOrder ?: -1) + 1
            repository.insertTemplate(
                TransactionTemplate(
                    name = name,
                    amount = amount,
                    type = type,
                    category = category,
                    subCategory = subCategory,
                    linkedAccountId = accountId,
                    toAccountId = toAccountId,
                    linkedBudgetBookId = budgetBookId,
                    description = description,
                    iconName = iconName,
                    emojiValue = emojiValue,
                    colorHex = colorHex,
                    displayOrder = nextOrder
                )
            )
        }
    }

    fun updateTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            repository.updateTemplate(template)
        }
    }

    fun deleteTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun reorderTemplates(ordered: List<TransactionTemplate>) {
        viewModelScope.launch {
            repository.updateTemplates(ordered.mapIndexed { index, template ->
                template.copy(displayOrder = index)
            })
        }
    }

    fun getPrefillFromTemplate(template: TransactionTemplate): Transaction {
        return Transaction(
            id = 0,
            amount = template.amount,
            type = template.type,
            category = template.category,
            subCategory = template.subCategory,
            dateTime = System.currentTimeMillis(),
            linkedAccountId = template.linkedAccountId,
            toAccountId = template.toAccountId,
            linkedBudgetBookId = template.linkedBudgetBookId,
            description = template.description
        )
    }

    fun markGiftCardFullyUsed(accountId: Long, currentBalance: Double) {
        // Keeping this for backward compatibility if any UI still calls it, 
        // but it's logically deprecated for the pooled model.
    }

    fun correctAccountBalance(account: Account, targetBalance: Double) {
        viewModelScope.launch {
            val currentCalculatedBalance = getAccountBalanceFlow(account.id).first()
            val rawDiff = targetBalance - currentCalculatedBalance
            // Round to 2 decimal places to avoid floating point precision issues
            val diff = (rawDiff * 100.0).roundToLong() / 100.0
            
            if (diff != 0.0) {
                val defaultBookId = budgetBooks.value.find { it.isDefault }?.id ?: -1L
                
                // Log Balance Correction as a transaction
                repository.insertTransaction(
                    Transaction(
                        amount = kotlin.math.abs(diff),
                        type = if (diff > 0) "INCOME" else "EXPENSE",
                        category = "Adjustment",
                        description = "Balance Correction",
                        dateTime = System.currentTimeMillis(),
                        linkedAccountId = account.id,
                        linkedBudgetBookId = defaultBookId
                    )
                )

                // Update current balance in account record (initialBalance remains unchanged)
                repository.updateAccount(
                    account.copy(
                        currentBalance = targetBalance
                    )
                )
            }
        }
    }

    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        dateTimeMs: Long,
        accountId: Long,
        budgetBookId: Long,
        description: String,
        toAccountId: Long? = null,
        subCategory: String? = null,
        linkedGiftCardId: Long? = null,
        isCreditCardPayment: Boolean = false
    ) {
        viewModelScope.launch {
            val finalBudgetBookId = if (type == "TRANSFER") {
                budgetBooks.value.find { it.isDefault }?.id ?: budgetBookId
            } else {
                budgetBookId
            }
            repository.insertTransaction(
                Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    subCategory = subCategory,
                    dateTime = dateTimeMs,
                    linkedAccountId = accountId,
                    linkedBudgetBookId = finalBudgetBookId,
                    description = description,
                    toAccountId = toAccountId,
                    linkedGiftCardId = linkedGiftCardId,
                    isCreditCardPayment = isCreditCardPayment
                )
            )
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun addTransferWithGiftCards(
        amount: Double,
        dateTimeMs: Long,
        fromAccountId: Long,
        toAccountId: Long,
        budgetBookId: Long,
        description: String,
        giftCardItems: List<com.saik.expensemanager.data.model.GiftCardData>
    ) {
        viewModelScope.launch {
            val finalBudgetBookId = budgetBooks.value.find { it.isDefault }?.id ?: budgetBookId
            repository.addTransferWithGiftCards(
                amount = amount,
                dateTimeMs = dateTimeMs,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                budgetBookId = finalBudgetBookId,
                description = description,
                giftCardItems = giftCardItems
            )
        }
    }

    fun updateTransferWithGiftCards(
        transaction: Transaction,
        giftCardItems: List<com.saik.expensemanager.data.model.GiftCardData>
    ) {
        viewModelScope.launch {
            repository.updateTransferWithGiftCards(transaction, giftCardItems)
        }
    }

    suspend fun getGiftCardsForTransaction(txId: Long): List<GiftCard> {
        return repository.getGiftCardsForTransaction(txId)
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addCategory(name: String, type: String, color: Int, parentId: Long? = null, iconName: String = "Label", onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertCategory(Category(name = name, type = type, color = color, parentId = parentId, iconName = iconName))
            onCreated(id)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun setCategoryAlert(categoryName: String, amount: Double, enabled: Boolean) {
        viewModelScope.launch {
            repository.insertCategoryAlert(CategoryAlert(categoryName = categoryName, limitAmount = amount, isEnabled = enabled))
        }
    }

    fun deleteCategoryAlert(alert: CategoryAlert) {
        viewModelScope.launch {
            repository.deleteCategoryAlert(alert)
        }
    }

    fun generateRepetitiveTransactions(untilDateMs: Long? = null, fromDateMs: Long? = null) {
        viewModelScope.launch(Dispatchers.Default) {
            val reps = repository.getAllRepetitiveTransactions()
            if (reps.isEmpty()) return@launch

            val generatedKeys = repository.getGeneratedTransactionKeys()
                .map { Pair(it.repetitiveId, it.repetitiveOccurrenceDate) }
                .toSet()

            val limit = untilDateMs ?: Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // Use today's start-of-day as the minimum generation date by default.
            // This prevents regenerating manually-deleted past occurrences during app init
            // or when updating a recurring template.
            val todayStartMs = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val minDateMs = fromDateMs ?: todayStartMs

            val newTransactions = mutableListOf<Transaction>()

            for (rep in reps) {
                val startCal = Calendar.getInstance().apply { timeInMillis = rep.startDate }
                val occurrenceCal = startCal.clone() as Calendar
                
                if (rep.frequency == "WEEKLY" && rep.dayOfWeek != null) {
                    while (occurrenceCal.get(Calendar.DAY_OF_WEEK) != rep.dayOfWeek) {
                        occurrenceCal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                
                if (rep.frequency == "MONTHLY" && rep.dayOfMonth != null) {
                    occurrenceCal.set(Calendar.DAY_OF_MONTH, minOf(rep.dayOfMonth, occurrenceCal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }

                var safetyCounter = 0
                var countGeneratedForThisRep = 0
                
                while (occurrenceCal.timeInMillis <= limit && safetyCounter < 5000) {
                    safetyCounter++
                    val occurrenceMs = occurrenceCal.timeInMillis
                    
                    if (rep.endDate != null && occurrenceMs > rep.endDate) break
                    if (rep.occurrences != null && countGeneratedForThisRep >= rep.occurrences) break

                    val key = Pair(rep.id, occurrenceMs)
                    // Skip occurrences before the minimum date (e.g., don't regenerate
                    // manually deleted past transactions when updating a recurring template).
                    if (occurrenceMs < minDateMs) {
                        countGeneratedForThisRep++
                        when (rep.frequency.uppercase()) {
                            "DAILY", "DAY" -> occurrenceCal.add(Calendar.DAY_OF_YEAR, rep.interval)
                            "WEEKLY", "WEEK" -> occurrenceCal.add(Calendar.WEEK_OF_YEAR, rep.interval)
                            "MONTHLY", "MONTH" -> {
                                occurrenceCal.add(Calendar.MONTH, rep.interval)
                                if (rep.dayOfMonth != null) {
                                    occurrenceCal.set(Calendar.DAY_OF_MONTH, minOf(rep.dayOfMonth, occurrenceCal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                                }
                            }
                            "YEARLY", "YEAR" -> occurrenceCal.add(Calendar.YEAR, rep.interval)
                            else -> break
                        }
                        continue
                    }

                    if (!generatedKeys.contains(key)) {
                        val finalDateTime = if (rep.reflectionTiming == "FIRST_OF_MONTH") {
                            Calendar.getInstance().apply {
                                timeInMillis = occurrenceMs
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        } else {
                            occurrenceMs
                        }

                        newTransactions.add(
                            Transaction(
                                dateTime = finalDateTime,
                                amount = rep.amount,
                                type = rep.type,
                                category = rep.category,
                                subCategory = rep.subCategory,
                                description = rep.description,
                                linkedAccountId = rep.linkedAccountId,
                                toAccountId = rep.toAccountId,
                                linkedBudgetBookId = rep.linkedBudgetBookId,
                                repetitiveId = rep.id,
                                repetitiveOccurrenceDate = occurrenceMs,
                                isGeneratedFromRecurrence = true,
                                linkedGiftCardId = rep.linkedGiftCardId,
                                isCreditCardPayment = rep.isCreditCardPayment
                            )
                        )
                    }
                    
                    countGeneratedForThisRep++
                    
                    when (rep.frequency.uppercase()) {
                        "DAILY", "DAY" -> occurrenceCal.add(Calendar.DAY_OF_YEAR, rep.interval)
                        "WEEKLY", "WEEK" -> occurrenceCal.add(Calendar.WEEK_OF_YEAR, rep.interval)
                        "MONTHLY", "MONTH" -> {
                            occurrenceCal.add(Calendar.MONTH, rep.interval)
                            if (rep.dayOfMonth != null) {
                                occurrenceCal.set(Calendar.DAY_OF_MONTH, minOf(rep.dayOfMonth, occurrenceCal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                            }
                        }
                        "YEARLY", "YEAR" -> occurrenceCal.add(Calendar.YEAR, rep.interval)
                        else -> break
                    }
                }
            }
            
            if (newTransactions.isNotEmpty()) {
                repository.insertTransactions(newTransactions)
            }
        }
    }

    fun triggerGenerationIfNeeded() {
        generateRepetitiveTransactions()
    }

    fun addRepetitiveTransaction(
        startDateMs: Long,
        amount: Double,
        type: String,
        category: String,
        frequency: String,
        interval: Int,
        accountId: Long,
        budgetBookId: Long,
        description: String,
        endDate: Long? = null,
        occurrences: Int? = null,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        toAccountId: Long? = null,
        reflectionTiming: String = "ON_DATE",
        linkTransactionId: Long? = null,
        subCategory: String? = null,
        linkedGiftCardId: Long? = null,
        isCreditCardPayment: Boolean = false
    ) {
        viewModelScope.launch {
            val finalBudgetBookId = if (type == "TRANSFER") {
                budgetBooks.value.find { it.isDefault }?.id ?: budgetBookId
            } else {
                budgetBookId
            }
            val repId = repository.insertRepetitiveTransaction(
                RepetitiveTransaction(
                    startDate = startDateMs,
                    amount = amount,
                    type = type,
                    category = category,
                    subCategory = subCategory,
                    frequency = frequency,
                    interval = interval,
                    linkedAccountId = accountId,
                    toAccountId = toAccountId,
                    linkedBudgetBookId = finalBudgetBookId,
                    description = description,
                    endDate = endDate,
                    occurrences = occurrences,
                    dayOfWeek = dayOfWeek,
                    dayOfMonth = dayOfMonth,
                    reflectionTiming = reflectionTiming,
                    linkedGiftCardId = linkedGiftCardId,
                    isCreditCardPayment = isCreditCardPayment
                )
            )
            
            if (linkTransactionId != null) {
                val allTxs = repository.allTransactions.firstOrNull() ?: emptyList()
                allTxs.find { it.id == linkTransactionId }?.let { tx ->
                    repository.updateTransaction(tx.copy(
                        amount = amount,
                        type = type,
                        category = category,
                        subCategory = subCategory,
                        dateTime = startDateMs,
                        linkedAccountId = accountId,
                        toAccountId = toAccountId,
                        description = description,
                        repetitiveId = repId,
                        repetitiveOccurrenceDate = startDateMs,
                        isGeneratedFromRecurrence = true
                    ))
                }
            }

            // Immediately trigger generation for newly added template, including past occurrences if any
            generateRepetitiveTransactions(fromDateMs = 0L)
            
            _isLoading.value = false
        }
    }

    fun updateRepetitiveTransaction(rep: RepetitiveTransaction) {
        viewModelScope.launch {
            repository.updateRepetitiveTransaction(rep)
            // Delete all future generated transactions for THIS recurring transaction 
            // from today onward, so they get regenerated with the updated template values.
            // Past transactions remain untouched and manually deleted past ones won't be recreated.
            val todayStartOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            repository.deleteFutureGeneratedTransactionsForRep(rep.id, todayStartOfDay)
            generateRepetitiveTransactions(fromDateMs = todayStartOfDay)
            _isLoading.value = false
        }
    }

    fun deleteRepetitiveTransaction(rep: RepetitiveTransaction) {
        viewModelScope.launch {
            repository.deleteRepetitiveTransaction(rep)
        }
    }

    fun setFilterDateRange(range: Pair<Long, Long>?) { _filterDateRange.value = range }
    fun setFilterAccounts(ids: Set<Long>) { _filterAccountIds.value = ids }
    fun setFilterCategories(names: Set<String>) { _filterCategoryNames.value = names }
    fun setFilterTransactionTypes(types: Set<String>) { _filterTransactionTypes.value = types }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setStatsFilterDateRange(range: Pair<Long, Long>?) { _statsFilterDateRange.value = range }
    fun setStatsFilterAccounts(ids: Set<Long>) { _statsFilterAccountIds.value = ids }
    fun setStatsFilterCategories(names: Set<String>) { _statsFilterCategoryNames.value = names }
    fun setStatsFilterTransactionTypes(types: Set<String>) { _statsFilterTransactionTypes.value = types }

    fun clearFilters() {
        _filterDateRange.value = null
        _filterAccountIds.value = emptySet()
        _filterCategoryNames.value = emptySet()
        _filterTransactionTypes.value = emptySet()
        _searchQuery.value = ""
    }

    fun clearStatsFilters() {
        _statsFilterDateRange.value = null
        _statsFilterAccountIds.value = emptySet()
        _statsFilterCategoryNames.value = emptySet()
        _statsFilterTransactionTypes.value = emptySet()
    }

    fun completeOnboarding(name: String, budget: Double?, currency: String) {
        viewModelScope.launch {
            repository.seedDemoDataForOnboarding(name, budget ?: 0.0, currency)
        }
    }

    fun setAppPin(pin: String) = viewModelScope.launch { repository.setAppPin(pin) }
    fun setAppLockEnabled(enabled: Boolean) = viewModelScope.launch { repository.setAppLockEnabled(enabled) }
    fun setBiometricLockEnabled(enabled: Boolean) = viewModelScope.launch { repository.setBiometricLockEnabled(enabled) }
    fun clearAppPin() = viewModelScope.launch { repository.clearAppPin() }

    // Compatibility aliases
    val selectedCalendar: StateFlow<Calendar> = transactionsCalendar
    fun selectMonthOffset(offset: Int) = selectTransactionsOffset(offset)
    fun setMonthYear(month: Int, year: Int) = setTransactionsMonthYear(month, year)
    fun selectOffsetByPeriod(period: String, offset: Int) = selectTransactionsOffsetByPeriod(period, offset)

    fun setAccountsViewMode(mode: ViewMode, account: Account? = null) {
        _accountsViewMode.value = mode
        val now = Calendar.getInstance()
        
        if (mode == ViewMode.DAILY && account?.isCreditCard == true) {
            // Find current active cycle start
            val today = now.get(Calendar.DAY_OF_MONTH)
            val start = (now.clone() as Calendar).apply {
                if (today < account.billingCycleStartDay) {
                    add(Calendar.MONTH, -1)
                }
                set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            _accountsCalendar.value = start
        } else {
            // Reset to 1st of month for consistency in non-CC views
            val start = (now.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            _accountsCalendar.value = start
        }
        _accountsCycleOffset.value = 0
    }

    fun selectAccountsOffset(offset: Int, account: Account? = null) {
        val current = _accountsCalendar.value.clone() as Calendar
        when (_accountsViewMode.value) {
            ViewMode.DAILY -> {
                // For CC, we move by cycles, for others by months
                current.add(Calendar.MONTH, offset)
            }
            ViewMode.MONTHLY, ViewMode.YEARLY -> current.add(Calendar.YEAR, offset)
            else -> current.add(Calendar.MONTH, offset)
        }
        _accountsCalendar.value = current
    }

    fun computeAccountSummary(accountId: Long, account: Account? = null): Flow<AccountSummary> = 
        combine(allTransactions, accountsCalendar, accountsViewMode) { txs, cal, mode ->
            
            val accountTxs = txs.filter { it.linkedAccountId == accountId || it.toAccountId == accountId }
            
            val (startMs, endMs) = when (mode) {
                ViewMode.DAILY -> {
                    if (account?.isCreditCard == true) {
                        // cal IS the cycle start date in our new CC detail logic
                        val start = (cal.clone() as Calendar).apply { 
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
                        }
                        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }
                        start.timeInMillis to end.timeInMillis
                    } else {
                        val start = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }
                        start.timeInMillis to end.timeInMillis
                    }
                }
                ViewMode.MONTHLY, ViewMode.YEARLY -> {
                    if (account?.isCreditCard == true) {
                        val firstMonth = (cal.clone() as Calendar).apply { set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1) }
                        val start = getCycleStartForCalendar(account, firstMonth)
                        val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1); add(Calendar.MILLISECOND, -1) }
                        start.timeInMillis to end.timeInMillis
                    } else {
                        val start = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1); add(Calendar.MILLISECOND, -1) }
                        start.timeInMillis to end.timeInMillis
                    }
                }
                else -> 0L to 0L
            }

            val filteredTxs = accountTxs.filter { it.dateTime in startMs..endMs }
            
            val income = filteredTxs.filter { (it.linkedAccountId == accountId && it.type == "INCOME") || (it.toAccountId == accountId && it.type == "TRANSFER") }.sumOf { it.amount }
            val expense = filteredTxs.filter { it.linkedAccountId == accountId && (it.type == "EXPENSE" || (it.type == "TRANSFER" && it.toAccountId != null)) }.sumOf { it.amount }
            
            AccountSummary(income, expense, income - expense, filteredTxs.sortedByDescending { it.dateTime })
        }

    fun computeAccountGroupedSummary(accountId: Long, account: Account? = null): Flow<List<GroupedSummary>> =
        combine(allTransactions, accountsCalendar, accountsViewMode) { txs, cal, mode ->
            val accountTxs = txs.filter { it.linkedAccountId == accountId || it.toAccountId == accountId }
            val result = mutableListOf<GroupedSummary>()

            when (mode) {
                ViewMode.MONTHLY -> {
                    val year = cal.get(Calendar.YEAR)
                    val now = Calendar.getInstance()
                    
                    // Determine which month to start from based on current active cycle
                    val startMonthIndex = if (year == now.get(Calendar.YEAR)) {
                        val today = now.get(Calendar.DAY_OF_MONTH)
                        if (account?.isCreditCard == true && today < account.billingCycleStartDay) {
                             now.get(Calendar.MONTH) - 1
                        } else {
                             now.get(Calendar.MONTH)
                        }
                    } else 11
                    
                    for (mIdx in startMonthIndex downTo 0) {
                        val mStart = Calendar.getInstance().apply { 
                            set(year, mIdx, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) 
                        }
                        
                        val (start, end) = if (account?.isCreditCard == true) {
                            // Find the cycle starting in this month
                            val cycleStart = getCycleStartForCalendar(account, mStart)
                            val cycleEnd = (cycleStart.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.DAY_OF_YEAR, -1) }
                            cycleStart to cycleEnd
                        } else {
                            val mEnd = (mStart.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.DAY_OF_YEAR, -1) }
                            mStart to mEnd
                        }

                        val filteredTxs = accountTxs.filter { it.dateTime in start.timeInMillis..end.timeInMillis }
                        if (filteredTxs.isNotEmpty()) {
                            val inc = filteredTxs.filter { (it.linkedAccountId == accountId && it.type == "INCOME") || (it.toAccountId == accountId && it.type == "TRANSFER") }.sumOf { it.amount }
                            val exp = filteredTxs.filter { it.linkedAccountId == accountId && (it.type == "EXPENSE" || (it.type == "TRANSFER" && it.toAccountId != null)) }.sumOf { it.amount }
                            val label = SimpleDateFormat("MMM", Locale.US).format(start.time)
                            val range = "${SimpleDateFormat("MM.dd", Locale.US).format(start.time)} - ${SimpleDateFormat("MM.dd", Locale.US).format(end.time)}"
                            result.add(GroupedSummary(label, inc, exp, inc - exp, range))
                        }
                    }
                }
                ViewMode.YEARLY -> {
                    // Annual summary is displayed in the header, no list items needed
                }
                else -> {}
            }
            result
        }

    private fun getCycleStartForCalendar(account: Account, cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }

    private fun getCycleStart(account: Account, offset: Int): Calendar {
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_MONTH)
        val cycleStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (today >= account.billingCycleStartDay) {
                set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay)
            } else {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay)
            }
        }
        cycleStart.add(Calendar.MONTH, offset)
        return cycleStart
    }

    fun computeCreditCardCycleSummary(account: Account): Flow<CreditCardBillingSummary> = 
        combine(allTransactions, accountsCycleOffset) { txs, offset ->
            val cardTxs = txs.filter { it.linkedAccountId == account.id || it.toAccountId == account.id }
            
            val currentCycleStart = getCycleStart(account, offset)
            val cycleStartMs = currentCycleStart.timeInMillis
            
            fun calculateSignedBalance(list: List<Transaction>): Double {
                return list.sumOf { tx ->
                    if (tx.linkedAccountId == account.id) {
                        when (tx.type) {
                            "EXPENSE", "TRANSFER" -> tx.amount
                            "INCOME" -> -tx.amount
                            else -> 0.0
                        }
                    } else if (tx.toAccountId == account.id && tx.type == "TRANSFER") {
                        -tx.amount
                    } else 0.0
                }
            }

            val initialDebt = -account.initialBalance
            val carryForwardSignedBalance = initialDebt + calculateSignedBalance(cardTxs.filter { it.dateTime < cycleStartMs })
            val currentCycleEndMs = (currentCycleStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
            val currentCycleTxs = cardTxs.filter { it.dateTime >= cycleStartMs && it.dateTime < currentCycleEndMs }
            
            val cardTotalSignedBalance = initialDebt + calculateSignedBalance(cardTxs)

            val currentCycleReductions = currentCycleTxs.sumOf { tx ->
                if (tx.toAccountId == account.id && tx.type == "TRANSFER" && tx.isCreditCardPayment) tx.amount
                else if (tx.linkedAccountId == account.id && tx.type == "INCOME" && (tx.category == "Cashback" || tx.category == "Refund" || tx.category == "Refunds")) tx.amount
                else 0.0
            }

            val payableVal = maxOf(0.0, carryForwardSignedBalance - currentCycleReductions)
            val outstandingVal = cardTotalSignedBalance

            val df = SimpleDateFormat("dd.MM.yy", Locale.US)
            val cycleEnd = (currentCycleStart.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }
            val cycleDateRange = "${df.format(currentCycleStart.time)} ~ ${df.format(cycleEnd.time)}"

            CreditCardBillingSummary(
                outstandingBalance = outstandingVal,
                payableBalance = payableVal,
                isGracePeriod = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) > account.billingCycleEndDay,
                dueDateString = cycleDateRange,
                outstandingTransactions = currentCycleTxs,
                payableTransactions = emptyList()
            )
        }

    fun updateSettings(theme: String, colorTheme: String, language: String, threshold: Double, skipExit: Boolean) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            repository.updateAppSettings(
                current.copy(
                    theme = theme,
                    colorTheme = colorTheme,
                    language = language,
                    budgetAlertThreshold = threshold,
                    skipExitConfirmation = skipExit
                )
            )
        }
    }

    fun updateLogTemplatesInstantly(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            repository.updateAppSettings(current.copy(logTemplatesInstantly = enabled))
        }
    }

    fun logTransactionFromTemplate(template: TransactionTemplate) {
        viewModelScope.launch {
            val transaction = getPrefillFromTemplate(template)
            repository.insertTransaction(transaction)
        }
    }

    fun updateBackupFolder(uri: String?) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            repository.updateAppSettings(current.copy(backupFolderUri = uri))
            scheduleBackup(getApplication(), useUpdatePolicy = true)
        }
    }

    fun updateLocalBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            repository.updateAppSettings(current.copy(localBackupEnabled = enabled))
            scheduleBackup(getApplication(), useUpdatePolicy = true)
        }
    }

    fun updateAutoBackupFrequency(days: Int) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            repository.updateAppSettings(current.copy(autoBackupFrequencyDays = days))
            scheduleBackup(getApplication(), useUpdatePolicy = true)
        }
    }

    fun updateCloudBackupSettings(
        email: String? = null,
        enabled: Boolean? = null,
        frequencyDays: Int? = null,
        clearEmail: Boolean = false,
        onMetered: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = repository.getAppSettings() ?: AppSettings()
            val newSettings = current.copy(
                googleAccountEmail = if (clearEmail) null else (email ?: current.googleAccountEmail),
                cloudBackupEnabled = enabled ?: current.cloudBackupEnabled,
                autoBackupFrequencyDays = frequencyDays ?: current.autoBackupFrequencyDays,
                cloudBackupOnMetered = onMetered ?: current.cloudBackupOnMetered
            )
            repository.updateAppSettings(newSettings)
            scheduleBackup(getApplication(), useUpdatePolicy = true)
        }
    }

    fun scheduleBackup(context: Context, useUpdatePolicy: Boolean = false) {
        val policy = if (useUpdatePolicy) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
        viewModelScope.launch {
            val settings = repository.getAppSettings() ?: return@launch
            val workManager = WorkManager.getInstance(context)
            val freqDays = settings.autoBackupFrequencyDays.toLong()
            
            // Flex interval: a reasonable window so WorkManager can pick an optimal time
            // without drifting too far from the intended schedule
            val flexMinutes = when {
                freqDays <= 1 -> 240L // Daily: 4 hour flex (increased from 30 min)
                freqDays <= 7 -> 480L // Weekly: 8 hour flex
                else -> 720L          // Monthly: 12 hour flex
            }

            // 1. Local Backup Work (No network constraint)
            if (settings.localBackupEnabled && settings.autoBackupFrequencyDays > 0 && settings.backupFolderUri != null) {
                val localRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                    freqDays, TimeUnit.DAYS,
                    flexMinutes, TimeUnit.MINUTES
                )
                    .setInputData(androidx.work.workDataOf("backup_type" to "local"))
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        1, TimeUnit.MINUTES
                    )
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    "local_backup_work",
                    policy,
                    localRequest
                )
            } else {
                cancelLocalBackup(context)
            }

            // 2. Cloud Backup Work (Network constraint)
            if (settings.cloudBackupEnabled && settings.autoBackupFrequencyDays > 0 && settings.googleAccountEmail != null) {
                val networkType = if (settings.cloudBackupOnMetered) NetworkType.CONNECTED else NetworkType.UNMETERED
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .setRequiresBatteryNotLow(true)
                    .build()
                val cloudRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                    freqDays, TimeUnit.DAYS,
                    flexMinutes, TimeUnit.MINUTES
                )
                    .setInputData(androidx.work.workDataOf("backup_type" to "cloud"))
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        1, TimeUnit.MINUTES
                    )
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    "cloud_backup_work",
                    policy,
                    cloudRequest
                )
            } else {
                cancelCloudBackup(context)
            }
        }
    }

    fun ensureBackupScheduled(context: Context) {
        viewModelScope.launch {
            val settings = repository.getAppSettings() ?: return@launch
            val now = System.currentTimeMillis()
            val freqMs = settings.autoBackupFrequencyDays.toLong() * 24 * 60 * 60 * 1000
            
            val workManager = WorkManager.getInstance(context)

            // 1. Kick off immediate one-time backup if due (app start is a great time)
            if (settings.localBackupEnabled && settings.backupFolderUri != null && (now - settings.lastLocalBackupTime >= freqMs)) {
                val immediateLocal = OneTimeWorkRequestBuilder<BackupWorker>()
                    .setInputData(workDataOf("backup_type" to "local"))
                    .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                    .build()
                workManager.enqueueUniqueWork("local_backup_immediate", ExistingWorkPolicy.REPLACE, immediateLocal)
            }
            
            if (settings.cloudBackupEnabled && settings.googleAccountEmail != null && (now - settings.lastBackupTime >= freqMs)) {
                val networkType = if (settings.cloudBackupOnMetered) NetworkType.CONNECTED else NetworkType.UNMETERED
                val immediateCloud = OneTimeWorkRequestBuilder<BackupWorker>()
                    .setInputData(workDataOf("backup_type" to "cloud"))
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(networkType)
                        .setRequiresBatteryNotLow(true)
                        .build())
                    .build()
                workManager.enqueueUniqueWork("cloud_backup_immediate", ExistingWorkPolicy.REPLACE, immediateCloud)
            }

            // 2. Ensure periodic work is enqueued (use KEEP to avoid resetting timer)
            scheduleBackup(context) 
        }
    }

    fun cancelCloudBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("cloud_backup_work")
    }

    fun cancelLocalBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("local_backup_work")
    }

    fun cancelBackup(context: Context) {
        cancelLocalBackup(context)
        cancelCloudBackup(context)
    }

    suspend fun performManualCloudBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        val settings = repository.getAppSettings() ?: return@withContext false
        val email = settings.googleAccountEmail ?: return@withContext false
        val googleDriveService = GoogleDriveService(context, email)

        try {
            // Flush WAL to main DB file
            AppDatabase.checkpoint(context)
            
            val dbFile = context.getDatabasePath("expense_tracker_db")
            if (dbFile.exists()) {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val backupName = "expense_backup_$timestamp.db"

                val tempFile = File(context.cacheDir, backupName)
                dbFile.copyTo(tempFile, overwrite = true)

                val description = "Manual backup on $timestamp"
                val fileId = googleDriveService.uploadBackup(tempFile, description)

                tempFile.delete()

                if (fileId != null) {
                    // Use targeted DAO update to avoid stale-write race with BackupWorker
                    repository.updateLastBackupTime(System.currentTimeMillis())
                    googleDriveService.deleteOldBackups(15)
                    // Re-enqueue periodic work so it picks up from now
                    ensureBackupScheduled(context)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromCloud(context: Context, fileId: String): Boolean = withContext(Dispatchers.IO) {
        val settings = repository.getAppSettings() ?: return@withContext false
        val email = settings.googleAccountEmail ?: return@withContext false
        val googleDriveService = GoogleDriveService(context, email)

        try {
            val tempFile = File(context.cacheDir, "temp_restore.db")
            val success = googleDriveService.downloadBackup(fileId, tempFile)
            
            if (success) {
                // Close database before restoration
                AppDatabase.closeDatabase()
                
                val dbFile = context.getDatabasePath("expense_tracker_db")
                val dbDir = dbFile.parentFile
                
                // Delete all auxiliary database files (WAL, SHM, Journal)
                dbDir?.listFiles()?.forEach { file ->
                    if (file.name.startsWith(dbFile.name)) {
                        file.delete()
                    }
                }

                // Copy with sync
                val inputStream = FileInputStream(tempFile)
                val outputStream = FileOutputStream(dbFile)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                        output.fd.sync() // Ensure data is physically written
                    }
                }
                
                tempFile.delete()
                
                // Restart app on Main thread
                withContext(Dispatchers.Main) {
                    restartApp(context)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun triggerBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = repository.getAppSettings() ?: return@launch
            val folderUri = settings.backupFolderUri ?: return@launch
            
            try {
                // Flush WAL to main DB file
                AppDatabase.checkpoint(context)

                val dbFile = context.getDatabasePath("expense_tracker_db")
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val backupFileName = "expense_backup_$timestamp.db"
                
                val root = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                val backupFile = root?.createFile("application/x-sqlite3", backupFileName)
                
                backupFile?.let { bFile ->
                    context.contentResolver.openOutputStream(bFile.uri)?.use { out ->
                        dbFile.inputStream().use { it.copyTo(out) }
                    }
                    
                    // Use targeted DAO update to avoid stale-write race with BackupWorker
                    repository.updateLastLocalBackupTime(System.currentTimeMillis())
                    
                    // Cleanup old backups (older than 15 days)
                    val fifteenDaysAgo = System.currentTimeMillis() - (15L * 24 * 60 * 60 * 1000)
                    root?.listFiles()?.forEach { file ->
                        val lastMod = file.lastModified()
                        if (file.name?.startsWith("expense_backup_") == true && lastMod > 0 && lastMod < fifteenDaysAgo) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreBackup(context: Context, backupUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Close database before restoration
                AppDatabase.closeDatabase()
                
                val dbFile = context.getDatabasePath("expense_tracker_db")
                val dbDir = dbFile.parentFile

                // Delete all auxiliary database files (WAL, SHM, Journal)
                dbDir?.listFiles()?.forEach { file ->
                    if (file.name.startsWith(dbFile.name)) {
                        file.delete()
                    }
                }

                // Copy with sync
                val inputStream = context.contentResolver.openInputStream(backupUri)
                val outputStream = FileOutputStream(dbFile)
                if (inputStream != null) {
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                            output.fd.sync() // Ensure data is physically written
                        }
                    }
                }
                
                // Restart app on Main thread
                withContext(Dispatchers.Main) {
                    restartApp(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }


    // CALCULATED STATES
    
    val activeBudgetBooks: StateFlow<List<BudgetBook>> = budgetBooks
        .map { list -> list.filter { !it.isClosed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Budget Book Object
    val activeBudgetBook: StateFlow<BudgetBook?> = combine(budgetBooks, selectedBudgetBookId) { books, id ->
        books.find { it.id == id } ?: books.find { it.isDefault } ?: books.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered Transactions for Transactions tab (Optimized filtering logic)
    // Debounced search query to avoid fuzzy search thrashing on every keystroke
    private val debouncedSearchQuery: StateFlow<String> = _searchQuery
        .debounce(300.milliseconds)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        selectedBudgetBookId,
        _transactionsCalendar,
        _filterDateRange,
        _filterAccountIds,
        _filterCategoryNames,
        debouncedSearchQuery,
        _viewMode,
        _filterTransactionTypes
    ) { args ->
        val txs = args[0] as List<Transaction>
        val bookId = args[1] as Long
        val cal = args[2] as Calendar
        val dateRange = args[3] as Pair<Long, Long>?
        val accountIds = args[4] as Set<Long>
        val categoryNames = args[5] as Set<String>
        val query = args[6] as String
        val currentViewMode = args[7] as ViewMode
        val transactionTypes = args[8] as Set<String>

        if (txs.isEmpty()) return@combine emptyList<Transaction>()

        // Pre-calculate date range for non-custom filters
        val (periodStartMs, periodEndMs) = if (dateRange != null) {
            dateRange.first to dateRange.second
        } else {
            val start = (cal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            when (currentViewMode) {
                ViewMode.DAILY, ViewMode.MONTHLY, ViewMode.CYCLE -> {
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }
                    start.timeInMillis to end.timeInMillis
                }
                ViewMode.WEEKLY -> {
                    start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                    val end = (start.clone() as Calendar).apply { add(Calendar.DATE, 7); add(Calendar.MILLISECOND, -1) }
                    start.timeInMillis to end.timeInMillis
                }
                ViewMode.YEARLY -> {
                    start.set(Calendar.DAY_OF_YEAR, 1)
                    val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1); add(Calendar.MILLISECOND, -1) }
                    start.timeInMillis to end.timeInMillis
                }
                ViewMode.CALENDAR -> {
                    // Show 42 days grid range
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    val offset = (start.get(Calendar.DAY_OF_WEEK) - start.firstDayOfWeek + 7) % 7
                    start.add(Calendar.DAY_OF_MONTH, -offset)
                    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 42); add(Calendar.MILLISECOND, -1) }
                    start.timeInMillis to end.timeInMillis
                }
            }
        }
        
        txs.filter { tx ->
            // Budget Book Filter: Match current book or global (-1L)
            // If current bookId is -1L, we show everything (global and book-specific)
            if (bookId != -1L && tx.linkedBudgetBookId != bookId && tx.linkedBudgetBookId != -1L) return@filter false
            
            // Period Filter
            if (tx.dateTime < periodStartMs || tx.dateTime > periodEndMs) return@filter false
            
            // Account Filter
            val matchesAccount = accountIds.isEmpty() || accountIds.contains(tx.linkedAccountId) || (tx.toAccountId != null && accountIds.contains(tx.toAccountId))
            if (!matchesAccount) return@filter false
            
            // Category Filter
            val matchesCategory = categoryNames.isEmpty() || categoryNames.contains(tx.category)
            if (!matchesCategory) return@filter false

            // Transaction Type Filter
            val matchesType = transactionTypes.isEmpty() || transactionTypes.contains(tx.type)
            if (!matchesType) return@filter false

            // Search Query — handled by fuzzy filtering + ranking after all other filters
            true
        }
        // --- Search fuzzy filtering and ranking ---
        .let { prefiltered ->
            if (query.isBlank()) {
                prefiltered.sortedByDescending { it.dateTime }
            } else {
                // Score each transaction: 1.0 = exact substring match, < 1.0 = fuzzy
                prefiltered.map { tx ->
                    tx to FuzzySearch.similarity(query, "${tx.description} ${tx.category}")
                }
                .filter { (_, score) ->
                    score >= FuzzySearch.DEFAULT_THRESHOLD
                }
                .sortedWith(
                    compareByDescending<Pair<Transaction, Double>> { it.second }
                        .thenByDescending { it.first.dateTime }
                )
                .map { it.first }
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Decoupled flow for Home Screen (Optimized: No Calendar creation in loop)
    val homeFilteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        selectedBudgetBookId,
        _transactionsCalendar
    ) { txs, bookId, cal ->
        val startCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        val startMs = startCal.timeInMillis
        val endMs = endCal.timeInMillis

        txs.filter { tx ->
            val bookMatch = bookId == -1L || tx.linkedBudgetBookId == bookId || tx.linkedBudgetBookId == -1L
            bookMatch && tx.dateTime >= startMs && tx.dateTime < endMs
        }.sortedWith(
            compareByDescending<Transaction> { 
                val c = Calendar.getInstance().apply { timeInMillis = it.dateTime }
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }.thenBy { it.isGeneratedFromRecurrence }
             .thenByDescending { it.dateTime }
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current month totals for active book (Now always current month for Home)
    val currentMonthSummary: StateFlow<PeriodSummary> = homeFilteredTransactions.map { txs ->
        val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        PeriodSummary(income, expense, income - expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary(0.0, 0.0, 0.0))

    // All-time transactions for the active Budget Book
    val budgetBookAllTimeTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions, selectedBudgetBookId
    ) { txs, bookId ->
        txs.filter { bookId == -1L || it.linkedBudgetBookId == bookId || it.linkedBudgetBookId == -1L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRepetitiveTransactions: StateFlow<List<RepetitiveTransaction>> = combine(
        allRepetitiveTransactions, selectedBudgetBookId
    ) { reps, bookId ->
        reps.filter { bookId == -1L || it.linkedBudgetBookId == bookId || it.linkedBudgetBookId == -1L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Account list: accounts are irrespective of budget books
    val filteredAccounts: StateFlow<List<Account>> = allAccounts

    // Account Summary details matching screen's period
    fun computeAccountSummaryForPeriod(accountId: Long, month: Int, year: Int): Flow<AccountSummary> = allTransactions.map { txs ->
        val accTxs = txs.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
            (tx.linkedAccountId == accountId || tx.toAccountId == accountId) &&
                    txCal.get(Calendar.MONTH) == month &&
                    txCal.get(Calendar.YEAR) == year
        }
        val income = accTxs.filter { (it.type == "INCOME" && it.linkedAccountId == accountId) || (it.type == "TRANSFER" && it.toAccountId == accountId) }.sumOf { it.amount }
        val expense = accTxs.filter { (it.type == "EXPENSE" && it.linkedAccountId == accountId) || (it.type == "TRANSFER" && it.linkedAccountId == accountId) }.sumOf { it.amount }
        AccountSummary(income, expense, income - expense, accTxs)
    }

    // Account Summary details based on billing cycle (exclusive for credit cards)
    fun computeCreditCardLifecycleSummary(account: Account): Flow<CreditCardBillingSummary> = allTransactions.map { txs ->
        // Include both transactions FROM the account and TO the account (for transfers)
        val cardTxs = txs.filter { it.linkedAccountId == account.id || it.toAccountId == account.id }
        
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_MONTH)

        // Find the start of the current cycle
        val currentCycleStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (today >= account.billingCycleStartDay) {
                set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay)
            } else {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay)
            }
        }

        val prevCycleEndMs = currentCycleStart.timeInMillis - 1
        
        fun getNetDebt(list: List<Transaction>): Double {
            var debt = -account.initialBalance
            list.forEach { tx ->
                if (tx.linkedAccountId == account.id) {
                    when (tx.type) {
                        "EXPENSE", "TRANSFER" -> debt += tx.amount
                        "INCOME" -> debt -= tx.amount
                    }
                }
                if (tx.toAccountId == account.id && tx.type == "TRANSFER") {
                    debt -= tx.amount
                }
            }
            return debt
        }

        // 1. Debt from all previous cycles (Statement Balance)
        val statementDebt = getNetDebt(cardTxs.filter { it.dateTime <= prevCycleEndMs })
        
        // 2. Repayments made in the current cycle
        val currentCycleRepayments = cardTxs.filter { it.dateTime >= currentCycleStart.timeInMillis && it.toAccountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }
        
        // 3. New spending in the current cycle (Expenses & Transfers from card minus Incomes)
        val cycleSpends = cardTxs.filter { 
            it.dateTime >= currentCycleStart.timeInMillis && 
            it.linkedAccountId == account.id && 
            (it.type == "EXPENSE" || it.type == "TRANSFER") &&
            it.description != "Balance Correction" &&
            it.category != "Adjustment"
        }.sumOf { it.amount }

        val cycleIncomes = cardTxs.filter {
            it.dateTime >= currentCycleStart.timeInMillis &&
            it.linkedAccountId == account.id &&
            it.type == "INCOME"
        }.sumOf { it.amount }

        val currentCycleSpend = cycleSpends - cycleIncomes

        // Disjoint results:
        val payableVal = Math.max(0.0, statementDebt - currentCycleRepayments)
        val outstandingVal = currentCycleSpend + (statementDebt - currentCycleRepayments)

        // Transactions for display
        val outstandingTxs = cardTxs.filter { it.dateTime >= currentCycleStart.timeInMillis }
        val prevCycleStart = (currentCycleStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevCycleTxs = cardTxs.filter { it.dateTime >= prevCycleStart.timeInMillis && it.dateTime <= prevCycleEndMs }

        CreditCardBillingSummary(
            outstandingBalance = outstandingVal,
            payableBalance = payableVal,
            isGracePeriod = today > account.billingCycleEndDay,
            dueDateString = "Pay by 5th",
            outstandingTransactions = outstandingTxs,
            payableTransactions = prevCycleTxs
        )
    }

    fun computeCreditCardInsights(account: Account, filterRange: Pair<Long, Long>? = null): Flow<CreditCardDetailedInsights> = allTransactions.map { txs ->
        val cardTxs = txs.filter { it.linkedAccountId == account.id || it.toAccountId == account.id }
        
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_MONTH)

        // 1. Cycle Window Logic (Default Current Cycle)
        val currentCycleStart = getCycleStart(account, 0)
        val currentCycleEnd = (currentCycleStart.clone() as Calendar).apply { 
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }

        val cycleStartMs = currentCycleStart.timeInMillis
        val cycleEndMs = currentCycleEnd.timeInMillis
        val prevCycleEndMs = cycleStartMs - 1

        // Common Signed Balance Helper: Debt is Positive, Credit is Negative
        fun calculateSignedBalance(transactions: List<Transaction>): Double {
            return transactions.sumOf { tx ->
                if (tx.linkedAccountId == account.id) {
                    when (tx.type) {
                        "EXPENSE", "TRANSFER" -> tx.amount
                        "INCOME" -> -tx.amount
                        else -> 0.0
                    }
                } else if (tx.toAccountId == account.id && tx.type == "TRANSFER") {
                    -tx.amount
                } else 0.0
            }
        }

        // Apply filter range if provided for summary stats, otherwise use current cycle
        val summaryStartMs = filterRange?.first ?: cycleStartMs
        val summaryEndMs = filterRange?.second ?: cycleEndMs
        val summaryTxs = cardTxs.filter { it.dateTime in summaryStartMs..summaryEndMs }

        // 2. BILLED SPEND (Filtered: includes EXPENSE and TRANSFER from card, minus INCOME on card)
        val cycleSpends = summaryTxs.filter { 
            it.linkedAccountId == account.id && 
            (it.type == "EXPENSE" || it.type == "TRANSFER") &&
            it.description != "Balance Correction" &&
            it.category != "Adjustment"
        }.sumOf { it.amount }

       /* val cycleIncomes = summaryTxs.filter {
            it.linkedAccountId == account.id && it.type == "INCOME"
        }.sumOf { it.amount }
        */

        //val currentCycleNetSpend = cycleSpends - cycleIncomes


        // 3. REDUCTIONS (Filtered)
        val cyclePayments = summaryTxs.filter { it.toAccountId == account.id && it.type == "TRANSFER" && it.isCreditCardPayment }.sumOf { it.amount }
        val cycleCashback = summaryTxs.filter { 
            it.linkedAccountId == account.id && it.type == "INCOME" && it.category == "Cashback"
        }.sumOf { it.amount }
        val cycleRefunds = summaryTxs.filter { 
            it.linkedAccountId == account.id && it.type == "INCOME" && (it.category == "Refund" || it.category == "Refunds")
        }.sumOf { it.amount }
        
        val totalReductionThisCycle = cyclePayments + cycleCashback + cycleRefunds

        // 4. OUTSTANDING NOW (Cumulative up to range end)
        val initialDebt = -account.initialBalance
        val carryForwardSignedBalance = initialDebt + calculateSignedBalance(cardTxs.filter { it.dateTime < summaryStartMs })
        val totalSignedBalance = initialDebt + calculateSignedBalance(cardTxs)
        val outstandingNow = totalSignedBalance

        // 5. PAYABLE BALANCE
        val payableVal = maxOf(0.0, carryForwardSignedBalance - totalReductionThisCycle)

        // 6. FEE WAIVER PROGRESS (Yearly)
        var yearlyEligibleSpend = 0.0
        var isFeeThresholdReached = false
        var daysRemaining = 0
        
        if (account.trackFeeWaiver && account.feeCycleStartDate != null && account.feeWaiverThreshold != null) {
            val feeStart = account.feeCycleStartDate!!
            val feeEndMs = (Calendar.getInstance().apply { timeInMillis = feeStart; add(Calendar.YEAR, 1) }).timeInMillis
            
            val periodTxs = cardTxs.filter { it.dateTime in feeStart until feeEndMs }
            val qualifyingExpenses = periodTxs.filter { isFeeWaiverQualifying(it, account.id) }.sumOf { it.amount }
            val qualifyingRefunds = periodTxs.filter { isFeeWaiverRefund(it, account.id) }.sumOf { it.amount }
            
            yearlyEligibleSpend = maxOf(0.0, qualifyingExpenses - qualifyingRefunds)
            isFeeThresholdReached = yearlyEligibleSpend >= account.feeWaiverThreshold!!
            
            daysRemaining = ((feeEndMs - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
        }

        val lastPayment = summaryTxs.filter { it.toAccountId == account.id && it.type == "TRANSFER" && it.isCreditCardPayment }
            .maxByOrNull { it.dateTime }?.dateTime

        val isGracePeriod = today > account.billingCycleEndDay

        CreditCardDetailedInsights(
            currentCycleSpend = cycleSpends,
            paidThisCycle = cyclePayments,
            creditsAppliedThisCycle = cycleCashback + cycleRefunds,
            totalReductionThisCycle = totalReductionThisCycle,
            outstandingBalance = outstandingNow,
            payableBalance = payableVal,
            feeWaiverProgress = yearlyEligibleSpend,
            feeWaiverThreshold = account.feeWaiverThreshold ?: 0.0,
            isFeeThresholdReached = isFeeThresholdReached,
            feeCycleDaysRemaining = daysRemaining,
            lastPaymentDate = lastPayment,
            cycleStartDate = summaryStartMs,
            cycleEndDate = summaryEndMs,
            isGracePeriod = isGracePeriod
        )
    }

    fun getPaymentHistory(accountId: Long, filterRange: Pair<Long, Long>? = null): Flow<List<PaymentHistoryItem>> = combine(allTransactions, allAccounts) { txs, accs ->
        val cardTxs = txs.filter { it.linkedAccountId == accountId || it.toAccountId == accountId }
        
        val history = mutableListOf<PaymentHistoryItem>()
        
        cardTxs.forEach { tx ->
            if (filterRange != null && (tx.dateTime < filterRange.first || tx.dateTime > filterRange.second)) return@forEach

            if (tx.toAccountId == accountId && tx.type == "TRANSFER" && tx.isCreditCardPayment) {
                val sourceAcc = accs.find { it.id == tx.linkedAccountId }?.name ?: "Unknown"
                history.add(PaymentHistoryItem(tx.id, tx.dateTime, tx.amount, "PAYMENT", sourceAcc, tx.description))
            } else if (tx.linkedAccountId == accountId && tx.type == "INCOME" && (tx.category == "Cashback" || tx.category == "Refund" || tx.category == "Refunds")) {
                history.add(PaymentHistoryItem(tx.id, tx.dateTime, tx.amount, "CREDIT", tx.category, tx.description))
            }
        }
        
        history.sortedByDescending { it.date }
    }

    // Account balance calculation for all accounts
    // Optimized balance calculation: One pass over transactions instead of N
    val accountBalances: StateFlow<Map<Long, Double>> = combine(allTransactions, allAccounts, allGiftCards) { txs, accs, giftCards ->
        // Start with initial balance for each account
        val balances = accs.associate { it.id to it.initialBalance }.toMutableMap()

        txs.forEach { tx ->
            val amount = tx.amount
            // Update source account
            balances[tx.linkedAccountId] = (balances[tx.linkedAccountId] ?: 0.0) + when (tx.type) {
                "INCOME" -> amount
                "EXPENSE", "TRANSFER" -> -amount
                else -> 0.0
            }
            // Update destination account for transfers
            if (tx.type == "TRANSFER" && tx.toAccountId != null) {
                balances[tx.toAccountId!!] = (balances[tx.toAccountId!!] ?: 0.0) + amount
            }
        }
        
        // For GIFT_CARD accounts, the balance is the sum of unused and non-expired gift cards
        accs.forEach { acc ->
            if (acc.type == "GIFT_CARD") {
                val unusedValue = giftCards.filter { it.linkedAccountId == acc.id && !it.isUsed && !it.isExpired() }.sumOf { it.amount }
                balances[acc.id] = unusedValue
            }
        }

        balances
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun getAccountBalanceFlow(accountId: Long): Flow<Double> = accountBalances.map { it[accountId] ?: 0.0 }

    /**
     * Daily closing balances for ALL accounts, keyed by [accountId][dateMs].
     *
     * COMPUTATION DIRECTION: Chronological (oldest-to-newest), starting from each
     * account's opening balance. The result maps each day's midnight timestamp to
     * the running balance *after* applying that day's transactions.
     *
     * INCLUDES ALL ENTRY TYPES): Transfers, Balance Corrections, recurring
     * transactions — every entry that modifies the account balance.
     *
     * FILTER INDEPENDENCE: This Flow uses the unfiltered allTransactions list.
     * The per-day "net total" on the UI may reflect user-applied filters, but
     * the displayed balance here always reflects the TRUE full account balance.
     *
     * CREDIT CARD SIGN CONVENTION: For credit cards, the balance naturally
     * reflects debt (positive = owed, negative = credit/overpayment). The caller
     * inverts the color convention for display (red = debt, green = credit).
     */
    val dailyClosingBalances: StateFlow<Map<Long, Map<Long, Double>>> = combine(allTransactions, allAccounts) { txs, accs ->
        val result = mutableMapOf<Long, MutableMap<Long, Double>>()

        accs.forEach { account ->
            // Get all transactions for this account (including transfers both ways)
            val accountTxs = txs
                .filter { it.linkedAccountId == account.id || it.toAccountId == account.id }
                .sortedBy { it.dateTime } // chronological

            val dateBalances = mutableMapOf<Long, Double>()
            // Seed: ALL transactions from epoch until today contribute from opening balance
            var running = account.initialBalance

            if (accountTxs.isEmpty()) {
                // No transactions at all: every day balance = opening balance
                result[account.id] = dateBalances
                return@forEach
            }

            // Group chronologically sorted txs by day
            var currentDay: Long? = null
            var dayRunning = running

            accountTxs.forEach { tx ->
                val txDay = getDateKeyMs(tx.dateTime)

                // When we hit a new day, store previous day's closing balance
                if (currentDay != null && currentDay != txDay) {
                    dateBalances[currentDay!!] = dayRunning
                }
                if (currentDay == null || currentDay != txDay) {
                    // First transaction of a new day: start from running balance
                    currentDay = txDay
                    dayRunning = running
                }

                // Apply transaction delta
                dayRunning += getTransactionDelta(tx, account.id)
                running += getTransactionDelta(tx, account.id)
            }
            // Store the last day's closing balance
            if (currentDay != null) {
                dateBalances[currentDay] = dayRunning
            }

            result[account.id] = dateBalances
        }
        result
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Returns this transaction's net effect on the given account's balance.
     * Positive = increases balance (reduces debt for CC)), negative = decreases balance.
     *
     * For TRANSFER out of this account: -amount {outflow}
     * For TRANSFER into this account: +amount {inflow}
     * For EXPENSE: -amount
     * For INCOME in linked account: +amount
     */
    private fun getTransactionDelta(tx: Transaction, accountId: Long): Double {
        return when {
            // This account is source
            tx.linkedAccountId == accountId -> when (tx.type) {
                "INCOME" -> tx.amount
                "EXPENSE", "TRANSFER" -> -tx.amount
                else -> 0.0
            }
            // This account is destination (transfer receiver)
            tx.toAccountId == accountId && tx.type == "TRANSFER" -> tx.amount
            else -> 0.0
        }
    }

    /**
     * Truncates epoch millis to midnight of that day.
     */
    private fun getDateKeyMs(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Convenience: get daily balances for a specific account
     */
    fun getDailyBalancesForAccount(accountId: Long): Flow<Map<Long, Double>> =
        dailyClosingBalances.map { it[accountId] ?: emptyMap() }

    // Amount I owe: Sum of net liabilities across all Credit Card accounts
    val totalAmountOwed: StateFlow<Double> = combine(
        allAccounts, allTransactions
    ) { accs, txs ->
        val ccIds = accs.filter { it.isCreditCard }.map { it.id }.toSet()
        if (ccIds.isEmpty()) return@combine 0.0

        val debts = mutableMapOf<Long, Double>()
        txs.forEach { tx ->
            if (ccIds.contains(tx.linkedAccountId)) {
                val current = debts.getOrDefault(tx.linkedAccountId, 0.0)
                debts[tx.linkedAccountId] = when (tx.type) {
                    "EXPENSE", "TRANSFER" -> current + tx.amount
                    "INCOME" -> current - tx.amount
                    else -> current
                }
            }
            if (tx.toAccountId != null && ccIds.contains(tx.toAccountId) && tx.type == "TRANSFER") {
                val current = debts.getOrDefault(tx.toAccountId, 0.0)
                debts[tx.toAccountId] = current - tx.amount
            }
        }
        debts.values.filter { it > 0 }.sum()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val creditCardTotalSummary: StateFlow<Pair<Double, Double>> = combine(
        allAccounts, allTransactions
    ) { accs, txs ->
        val creditCards = accs.filter { it.isCreditCard }
        if (creditCards.isEmpty()) return@combine Pair(0.0, 0.0)

        var totalOutstanding = 0.0
        var totalPayable = 0.0
        
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_MONTH)

        creditCards.forEach { account ->
            val allCardTxs = txs.filter { it.linkedAccountId == account.id || it.toAccountId == account.id }
            val currentCycleStart = getCycleStart(account, 0)
            val cycleStartMs = currentCycleStart.timeInMillis

            val initialDebt = -account.initialBalance
            var cardTotalSignedBalance = initialDebt
            var carryForwardSignedBalance = initialDebt
            
            allCardTxs.forEach { tx ->
                val delta = if (tx.linkedAccountId == account.id) {
                    when (tx.type) {
                        "EXPENSE", "TRANSFER" -> tx.amount
                        "INCOME" -> -tx.amount
                        else -> 0.0
                    }
                } else if (tx.toAccountId == account.id && tx.type == "TRANSFER") {
                    -tx.amount
                } else 0.0
                
                cardTotalSignedBalance += delta
                if (tx.dateTime < cycleStartMs) {
                    carryForwardSignedBalance += delta
                }
            }
            
            val currentCycleReductions = allCardTxs.filter { it.dateTime >= cycleStartMs }.sumOf { tx ->
                if (tx.toAccountId == account.id && tx.type == "TRANSFER" && tx.isCreditCardPayment) tx.amount
                else if (tx.linkedAccountId == account.id && tx.type == "INCOME" && (tx.category == "Cashback" || tx.category == "Refund" || tx.category == "Refunds")) tx.amount
                else 0.0
            }

            totalOutstanding += cardTotalSignedBalance
            totalPayable += maxOf(0.0, carryForwardSignedBalance - currentCycleReductions)
        }
        Pair(totalOutstanding, totalPayable)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // ALL ACCOUNTS statistics (regardless of active budget book)
    val accountsGlobalSummary: StateFlow<PeriodSummary> = combine(
        allTransactions, _accountsCalendar
    ) { txs, cal ->
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        
        val currentTxs = txs.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
            txCal.get(Calendar.MONTH) == month && txCal.get(Calendar.YEAR) == year
        }
        val income = currentTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = currentTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        PeriodSummary(income, expense, income - expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary(0.0, 0.0, 0.0))

    val netWorthSummary: StateFlow<NetWorthSummary> = combine(accountBalances, allAccounts) { balances, accs ->
        val assets = accs.filter { !it.isCreditCard }
            .sumOf { (balances[it.id] ?: 0.0).coerceAtLeast(0.0) }
        val creditDues = accs.filter { it.isCreditCard }
            .sumOf { maxOf(0.0, -(balances[it.id] ?: 0.0)) }
        val creditCredits = accs.filter { it.isCreditCard }
            .sumOf { maxOf(0.0, balances[it.id] ?: 0.0) }
        
        NetWorthSummary(
            netWorth = assets + creditCredits - creditDues,
            totalAssets = assets + creditCredits,
            totalCreditDues = creditDues
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthSummary(0.0, 0.0, 0.0))

    val accountGroupTotals: StateFlow<Map<String, Double>> = combine(accountBalances, allAccounts) { balances, accs ->
        accs.groupBy { it.type }.mapValues { entry ->
            entry.value.sumOf { balances[it.id] ?: 0.0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // DATA EXPORT FUNCTION
    fun exportTransactionsToCSV(): String {
        val accs = allAccounts.value
        val txs = allTransactions.value
        val reps = allRepetitiveTransactions.value
        val books = budgetBooks.value
        val giftCards = allGiftCards.value
        
        val builder = StringBuilder()
        // Header
        builder.append("Date,Amount,Type,Category,Subcategory,Description,AccountName,ToAccountName,BudgetBookName,IsRecurringTemplate,Frequency,Interval,ReflectionTiming,EndDate,Occurrences,DayOfWeek,DayOfMonth,IsCcPayment,GiftCardCode,GiftCardBrand,GiftCardCustomBrand,GiftCardAmount,GiftCardPurchaseDate,GiftCardValidity,GiftCardIsUsed,GiftCardUsedDate,GiftCardNote\n")
        
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        // Export Regular Transactions
        txs.forEach { tx ->
            val acc = accs.find { it.id == tx.linkedAccountId }?.name ?: ""
            val toAcc = accs.find { it.id == tx.toAccountId }?.name ?: ""
            val book = books.find { it.id == tx.linkedBudgetBookId }?.name ?: ""
            val dateStr = df.format(Date(tx.dateTime))
            
            val gc = giftCards.find { it.id == tx.linkedGiftCardId }
            val gcData = if (gc != null) {
                ",\"${gc.code}\",${gc.brand},\"${gc.customBrandName ?: ""}\",${gc.amount},${gc.purchaseDate},${gc.validityDays},${gc.isUsed},${gc.usedDate ?: ""},\"${(gc.note ?: "").replace("\"", "\"\"")}\""
            } else {
                ",,,,,,,,,"
            }
            
            builder.append("$dateStr,${tx.amount},${tx.type},\"${tx.category.replace("\"", "\"\"")}\",\"${(tx.subCategory ?: "").replace("\"", "\"\"")}\",\"${tx.description.replace("\"", "\"\"")}\",$acc,$toAcc,$book,false,,,,,,,${tx.isCreditCardPayment}${gcData}\n")
        }

        // Export Recurring Templates
        reps.forEach { rep ->
            val acc = accs.find { it.id == rep.linkedAccountId }?.name ?: ""
            val toAcc = accs.find { it.id == rep.toAccountId }?.name ?: ""
            val book = books.find { it.id == rep.linkedBudgetBookId }?.name ?: ""
            val dateStr = df.format(Date(rep.startDate))
            val endStr = if (rep.endDate != null) df.format(Date(rep.endDate)) else ""
            
            val gc = giftCards.find { it.id == rep.linkedGiftCardId }
            val gcData = if (gc != null) {
                ",\"${gc.code}\",${gc.brand},\"${gc.customBrandName ?: ""}\",${gc.amount},${gc.purchaseDate},${gc.validityDays},${gc.isUsed},${gc.usedDate ?: ""},\"${(gc.note ?: "").replace("\"", "\"\"")}\""
            } else {
                ",,,,,,,,,"
            }

            builder.append("$dateStr,${rep.amount},${rep.type},\"${rep.category.replace("\"", "\"\"")}\",\"${(rep.subCategory ?: "").replace("\"", "\"\"")}\",\"${rep.description.replace("\"", "\"\"")}\",$acc,$toAcc,$book,true,${rep.frequency},${rep.interval},${rep.reflectionTiming},$endStr,${rep.occurrences ?: ""},${rep.dayOfWeek ?: ""},${rep.dayOfMonth ?: ""},${rep.isCreditCardPayment}${gcData}\n")
        }

        // Export Standalone Gift Cards (those not linked to any transaction or template)
        val linkedGcIds = (txs.mapNotNull { it.linkedGiftCardId } + reps.mapNotNull { it.linkedGiftCardId }).toSet()
        val standaloneGcs = giftCards.filter { it.id !in linkedGcIds }
        
        standaloneGcs.forEach { gc ->
            val acc = accs.find { it.id == gc.linkedAccountId }?.name ?: ""
            val dateStr = df.format(Date(gc.purchaseDate))
            val gcData = ",\"${gc.code}\",${gc.brand},\"${gc.customBrandName ?: ""}\",${gc.amount},${gc.purchaseDate},${gc.validityDays},${gc.isUsed},${gc.usedDate ?: ""},\"${(gc.note ?: "").replace("\"", "\"\"")}\""
            
            builder.append("$dateStr,${gc.amount},GIFT_CARD,\"\",,\"Gift Card: ${gc.code}\",$acc,,,\"\",,,,,,,,${gcData}\n")
        }
        
        return builder.toString()
    }

    fun importTransactionsFromCSV(csvData: String) {
        viewModelScope.launch {
            val lines = csvData.split("\n")
            if (lines.size <= 1) return@launch
            
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val accs = allAccounts.value
            val books = budgetBooks.value
            
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue
                
                // Robust CSV splitting to handle commas within quotes
                val parts = mutableListOf<String>()
                var currentPart = StringBuilder()
                var inQuotes = false
                for (char in line) {
                    when {
                        char == '\"' -> inQuotes = !inQuotes
                        char == ',' && !inQuotes -> {
                            parts.add(currentPart.toString())
                            currentPart = StringBuilder()
                        }
                        else -> currentPart.append(char)
                    }
                }
                parts.add(currentPart.toString())
                
                if (parts.size < 8) {
                    Log.e("Import", "Skipping line $i: expected at least 8 parts, found ${parts.size}")
                    continue
                }
                
                try {
                    // Try multiple date formats for better user experience
                    val dateStr = parts[0].trim().replace("\\s+".toRegex(), " ") // Normalize spaces
                    val fallbackFormats = listOf(
                        "dd-MM-yyyy hh:mm:ss a",
                        "MM-dd-yyyy hh:mm:ss a",
                        "yyyy-MM-dd HH:mm:ss",
                        "dd-MM-yyyy",
                        "MM-dd-yyyy",
                        "yyyy-MM-dd",
                        "dd-MM",
                        "MM-dd"
                    )
                    
                    var parsedDate: Date? = null
                    for (format in fallbackFormats) {
                        try {
                            val sdf = SimpleDateFormat(format, Locale.US)
                            parsedDate = sdf.parse(dateStr)
                            if (parsedDate != null) {
                                // If short format like dd-MM, default to current year
                                if (format == "dd-MM" || format == "MM-dd") {
                                    val cal = Calendar.getInstance()
                                    val currentYear = cal.get(Calendar.YEAR)
                                    cal.time = parsedDate
                                    cal.set(Calendar.YEAR, currentYear)
                                    parsedDate = cal.time
                                }
                                break
                            }
                        } catch (e: Exception) { /* continue */ }
                    }
                    
                    val date = parsedDate?.time ?: System.currentTimeMillis()
                    val amount = parts[1].trim().toDoubleOrNull() ?: 0.0
                    val type = parts[2].trim().uppercase()
                    
                    // Skip empty/malformed rows (common in spreadsheet exports)
                    if (amount == 0.0 && type.isEmpty() && parts[3].trim().isEmpty()) continue
                    if (!listOf("INCOME", "EXPENSE", "TRANSFER", "GIFT_CARD").contains(type)) continue

                    // Match category more robustly
                    val rawCategory = parts[3].trim()
                    val matchedCategory = allCategories.value.find { it.name.equals(rawCategory, ignoreCase = true) }
                    val categoryName = matchedCategory?.name ?: rawCategory
                    
                    val subCategory = if (parts.size > 4) parts[4].trim().ifEmpty { null } else null

                    val description = if (parts.size > 5) parts[5].trim() else ""
                    val accName = if (parts.size > 6) parts[6].trim() else ""
                    val toAccName = if (parts.size > 7) parts[7].trim() else ""
                    val bookName = if (parts.size > 8) parts[8].trim() else ""
                    
                    val isRecurringTemplate = if (parts.size > 9) parts[9].trim().toBoolean() else false
                    val isCcPayment = if (parts.size > 17) parts[17].trim().toBoolean() else false
                    
                    val accId = accs.find { it.name.equals(accName, ignoreCase = true) }?.id 
                        ?: accs.firstOrNull { it.type == "CASH" }?.id 
                        ?: accs.firstOrNull()?.id ?: -1L
                        
                    val toAccId = if (toAccName.isNotEmpty()) accs.find { it.name.equals(toAccName, ignoreCase = true) }?.id else null
                    
                    val bookId = if (type == "TRANSFER") {
                        books.find { it.isDefault }?.id ?: books.firstOrNull()?.id ?: -1L
                    } else {
                        books.find { it.name.equals(bookName, ignoreCase = true) }?.id ?: books.firstOrNull()?.id ?: -1L
                    }

                    if (isRecurringTemplate && parts.size >= 13) {
                        val frequency = if (parts.size > 10) parts[10].trim() else "MONTHLY"
                        val interval = if (parts.size > 11) parts[11].trim().toIntOrNull() ?: 1 else 1
                        val reflectionTiming = if (parts.size > 12) parts[12].trim() else "ON_DATE"
                        val endDate = if (parts.size > 13 && parts[13].isNotEmpty()) {
                            try { df.parse(parts[13])?.time } catch (e: Exception) { null }
                        } else null
                        val occurrences = if (parts.size > 14) parts[14].trim().toIntOrNull() else null
                        val dayOfWeek = if (parts.size > 15) parts[15].trim().toIntOrNull() else null
                        val dayOfMonth = if (parts.size > 16) parts[16].trim().toIntOrNull() else null
                        
                        // Gift Card handling
                        var linkedGiftCardId: Long? = null
                        if (parts.size >= 27 && parts[18].isNotBlank()) {
                            val gcCode = parts[18].trim()
                            val gcBrand = parts[19].trim()
                            val gcCustomBrand = parts[20].trim().ifEmpty { null }
                            val gcAmount = parts[21].trim().toDoubleOrNull() ?: 0.0
                            val gcPurchaseDate = parts[22].trim().toLongOrNull() ?: date
                            val gcValidity = parts[23].trim().toIntOrNull() ?: 365
                            val gcIsUsed = parts[24].trim().toBoolean()
                            val gcUsedDate = parts[25].trim().toLongOrNull()
                            val gcNote = parts[26].trim().ifEmpty { null }
                            
                            val gcAccId = accs.find { it.name.equals(accName, ignoreCase = true) }?.id ?: accId
                            
                            linkedGiftCardId = repository.insertGiftCard(
                                GiftCard(
                                    linkedAccountId = gcAccId,
                                    brand = gcBrand,
                                    customBrandName = gcCustomBrand,
                                    code = gcCode,
                                    amount = gcAmount,
                                    purchaseDate = gcPurchaseDate,
                                    validityDays = gcValidity,
                                    isUsed = gcIsUsed,
                                    usedDate = gcUsedDate,
                                    note = gcNote
                                )
                            )
                        }

                        if (type == "GIFT_CARD") {
                            // Already handled gift card insertion
                        } else {
                            repository.insertRepetitiveTransaction(
                                RepetitiveTransaction(
                                    startDate = date,
                                    amount = amount,
                                    type = type,
                                    category = categoryName,
                                    subCategory = subCategory,
                                    frequency = frequency,
                                    interval = interval,
                                    linkedAccountId = accId,
                                    toAccountId = toAccId,
                                    linkedBudgetBookId = bookId,
                                    description = description,
                                    endDate = endDate,
                                    occurrences = occurrences,
                                    dayOfWeek = dayOfWeek,
                                    dayOfMonth = dayOfMonth,
                                    reflectionTiming = reflectionTiming,
                                    linkedGiftCardId = linkedGiftCardId,
                                    isCreditCardPayment = isCcPayment
                                )
                            )
                        }
                    } else {
                        // Gift Card handling for regular transactions
                        var linkedGiftCardId: Long? = null
                        if (parts.size >= 27 && parts[18].isNotBlank()) {
                            val gcCode = parts[18].trim()
                            val gcBrand = parts[19].trim()
                            val gcCustomBrand = parts[20].trim().ifEmpty { null }
                            val gcAmount = parts[21].trim().toDoubleOrNull() ?: 0.0
                            val gcPurchaseDate = parts[22].trim().toLongOrNull() ?: date
                            val gcValidity = parts[23].trim().toIntOrNull() ?: 365
                            val gcIsUsed = parts[24].trim().toBoolean()
                            val gcUsedDate = parts[25].trim().toLongOrNull()
                            val gcNote = parts[26].trim().ifEmpty { null }
                            
                            val gcAccId = accs.find { it.name.equals(accName, ignoreCase = true) }?.id ?: accId
                            
                            linkedGiftCardId = repository.insertGiftCard(
                                GiftCard(
                                    linkedAccountId = gcAccId,
                                    brand = gcBrand,
                                    customBrandName = gcCustomBrand,
                                    code = gcCode,
                                    amount = gcAmount,
                                    purchaseDate = gcPurchaseDate,
                                    validityDays = gcValidity,
                                    isUsed = gcIsUsed,
                                    usedDate = gcUsedDate,
                                    note = gcNote
                                )
                            )
                        }

                        if (type == "GIFT_CARD") {
                            // Already handled
                        } else {
                            repository.insertTransaction(
                                Transaction(
                                    dateTime = date,
                                    amount = amount,
                                    type = type,
                                    category = categoryName,
                                    subCategory = subCategory,
                                    description = description,
                                    linkedAccountId = accId,
                                    toAccountId = toAccId,
                                    linkedBudgetBookId = bookId,
                                    linkedGiftCardId = linkedGiftCardId,
                                    isCreditCardPayment = isCcPayment
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            generateRepetitiveTransactions(fromDateMs = 0L)
            
            _isLoading.value = false
        }
    }

    fun exportDataToString(): String = exportTransactionsToCSV()

    /**
     * Determines if a transaction contributes to a credit card's annual fee-waiver spend.
     * Both "EXPENSE" (purchases) and "TRANSFER" (transfers from card) qualify, excluding manual balance adjustments.
     */
    fun isFeeWaiverQualifying(tx: Transaction, accountId: Long): Boolean {
        return tx.linkedAccountId == accountId && 
               (tx.type == "EXPENSE" || tx.type == "TRANSFER") && 
               tx.description != "Balance Correction" &&
               tx.category != "Adjustment"
    }

    /**
     * Toggles the closed status of an account.
     */
    fun toggleAccountClosed(account: Account) {
        viewModelScope.launch {
            val updated = account.copy(isClosed = !account.isClosed, isActive = account.isClosed)
            repository.updateAccount(updated)
        }
    }

    /**
     * Determines if a transaction is a refund that reduces a credit card's annual fee-waiver spend.
     */
    fun isFeeWaiverRefund(tx: Transaction, accountId: Long): Boolean {
        return tx.linkedAccountId == accountId && 
               tx.type == "INCOME" && 
               (tx.category == "Refund" || tx.category == "Refunds")
    }
}

// Data holder classes
data class PeriodSummary(val totalIncome: Double, val totalExpense: Double, val netBalance: Double)
data class NetWorthSummary(val netWorth: Double, val totalAssets: Double, val totalCreditDues: Double)
data class AccountSummary(val totalIncome: Double, val totalExpense: Double, val netBalance: Double, val transactions: List<Transaction>)
data class CreditCardBillingSummary(val outstandingBalance: Double, val payableBalance: Double, val isGracePeriod: Boolean, val dueDateString: String, val outstandingTransactions: List<Transaction>, val payableTransactions: List<Transaction>)
data class GroupedSummary(val label: String, val income: Double, val expense: Double, val net: Double, val dateRange: String)
