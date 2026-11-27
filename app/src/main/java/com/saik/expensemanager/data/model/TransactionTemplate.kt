package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val category: String,
    val subCategory: String? = null,
    val linkedAccountId: Long,
    val toAccountId: Long? = null,
    val linkedBudgetBookId: Long,
    val description: String = "",
    val iconName: String = "Star",
    val emojiValue: String? = null,
    val colorHex: String? = null,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
