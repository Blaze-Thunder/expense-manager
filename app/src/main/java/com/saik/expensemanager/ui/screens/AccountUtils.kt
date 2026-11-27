package com.saik.expensemanager.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

object AccountUtils {
    fun formatAccountType(type: String): String {
        return type.split('_')
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }

    fun getAccountIcon(name: String): ImageVector {
        return when (name) {
            "AccountBalance" -> Icons.Default.AccountBalance
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "Payments" -> Icons.Default.Payments
            "CreditCard" -> Icons.Default.CreditCard
            "Savings" -> Icons.Default.Savings
            "MonetizationOn" -> Icons.Default.MonetizationOn
            "Wallet" -> Icons.Default.Wallet
            "Paid" -> Icons.Default.Paid
            "LocalAtm" -> Icons.Default.LocalAtm
            "AttachMoney" -> Icons.Default.AttachMoney
            "CurrencyExchange" -> Icons.Default.CurrencyExchange
            "PriceChange" -> Icons.Default.PriceChange
            "Money" -> Icons.Default.Money
            else -> Icons.Default.AccountBalanceWallet
        }
    }

    val iconList = listOf(
        "Payments",
        "Wallet",
        "AccountBalanceWallet",
        "LocalAtm",
        "Money",
        "AttachMoney",
        "AccountBalance",
        "CreditCard",
        "Savings",
        "Paid",
        "MonetizationOn",
        "CurrencyExchange",
        "PriceChange"
    )
}
