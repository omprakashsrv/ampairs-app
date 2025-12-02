package com.ampairs.common.util

/**
 * KMP-compatible string formatting utilities
 */

/**
 * Format a double value to 2 decimal places
 * KMP-compatible alternative to String.format()
 */
fun Double.formatDecimal(decimals: Int = 2): String {
    val wholePart = this.toLong()
    val fractionalPart = ((this - wholePart) * when (decimals) {
        1 -> 10
        2 -> 100
        3 -> 1000
        else -> 100
    }).toInt().toString().padStart(decimals, '0')

    return "$wholePart.$fractionalPart"
}

/**
 * Format currency amount with symbol
 */
fun Double.formatCurrency(symbol: String = "₹", decimals: Int = 2): String {
    return "$symbol${this.formatDecimal(decimals)}"
}

/**
 * Format currency amount with currency code
 * Uses currency code (INR, USD, etc.) to determine symbol
 */
fun formatCurrencyWithCode(amount: Double, currencyCode: String, decimals: Int = 2): String {
    val symbol = when (currencyCode.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> currencyCode
    }
    return amount.formatCurrency(symbol, decimals)
}
