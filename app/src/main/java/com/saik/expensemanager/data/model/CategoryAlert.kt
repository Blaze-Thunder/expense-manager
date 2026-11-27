package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_alerts")
data class CategoryAlert(
    @PrimaryKey val categoryName: String,
    val limitAmount: Double,
    val isEnabled: Boolean = true
)
