package com.saik.expensemanager.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    
    /**
     * Formats an amount with the given currency symbol.
     * If the symbol is '₹', it uses the Indian numbering system (Lakhs, Crores).
     * Otherwise, it uses the standard grouping (Thousands).
     */
    fun formatAmount(amount: Double, currencySymbol: String, includeSymbol: Boolean = true): String {
        val cleanAmount = if (kotlin.math.abs(amount) < 0.001) 0.0 else amount
        val isNegative = cleanAmount < 0
        val absAmount = kotlin.math.abs(cleanAmount)
        val formattedNumber = if (currencySymbol == "₹") {
            formatIndianNumber(absAmount)
        } else {
            formatStandardNumber(absAmount)
        }
        
        return if (includeSymbol) {
            if (isNegative) "-$currencySymbol$formattedNumber" else "$currencySymbol$formattedNumber"
        } else {
            if (isNegative) "-$formattedNumber" else formattedNumber
        }
    }

    /**
     * Formats an amount with sign and currency symbol.
     * Example: +₹1,00,000.00 or -₹1,00,000.00
     */
    fun formatAmountWithSign(amount: Double, currencySymbol: String, type: String? = null): String {
        val cleanAmount = if (kotlin.math.abs(amount) < 0.001) 0.0 else amount
        val absAmount = kotlin.math.abs(cleanAmount)
        val sign = when {
            type == "INCOME" -> "+"
            type == "EXPENSE" -> "-"
            type == "TRANSFER" -> ""
            cleanAmount > 0 -> "+"
            cleanAmount < 0 -> "-"
            else -> ""
        }
        
        return "$sign${formatAmount(absAmount, currencySymbol)}"
    }

    private fun formatIndianNumber(amount: Double): String {
        return try {
            // Locale("en", "IN") is deprecated in some Java versions, forLanguageTag is safer
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
            if (formatter is DecimalFormat) {
                formatter.minimumFractionDigits = 2
                formatter.maximumFractionDigits = 2
            }
            formatter.format(amount)
        } catch (e: Exception) {
            formatStandardNumber(amount)
        }
    }

    private fun formatStandardNumber(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        if (formatter is DecimalFormat) {
            formatter.minimumFractionDigits = 2
            formatter.maximumFractionDigits = 2
        }
        return formatter.format(amount)
    }
}
