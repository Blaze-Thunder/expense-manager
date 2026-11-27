package com.saik.expensemanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saik.expensemanager.data.dao.*
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.AppSettings
import com.saik.expensemanager.data.model.CategoryAlert
import com.saik.expensemanager.data.model.GiftCard
import com.saik.expensemanager.data.model.RepetitiveTransaction
import com.saik.expensemanager.data.model.TransactionTemplate

@Database(
    entities = [
        Category::class,
        BudgetBook::class,
        Account::class,
        Transaction::class,
        AppSettings::class,
        CategoryAlert::class,
        RepetitiveTransaction::class,
        GiftCard::class,
        TransactionTemplate::class,
    ],
    version = 28,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetBookDao(): BudgetBookDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun categoryAlertDao(): CategoryAlertDao
    abstract fun repetitiveTransactionDao(): RepetitiveTransactionDao
    abstract fun giftCardDao(): GiftCardDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_linkedAccountId` ON `transactions` (`linkedAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_linkedBudgetBookId` ON `transactions` (`linkedBudgetBookId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_dateTime` ON `transactions` (`dateTime`)")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN homeAccountCategories TEXT NOT NULL DEFAULT 'CASH,WALLET,BANK,CREDIT_CARD,GIFT_CARD'")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN logTemplatesInstantly INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `subCategory` TEXT, 
                        `linkedAccountId` INTEGER NOT NULL, 
                        `toAccountId` INTEGER, 
                        `linkedBudgetBookId` INTEGER NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `iconName` TEXT NOT NULL, 
                        `emojiValue` TEXT, 
                        `colorHex` TEXT, 
                        `displayOrder` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Account changes
                db.execSQL("ALTER TABLE accounts ADD COLUMN trackFeeWaiver INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN annualFeeAmount REAL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN feeWaiverThreshold REAL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN feeCycleStartDate INTEGER")
                db.execSQL("ALTER TABLE accounts ADD COLUMN feeCycleType TEXT")
                db.execSQL("ALTER TABLE accounts ADD COLUMN feeThresholdEnabled INTEGER NOT NULL DEFAULT 0")

                // Transaction changes
                db.execSQL("ALTER TABLE transactions ADD COLUMN isCreditCardPayment INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE repetitive_transactions ADD COLUMN isCreditCardPayment INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budget_books_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `monthlyLimit` REAL, 
                        `currencyCode` TEXT NOT NULL, 
                        `isActive` INTEGER NOT NULL, 
                        `isDefault` INTEGER NOT NULL, 
                        `isClosed` INTEGER NOT NULL, 
                        `startDate` INTEGER, 
                        `endDate` INTEGER
                    )
                """.trimIndent()
                )

                // 2. Copy the data
                db.execSQL(
                    """
                    INSERT INTO `budget_books_new` (`id`, `name`, `monthlyLimit`, `currencyCode`, `isActive`, `isDefault`, `isClosed`, `startDate`, `endDate`)
                    SELECT `id`, `name`, `monthlyBudgetAmount`, `currencyCode`, `isActive`, `isDefault`, `isClosed`, `startDate`, `endDate` FROM `budget_books`
                """.trimIndent()
                )

                // 3. Remove the old table
                db.execSQL("DROP TABLE `budget_books`")

                // 4. Rename the new table
                db.execSQL("ALTER TABLE `budget_books_new` RENAME TO `budget_books`")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budget_books ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE budget_books ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE budget_books ADD COLUMN startDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE budget_books ADD COLUMN endDate INTEGER DEFAULT NULL")

                // Data fix: Set first book as default
                db.execSQL("UPDATE budget_books SET isDefault = 1 WHERE id = (SELECT id FROM budget_books ORDER BY id ASC LIMIT 1)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN accountTypeOrder TEXT NOT NULL DEFAULT 'CASH,WALLET,BANK,CREDIT_CARD'")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN subCategory TEXT")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE repetitive_transactions ADD COLUMN subCategory TEXT")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN lastLocalBackupTime INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `gift_cards`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gift_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `linkedAccountId` INTEGER NOT NULL, 
                        `brand` TEXT NOT NULL, 
                        `customBrandName` TEXT, 
                        `code` TEXT NOT NULL, 
                        `amount` REAL NOT NULL, 
                        `purchaseDate` INTEGER NOT NULL, 
                        `validityDays` INTEGER NOT NULL, 
                        `isUsed` INTEGER NOT NULL, 
                        `usedDate` INTEGER, 
                        `note` TEXT, 
                        FOREIGN KEY(`linkedAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gift_cards_linkedAccountId` ON `gift_cards` (`linkedAccountId`)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to accounts table
                db.execSQL("ALTER TABLE accounts ADD COLUMN initialBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN currentBalance REAL NOT NULL DEFAULT 0.0")

                // 2. Perform pooled account migration for Gift Cards
                val giftCardsCursor = db.query("SELECT * FROM gift_cards")
                val giftCards = mutableListOf<Map<String, Any?>>()
                while (giftCardsCursor.moveToNext()) {
                    val map = mutableMapOf<String, Any?>()
                    for (i in 0 until giftCardsCursor.columnCount) {
                        val name = giftCardsCursor.getColumnName(i)
                        map[name] = when (giftCardsCursor.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> giftCardsCursor.getLong(i)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> giftCardsCursor.getDouble(i)
                            android.database.Cursor.FIELD_TYPE_STRING -> giftCardsCursor.getString(i)
                            else -> null
                        }
                    }
                    giftCards.add(map)
                }
                giftCardsCursor.close()

                if (giftCards.isEmpty()) return

                // Group by brand
                val grouped = giftCards.groupBy { card ->
                    val brand = card["brand"] as String
                    val customBrandName = card["customBrandName"] as? String
                    if ((brand == "OTHER") && !customBrandName.isNullOrBlank()) customBrandName else brand
                }

                grouped.forEach { (brandLabelRaw, cards) ->
                    val brandLabel = when (brandLabelRaw) {
                        "AMAZON_PAY" -> "Amazon Pay"
                        "FLIPKART" -> "Flipkart"
                        "MYNTRA" -> "Myntra"
                        "GOOGLE_PLAY" -> "Google Play"
                        "PAYTM" -> "Paytm"
                        "OTHER" -> "Other"
                        else -> brandLabelRaw
                    }
                    
                    val accountName = "$brandLabel Gift Card"
                    
                    // Pick the first account to keep
                    val firstLinkedAccountId = cards.first()["linkedAccountId"] as Long
                    
                    // Sum of unused cards
                    val currentBalance = cards.asSequence().filter { (it["isUsed"] as Long) == 0L }.sumOf { it["amount"] as Double }
                    
                    // Update the kept account
                    db.execSQL("UPDATE accounts SET name = ?, currentBalance = ? WHERE id = ?", arrayOf<Any>(accountName, currentBalance, firstLinkedAccountId))
                    
                    // Update all gift cards in this group to point to the kept account
                    db.execSQL("UPDATE gift_cards SET linkedAccountId = ? WHERE id IN (${cards.joinToString { it["id"].toString() }})", arrayOf<Any>(firstLinkedAccountId))
                    
                    // Identify other accounts to delete
                    val otherAccountIds = cards.map { it["linkedAccountId"] as Long }.distinct().filter { it != firstLinkedAccountId }
                    if (otherAccountIds.isNotEmpty()) {
                        db.execSQL("DELETE FROM accounts WHERE id IN (${otherAccountIds.joinToString()})")
                    }
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gift_cards ADD COLUMN entryTransactionId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN linkedGiftCardId INTEGER")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // In case 15->16 failed or was skipped
                try {
                    db.execSQL("ALTER TABLE gift_cards ADD COLUMN entryTransactionId INTEGER")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN linkedGiftCardId INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN appLockEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN biometricLockEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN pinHash TEXT")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN pinSalt TEXT")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE repetitive_transactions ADD COLUMN linkedGiftCardId INTEGER")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN cloudBackupOnMetered INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_db"
                )
                .setJournalMode(JournalMode.TRUNCATE)
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun checkpoint(context: Context) {
            val db = getDatabase(context)
            val supportDb = db.openHelper.writableDatabase
            supportDb.query("PRAGMA wal_checkpoint(FULL)").close()
        }
    }
}
