package com.saik.expensemanager.data.dao

import androidx.room.*
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.AppSettings
import com.saik.expensemanager.data.model.CategoryAlert
import com.saik.expensemanager.data.model.RepetitiveTransaction
import com.saik.expensemanager.data.model.TransactionTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {
    @Query("SELECT * FROM transaction_templates ORDER BY displayOrder ASC")
    fun getAllTemplatesFlow(): Flow<List<TransactionTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TransactionTemplate): Long

    @Update
    suspend fun updateTemplate(template: TransactionTemplate)

    @Update
    suspend fun updateTemplates(templates: List<TransactionTemplate>)

    @Delete
    suspend fun deleteTemplate(template: TransactionTemplate)
}

@Dao
interface BudgetBookDao {
    @Query("SELECT * FROM budget_books")
    fun getAllBudgetBooksFlow(): Flow<List<BudgetBook>>

    @Query("SELECT * FROM budget_books")
    suspend fun getAllBudgetBooks(): List<BudgetBook>

    @Query("SELECT * FROM budget_books WHERE id = :id")
    suspend fun getBudgetBookById(id: Long): BudgetBook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetBook(budgetBook: BudgetBook): Long

    @Update
    suspend fun updateBudgetBook(budgetBook: BudgetBook)

    @androidx.room.Transaction
    suspend fun setDefaultBook(bookId: Long) {
        clearDefault()
        setAsDefault(bookId)
    }

    @Query("UPDATE budget_books SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE budget_books SET isDefault = 1 WHERE id = :bookId")
    suspend fun setAsDefault(bookId: Long)

    @Delete
    suspend fun deleteBudgetBook(budgetBook: BudgetBook)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY displayOrder ASC")
    fun getAllAccountsFlow(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY displayOrder ASC")
    suspend fun getAllAccounts(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE type = :type AND name = :name LIMIT 1")
    suspend fun getAccountByTypeAndName(type: String, name: String): Account?

    @Query("SELECT * FROM accounts WHERE associatedBudgetBookId = :budgetBookId ORDER BY displayOrder ASC")
    fun getAccountsForBudgetBookFlow(budgetBookId: Long): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE associatedBudgetBookId = :budgetBookId ORDER BY displayOrder ASC")
    suspend fun getAccountsForBudgetBook(budgetBookId: Long): List<Account>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Query("UPDATE accounts SET currentBalance = currentBalance + :delta WHERE id = :accountId")
    suspend fun adjustBalance(accountId: Long, delta: Double)

    @Update
    suspend fun updateAccount(account: Account)

    @Update
    suspend fun updateAccounts(accounts: List<Account>)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("""
        SELECT 
            a.id as cardId, 
            a.name as cardName, 
            a.feeWaiverThreshold as threshold,
            a.feeCycleStartDate as feeCycleStartDate,
            COALESCE(SUM(CASE 
                WHEN t.type = 'EXPENSE' AND t.description != 'Balance Correction' AND t.category != 'Adjustment' 
                     AND t.dateTime >= a.feeCycleStartDate AND t.dateTime < (a.feeCycleStartDate + 31536000000) 
                THEN t.amount 
                WHEN t.type = 'INCOME' AND (t.category = 'Refund' OR t.category = 'Refunds')
                     AND t.dateTime >= a.feeCycleStartDate AND t.dateTime < (a.feeCycleStartDate + 31536000000)
                THEN -t.amount 
                ELSE 0 
            END), 0) as currentSpend
        FROM accounts a
        LEFT JOIN transactions t ON a.id = t.linkedAccountId
        WHERE a.isCreditCard = 1 AND a.trackFeeWaiver = 1 AND a.feeWaiverThreshold IS NOT NULL AND a.feeCycleStartDate IS NOT NULL
        GROUP BY a.id
        HAVING currentSpend < threshold
    """)
    fun getPendingFeeWaiverAlertsFlow(): Flow<List<com.saik.expensemanager.data.model.CardSpendStatusRaw>>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    data class GeneratedKey(val repetitiveId: Long, val repetitiveOccurrenceDate: Long)

    @Query("SELECT * FROM transactions WHERE linkedBudgetBookId = :budgetBookId ORDER BY dateTime DESC")
    fun getTransactionsForBudgetBookFlow(budgetBookId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE linkedAccountId = :accountId ORDER BY dateTime DESC")
    fun getTransactionsForAccountFlow(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE linkedAccountId = :accountId ORDER BY dateTime DESC")
    suspend fun getTransactionsForAccount(accountId: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE linkedAccountId = :accountId OR toAccountId = :accountId ORDER BY dateTime DESC")
    suspend fun getTransactionsInvolvingAccount(accountId: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT repetitiveId, repetitiveOccurrenceDate FROM transactions WHERE repetitiveId IS NOT NULL AND repetitiveOccurrenceDate IS NOT NULL")
    suspend fun getGeneratedTransactionKeys(): List<GeneratedKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName AND type = :type")
    suspend fun updateCategoryName(oldName: String, newName: String, type: String)

    @Query("UPDATE transactions SET subCategory = :newName WHERE subCategory = :oldName AND type = :type")
    suspend fun updateSubCategoryName(oldName: String, newName: String, type: String)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE isGeneratedFromRecurrence = 1 AND dateTime > :now")
    suspend fun deleteFutureGeneratedTransactions(now: Long)

    @Query("DELETE FROM transactions WHERE isGeneratedFromRecurrence = 1 AND repetitiveId = :repId AND dateTime >= :now")
    suspend fun deleteFutureGeneratedTransactionsForRep(repId: Long, now: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getAppSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettings(): AppSettings?

    @Query("UPDATE app_settings SET lastBackupTime = :time WHERE id = 1")
    suspend fun updateLastBackupTime(time: Long)

    @Query("UPDATE app_settings SET lastLocalBackupTime = :time WHERE id = 1")
    suspend fun updateLastLocalBackupTime(time: Long)

    @Query("UPDATE app_settings SET backupFolderUri = NULL WHERE id = 1")
    suspend fun clearBackupFolderUri()

    @Query("UPDATE app_settings SET pinHash = :hash, pinSalt = :salt WHERE id = 1")
    suspend fun updatePin(hash: String?, salt: String?)

    @Query("UPDATE app_settings SET appLockEnabled = :enabled WHERE id = 1")
    suspend fun updateAppLockEnabled(enabled: Boolean)

    @Query("UPDATE app_settings SET biometricLockEnabled = :enabled WHERE id = 1")
    suspend fun updateBiometricEnabled(enabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettings)
}

@Dao
interface CategoryAlertDao {
    @Query("SELECT * FROM category_alerts")
    fun getAllAlertsFlow(): Flow<List<CategoryAlert>>

    @Query("SELECT * FROM category_alerts")
    suspend fun getAllAlerts(): List<CategoryAlert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: CategoryAlert)

    @Query("UPDATE category_alerts SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)

    @Delete
    suspend fun deleteAlert(alert: CategoryAlert)
}

@Dao
interface RepetitiveTransactionDao {
    @Query("SELECT * FROM repetitive_transactions")
    fun getAllRepetitiveTransactionsFlow(): Flow<List<RepetitiveTransaction>>

    @Query("SELECT * FROM repetitive_transactions")
    suspend fun getAllRepetitiveTransactions(): List<RepetitiveTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepetitiveTransaction(rep: RepetitiveTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepetitiveTransactions(reps: List<RepetitiveTransaction>)

    @Update
    suspend fun updateRepetitiveTransaction(rep: RepetitiveTransaction)

    @Query("UPDATE repetitive_transactions SET category = :newName WHERE category = :oldName AND type = :type")
    suspend fun updateCategoryName(oldName: String, newName: String, type: String)

    @Query("UPDATE repetitive_transactions SET subCategory = :newName WHERE subCategory = :oldName AND type = :type")
    suspend fun updateSubCategoryName(oldName: String, newName: String, type: String)

    @Delete
    suspend fun deleteRepetitiveTransaction(rep: RepetitiveTransaction)
}

