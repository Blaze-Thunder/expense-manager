package com.saik.expensemanager.data.model

data class CardSpendStatus(
    val cardId: Long,
    val cardName: String,
    val threshold: Double,
    val currentSpend: Double,
    val daysLeftInCycle: Int?
)

data class CardSpendStatusRaw(
    val cardId: Long,
    val cardName: String,
    val threshold: Double,
    val feeCycleStartDate: Long,
    val currentSpend: Double
)
