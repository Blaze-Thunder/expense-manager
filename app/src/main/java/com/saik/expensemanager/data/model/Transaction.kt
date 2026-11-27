package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("linkedAccountId"),
        Index("toAccountId"),
        Index("linkedBudgetBookId"),
        Index("dateTime"),
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTime: Long, // Epoch millisecond
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", or "TRANSFER"
    val category: String, // Named primary category
    val subCategory: String? = null, // Optional subcategory
    val description: String = "",
    val linkedAccountId: Long,
    val toAccountId: Long? = null, // Used for TRANSFER type
    val linkedBudgetBookId: Long,
    val repetitiveId: Long? = null,
    val repetitiveOccurrenceDate: Long? = null,
    val isGeneratedFromRecurrence: Boolean = false,
    val linkedGiftCardId: Long? = null,
    val isCreditCardPayment: Boolean = false
)
