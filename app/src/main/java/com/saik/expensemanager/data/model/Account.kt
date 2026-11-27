package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "CASH", "WALLET", "CREDIT_CARD", "BANK", "GIFT_CARD"
    val associatedBudgetBookId: Long,
    val billingCycleStartDay: Int = 1, // 1 to 31
    val billingCycleEndDay: Int = 28, // 1 to 31
    val isCreditCard: Boolean = false,
    
    // Credit Card Fee Tracking
    val trackFeeWaiver: Boolean = false,
    val annualFeeAmount: Double? = null,
    val feeWaiverThreshold: Double? = null,
    val feeCycleStartDate: Long? = null,
    val feeCycleType: String? = null, // e.g., "YEARLY"
    val feeThresholdEnabled: Boolean = false,

    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val isActive: Boolean = true,
    val isClosed: Boolean = false,
    val iconName: String = "AccountBalanceWallet",
    val displayOrder: Int = 0
)
