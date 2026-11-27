package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repetitive_transactions")
data class RepetitiveTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", or "TRANSFER"
    val category: String,
    val subCategory: String? = null,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val interval: Int = 1,
    val linkedAccountId: Long,
    val toAccountId: Long? = null,
    val linkedBudgetBookId: Long,
    val description: String = "",
    val endDate: Long? = null,
    val occurrences: Int? = null,
    val dayOfWeek: Int? = null, // Calendar.SUNDAY etc.
    val dayOfMonth: Int? = null,
    val lastGeneratedDate: Long? = null,
    val reflectionTiming: String = "ON_DATE", // "ON_DATE" or "FIRST_OF_MONTH"
    val linkedGiftCardId: Long? = null,
    val isCreditCardPayment: Boolean = false
)
