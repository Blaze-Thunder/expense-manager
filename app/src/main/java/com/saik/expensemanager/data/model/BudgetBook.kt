package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_books")
data class BudgetBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val monthlyLimit: Double? = null,
    val currencyCode: String, // e.g. "₹", "$", "€", "£"
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val isClosed: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null
)
