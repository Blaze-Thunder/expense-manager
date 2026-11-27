package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val color: Int, // ARGB Int
    val parentId: Long? = null,
    val iconName: String = "Label"
)
