package com.ampairs.common.locale

import kotlin.math.abs
import kotlin.math.round

/**
 * Currency symbol for an ISO 4217 code. Unknown codes fall back to "<CODE> " so the amount is
 * still unambiguous (e.g. "ZAR 1,234.00").
 */
fun currencySymbol(code: String): String = when (code.trim().uppercase()) {
    "INR" -> "₹"
    "USD", "AUD", "CAD", "SGD", "NZD", "HKD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY", "CNY" -> "¥"
    else -> code.trim().uppercase() + " "
}

/**
 * Format a money amount using the workspace currency. INR uses Indian digit grouping (mirrors the
 * legacy `toInr()`: `920710.5` → `₹9,20,710.50`); other currencies use thousands grouping. Always
 * two decimals. Returns "" for a null amount.
 */
fun formatMoney(amount: Double?, locale: AppLocale): String =
    formatMoney(amount, locale.currencyCode)

fun formatMoney(amount: Double?, currencyCode: String): String {
    if (amount == null) return ""
    val code = currencyCode.ifBlank { "INR" }
    val symbol = currencySymbol(code)
    val negative = amount < 0.0
    val cents = round(abs(amount) * 100).toLong()
    val whole = (cents / 100).toString()
    val fraction = (cents % 100).toString().padStart(2, '0')
    val grouped = if (code.equals("INR", ignoreCase = true)) groupIndian(whole) else groupThousands(whole)
    return (if (negative) "-" else "") + symbol + grouped + "." + fraction
}

/** Indian grouping: last three digits, then groups of two (`9,20,710`). */
private fun groupIndian(whole: String): String {
    if (whole.length <= 3) return whole
    val last3 = whole.takeLast(3)
    val rest = whole.dropLast(3)
    val parts = ArrayDeque<String>()
    var i = rest.length
    while (i > 0) {
        val start = maxOf(0, i - 2)
        parts.addFirst(rest.substring(start, i))
        i = start
    }
    return parts.joinToString(",") + "," + last3
}

/** Western grouping: groups of three (`1,234,567`). */
private fun groupThousands(whole: String): String {
    if (whole.length <= 3) return whole
    val parts = ArrayDeque<String>()
    var i = whole.length
    while (i > 0) {
        val start = maxOf(0, i - 3)
        parts.addFirst(whole.substring(start, i))
        i = start
    }
    return parts.joinToString(",")
}
