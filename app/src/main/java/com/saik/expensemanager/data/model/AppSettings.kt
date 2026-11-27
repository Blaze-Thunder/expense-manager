package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val currencyCode: String = "₹", // Default selecion
    val theme: String = "SYSTEM", // "DARK", "LIGHT", "SYSTEM"
    val colorTheme: String = "WARM_CLAY", // "WARM_CLAY", "DEEP_BLUE", "FOREST_GREEN", "ROYAL_PURPLE"
    val language: String = "English",
    val budgetAlertThreshold: Double = 0.80, // Default 80% threshold
    val cloudBackupEnabled: Boolean = false,
    val cloudBackupOnMetered: Boolean = false,
    val localBackupEnabled: Boolean = false,
    val backupFolderUri: String? = null,
    val lastBackupTime: Long = 0,
    val lastLocalBackupTime: Long = 0,
    val autoBackupFrequencyDays: Int = 1, // 0 means disabled
    val googleAccountEmail: String? = null,
    val hasCompletedOnboarding: Boolean = false,
    val skipExitConfirmation: Boolean = false,
    val accountTypeOrder: String = "CASH,WALLET,BANK,CREDIT_CARD,GIFT_CARD",
    val homeAccountCategories: String = "CASH,WALLET,BANK,CREDIT_CARD,GIFT_CARD",
    val appLockEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val logTemplatesInstantly: Boolean = false
)
