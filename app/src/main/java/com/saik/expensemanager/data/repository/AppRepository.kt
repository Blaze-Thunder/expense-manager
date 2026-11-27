package com.saik.expensemanager.data.repository

import com.saik.expensemanager.data.dao.*
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.AppSettings
import com.saik.expensemanager.data.model.CategoryAlert
import com.saik.expensemanager.data.model.GiftCard
import com.saik.expensemanager.data.model.GiftCardBrand
import com.saik.expensemanager.data.model.TransactionTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import com.saik.expensemanager.data.model.RepetitiveTransaction
import com.saik.expensemanager.util.PinUtils

class AppRepository(
    private val budgetBookDao: BudgetBookDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val appSettingsDao: AppSettingsDao,
    private val categoryAlertDao: CategoryAlertDao,
    private val repetitiveTransactionDao: RepetitiveTransactionDao,
    private val giftCardDao: GiftCardDao,
    private val transactionTemplateDao: TransactionTemplateDao,
) {
    // Flows
    val allBudgetBooks: Flow<List<BudgetBook>> = budgetBookDao.getAllBudgetBooksFlow()
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccountsFlow()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategoriesFlow()
    val appSettings: Flow<AppSettings?> = appSettingsDao.getAppSettingsFlow()
    val allCategoryAlerts: Flow<List<CategoryAlert>> = categoryAlertDao.getAllAlertsFlow()
    val allRepetitiveTransactions: Flow<List<RepetitiveTransaction>> = repetitiveTransactionDao.getAllRepetitiveTransactionsFlow()
    val allGiftCards: Flow<List<GiftCard>> = giftCardDao.getAll()
    val allTemplates: Flow<List<TransactionTemplate>> = transactionTemplateDao.getAllTemplatesFlow()
    val pendingCardAlertsRaw: Flow<List<com.saik.expensemanager.data.model.CardSpendStatusRaw>> = accountDao.getPendingFeeWaiverAlertsFlow()

    suspend fun getAllRepetitiveTransactions(): List<RepetitiveTransaction> {
        return repetitiveTransactionDao.getAllRepetitiveTransactions()
    }

    suspend fun insertRepetitiveTransaction(rep: RepetitiveTransaction): Long {
        return repetitiveTransactionDao.insertRepetitiveTransaction(rep)
    }

    suspend fun updateRepetitiveTransaction(rep: RepetitiveTransaction) {
        repetitiveTransactionDao.updateRepetitiveTransaction(rep)
    }

    suspend fun deleteRepetitiveTransaction(rep: RepetitiveTransaction) {
        repetitiveTransactionDao.deleteRepetitiveTransaction(rep)
    }

    suspend fun insertGiftCard(card: GiftCard): Long {
        return giftCardDao.insert(card)
    }

    // Budget Book actions
    suspend fun insertBudgetBook(budgetBook: BudgetBook): Long {
        return budgetBookDao.insertBudgetBook(budgetBook)
    }

    suspend fun updateBudgetBook(budgetBook: BudgetBook) {
        budgetBookDao.updateBudgetBook(budgetBook)
    }

    suspend fun setDefaultBudgetBook(bookId: Long) {
        budgetBookDao.setDefaultBook(bookId)
    }

    suspend fun deleteBudgetBook(budgetBook: BudgetBook) {
        budgetBookDao.deleteBudgetBook(budgetBook)
    }

    // Account actions
    suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun updateAccounts(accounts: List<Account>) {
        accountDao.updateAccounts(accounts)
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account)
    }

    // Transactions actions
    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction)
        if (transaction.linkedGiftCardId != null && transaction.type == "TRANSFER") {
            markGiftCardUsed(transaction.linkedGiftCardId)
        }
        return id
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun getGeneratedTransactionKeys(): List<TransactionDao.GeneratedKey> {
        return transactionDao.getGeneratedTransactionKeys()
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        giftCardDao.deleteByEntryTransactionId(transaction.id)
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteFutureGeneratedTransactions(now: Long) {
        transactionDao.deleteFutureGeneratedTransactions(now)
    }

    suspend fun deleteFutureGeneratedTransactionsForRep(repId: Long, now: Long) {
        transactionDao.deleteFutureGeneratedTransactionsForRep(repId, now)
    }

    // Category actions
    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        val oldCategory = categoryDao.getCategoryById(category.id)
        if (oldCategory != null && oldCategory.name != category.name) {
            if (category.parentId == null) {
                // Primary category
                transactionDao.updateCategoryName(oldCategory.name, category.name, category.type)
                repetitiveTransactionDao.updateCategoryName(oldCategory.name, category.name, category.type)
            } else {
                // Subcategory
                transactionDao.updateSubCategoryName(oldCategory.name, category.name, category.type)
                repetitiveTransactionDao.updateSubCategoryName(oldCategory.name, category.name, category.type)
            }
            // Update alerts for this category name (could be primary or subcategory)
            categoryAlertDao.updateCategoryName(oldCategory.name, category.name)
        }
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    // Settings
    suspend fun updateAppSettings(settings: AppSettings) {
        appSettingsDao.insertAppSettings(settings)
    }

    suspend fun getAppSettings(): AppSettings? {
        return appSettingsDao.getAppSettings()
    }

    suspend fun updateLastBackupTime(time: Long) = appSettingsDao.updateLastBackupTime(time)

    suspend fun updateLastLocalBackupTime(time: Long) = appSettingsDao.updateLastLocalBackupTime(time)

    suspend fun setAppPin(pin: String) {
        val salt = PinUtils.generateSalt()
        val hash = PinUtils.hashPin(pin, salt)
        appSettingsDao.updatePin(hash, salt)
        appSettingsDao.updateAppLockEnabled(true)
    }

    suspend fun setAppLockEnabled(enabled: Boolean) = appSettingsDao.updateAppLockEnabled(enabled)
    suspend fun setBiometricLockEnabled(enabled: Boolean) = appSettingsDao.updateBiometricEnabled(enabled)
    suspend fun clearAppPin() {
        appSettingsDao.updatePin(null, null)
        appSettingsDao.updateAppLockEnabled(enabled = false)
        appSettingsDao.updateBiometricEnabled(enabled = false)
    }

    // Category Alert actions
    suspend fun insertCategoryAlert(alert: CategoryAlert) {
        categoryAlertDao.insertAlert(alert)
    }

    suspend fun deleteCategoryAlert(alert: CategoryAlert) {
        categoryAlertDao.deleteAlert(alert)
    }

    // Seed Data helpers
    suspend fun populateDefaultCategoriesIfNeeded() {
        val existing = categoryDao.getAllCategories()
        if (existing.isEmpty()) {
            // Insert Income categories first
            categoryDao.insertCategory(Category(name = "Salary", type = "INCOME", color = 0xFF4CAF50.toInt(), iconName = "Work"))
            categoryDao.insertCategory(Category(name = "Business", type = "INCOME", color = 0xFF009688.toInt(), iconName = "Work"))
            categoryDao.insertCategory(Category(name = "Investment", type = "INCOME", color = 0xFF3F51B5.toInt(), iconName = "Star"))
            categoryDao.insertCategory(Category(name = "Gift", type = "INCOME", color = 0xFFE91E63.toInt(), iconName = "CardGiftcard"))
            categoryDao.insertCategory(Category(name = "Refunds", type = "INCOME", color = 0xFF00B0FF.toInt(), iconName = "Paid"))

            // Parent Expense categories
            val foodId = categoryDao.insertCategory(Category(name = "Food", type = "EXPENSE", color = 0xFFFF9800.toInt(), iconName = "Restaurant"))
            categoryDao.insertCategory(Category(name = "Rent", type = "EXPENSE", color = 0xFF795548.toInt(), iconName = "Home"))
            val utilitiesId = categoryDao.insertCategory(Category(name = "Utilities", type = "EXPENSE", color = 0xFF2196F3.toInt(), iconName = "Bolt"))
            val transportId = categoryDao.insertCategory(Category(name = "Transport", type = "EXPENSE", color = 0xFF03A9F4.toInt(), iconName = "DirectionsCar"))
            categoryDao.insertCategory(Category(name = "Shopping", type = "EXPENSE", color = 0xFFFF4081.toInt(), iconName = "ShoppingCart"))
            categoryDao.insertCategory(Category(name = "Entertainment", type = "EXPENSE", color = 0xFF9C27B0.toInt(), iconName = "Movie"))
            categoryDao.insertCategory(Category(name = "Health", type = "EXPENSE", color = 0xFFF44336.toInt(), iconName = "Favorite"))
            categoryDao.insertCategory(Category(name = "Education", type = "EXPENSE", color = 0xFF673AB7.toInt(), iconName = "School"))
            categoryDao.insertCategory(Category(name = "Others", type = "EXPENSE", color = 0xFF607D8B.toInt(), iconName = "Star"))

            // Subcategories
            categoryDao.insertCategory(Category(name = "Groceries", type = "EXPENSE", color = 0xFFD84315.toInt(), iconName = "ShoppingCart", parentId = foodId))
            categoryDao.insertCategory(Category(name = "Restaurants", type = "EXPENSE", color = 0xFFFF5722.toInt(), iconName = "Restaurant", parentId = foodId))
            categoryDao.insertCategory(Category(name = "Cafes", type = "EXPENSE", color = 0xFFFFC107.toInt(), iconName = "Restaurant", parentId = foodId))
            
            categoryDao.insertCategory(Category(name = "Fuel", type = "EXPENSE", color = 0xFF00BCD4.toInt(), iconName = "DirectionsCar", parentId = transportId))
            categoryDao.insertCategory(Category(name = "Public Transit", type = "EXPENSE", color = 0xFF009688.toInt(), iconName = "DirectionsCar", parentId = transportId))
            
            categoryDao.insertCategory(Category(name = "Electricity", type = "EXPENSE", color = 0xFFE91E63.toInt(), iconName = "Bolt", parentId = utilitiesId))
            categoryDao.insertCategory(Category(name = "Water & Gas", type = "EXPENSE", color = 0xFF3F51B5.toInt(), iconName = "Bolt", parentId = utilitiesId))

            // Adjustment categories
            categoryDao.insertCategory(Category(name = "Adjustment", type = "INCOME", color = 0xFF607D8B.toInt(), iconName = "Tune"))
            categoryDao.insertCategory(Category(name = "Adjustment", type = "EXPENSE", color = 0xFF607D8B.toInt(), iconName = "Tune"))
        }
    }

    suspend fun seedDemoDataForOnboarding(
        budgetBookName: String,
        monthlyBudget: Double,
        currencySymbol: String
    ) {
        // 1. Populate default categories
        populateDefaultCategoriesIfNeeded()

        // 2. Create budget book
        val budgetBookId = insertBudgetBook(
            BudgetBook(
                name = budgetBookName,
                monthlyLimit = monthlyBudget,
                currencyCode = currencySymbol,
                isActive = true,
                isDefault = true
            )
        )

        // 3. Mark Onboarding completed and set currency
        val currentSettings = getAppSettings() ?: AppSettings()
        updateAppSettings(
            currentSettings.copy(
                currencyCode = currencySymbol,
                hasCompletedOnboarding = true
            )
        )

        // 4. Create standard default accounts conditionally (irrespective of budget books)
        val existingAccounts = accountDao.getAllAccounts()
        if (existingAccounts.isEmpty()) {
            insertAccount(
                Account(
                    name = "Cash Wallet",
                    type = "CASH",
                    associatedBudgetBookId = budgetBookId,
                    isActive = true,
                    iconName = "Payments"
                )
            )

            insertAccount(
                Account(
                    name = "Savings Bank Account",
                    type = "BANK",
                    associatedBudgetBookId = budgetBookId,
                    isActive = true,
                    iconName = "AccountBalance"
                )
            )

            insertAccount(
                Account(
                    name = "Main Credit Card",
                    type = "CREDIT_CARD",
                    associatedBudgetBookId = budgetBookId,
                    billingCycleStartDay = 1,
                    billingCycleEndDay = 28,
                    isCreditCard = true,
                    isActive = true,
                    iconName = "CreditCard"
                )
            )
        }

        // 5. Seed some Alert defaults
        insertCategoryAlert(CategoryAlert("Food", 300.0, true))
        insertCategoryAlert(CategoryAlert("Shopping", 200.0, true))
    }

    suspend fun addGiftCard(
        brand: String,
        customBrandName: String?,
        code: String,
        amount: Double,
        purchaseDate: Long,
        validityDays: Int,
        note: String?,
        budgetBookId: Long
    ) {
        val brandLabel = if ((brand == "OTHER") && !customBrandName.isNullOrBlank()) customBrandName else GiftCardBrand.valueOf(brand).displayName
        val accountName = "$brandLabel Gift Card"
        
        val account = accountDao.getAccountByTypeAndName("GIFT_CARD", accountName)
        val accountId = account?.id ?: accountDao.insertAccount(
            Account(
                name = accountName,
                type = "GIFT_CARD",
                associatedBudgetBookId = budgetBookId,
                isActive = true,
                iconName = "CardGiftcard",
                initialBalance = 0.0,
                currentBalance = 0.0
            )
        )

        // 1. Insert Gift Card first (with null entryTransactionId)
        val giftCardId = giftCardDao.insert(
            GiftCard(
                linkedAccountId = accountId,
                brand = brand,
                customBrandName = customBrandName,
                code = code,
                amount = amount,
                purchaseDate = purchaseDate,
                validityDays = validityDays,
                note = note
            )
        )
        
        // 2. Insert Income Transaction linked to the Gift Card
        val txId = transactionDao.insertTransaction(
            Transaction(
                amount = amount,
                type = "INCOME",
                category = "Gift",
                dateTime = purchaseDate,
                linkedAccountId = accountId,
                linkedBudgetBookId = budgetBookId,
                description = "Added Gift Card: $brandLabel ($code)",
                linkedGiftCardId = giftCardId
            )
        )

        // 3. Update Gift Card with the entryTransactionId
        giftCardDao.update(
            GiftCard(
                id = giftCardId,
                linkedAccountId = accountId,
                brand = brand,
                customBrandName = customBrandName,
                code = code,
                amount = amount,
                purchaseDate = purchaseDate,
                validityDays = validityDays,
                note = note,
                entryTransactionId = txId
            )
        )
    }

    suspend fun markGiftCardUsed(giftCardId: Long) {
        val card = giftCardDao.getById(giftCardId) ?: return
        if (card.isUsed) return
        giftCardDao.markUsed(giftCardId, System.currentTimeMillis())
        // No direct balance adjustment here, assuming a transfer transaction handles it
    }

    suspend fun markGiftCardUnused(giftCardId: Long) {
        val card = giftCardDao.getById(giftCardId) ?: return
        if (!card.isUsed) return
        giftCardDao.markUnused(giftCardId)
        // If we want to delete the transfer transaction too, we'd need its ID
    }

    suspend fun updateGiftCard(card: GiftCard) {
        val oldCard = giftCardDao.getById(card.id) ?: return
        giftCardDao.update(card)
        
        // Update the linked entry transaction if details changed
        if (oldCard.amount != card.amount || oldCard.purchaseDate != card.purchaseDate) {
            card.entryTransactionId?.let { txId ->
                transactionDao.getTransactionById(txId)?.let { tx ->
                    transactionDao.updateTransaction(
                        tx.copy(
                            amount = card.amount,
                            dateTime = card.purchaseDate
                        )
                    )
                }
            }
        }
    }

    suspend fun deleteGiftCard(card: GiftCard) {
        giftCardDao.delete(card)
        // Delete the entry transaction
        card.entryTransactionId?.let { txId ->
            transactionDao.deleteTransactionById(txId)
        }
    }

    // Template actions
    suspend fun insertTemplate(template: TransactionTemplate): Long {
        return transactionTemplateDao.insertTemplate(template)
    }

    suspend fun updateTemplate(template: TransactionTemplate) {
        transactionTemplateDao.updateTemplate(template)
    }

    suspend fun updateTemplates(templates: List<TransactionTemplate>) {
        transactionTemplateDao.updateTemplates(templates)
    }

    suspend fun deleteTemplate(template: TransactionTemplate) {
        transactionTemplateDao.deleteTemplate(template)
    }

    suspend fun getGiftCardsForTransaction(txId: Long): List<GiftCard> = withContext(Dispatchers.IO) {
        giftCardDao.getByEntryTransactionId(txId)
    }

    suspend fun addTransferWithGiftCards(
        amount: Double,
        dateTimeMs: Long,
        fromAccountId: Long,
        toAccountId: Long,
        budgetBookId: Long,
        description: String,
        giftCardItems: List<com.saik.expensemanager.data.model.GiftCardData>
    ): Long = withContext(Dispatchers.IO) {
        val txId = transactionDao.insertTransaction(
            Transaction(
                amount = amount,
                type = "TRANSFER",
                category = "",
                dateTime = dateTimeMs,
                linkedAccountId = fromAccountId,
                toAccountId = toAccountId,
                linkedBudgetBookId = budgetBookId,
                description = description
            )
        )
        giftCardItems.forEach { item ->
            giftCardDao.insert(
                GiftCard(
                    linkedAccountId = toAccountId,
                    brand = item.brand,
                    customBrandName = item.customBrandName,
                    code = item.code,
                    amount = item.amount,
                    purchaseDate = dateTimeMs,
                    validityDays = item.validityDays,
                    note = item.note,
                    entryTransactionId = txId
                )
            )
        }
        txId
    }

    suspend fun updateTransferWithGiftCards(
        transaction: Transaction,
        giftCardItems: List<com.saik.expensemanager.data.model.GiftCardData>
    ) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
        giftCardDao.deleteByEntryTransactionId(transaction.id)
        val toAccId = transaction.toAccountId ?: return@withContext
        giftCardItems.forEach { item ->
            giftCardDao.insert(
                GiftCard(
                    linkedAccountId = toAccId,
                    brand = item.brand,
                    customBrandName = item.customBrandName,
                    code = item.code,
                    amount = item.amount,
                    purchaseDate = transaction.dateTime,
                    validityDays = item.validityDays,
                    note = item.note,
                    entryTransactionId = transaction.id
                )
            )
        }
    }
}
