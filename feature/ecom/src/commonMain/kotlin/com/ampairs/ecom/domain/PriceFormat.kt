package com.ampairs.ecom.domain

import kotlin.math.roundToLong

/**
 * KMP-safe currency formatting (no `String.format` / `DecimalFormat`).
 * Renders a rupee amount with two decimals, e.g. 499.0 → "₹499.00".
 */
fun Double.asRupee(): String {
    val cents = (this * 100).roundToLong()
    val whole = cents / 100
    val frac = (if (cents < 0) -cents else cents) % 100
    val fracStr = if (frac < 10) "0$frac" else "$frac"
    return "₹$whole.$fracStr"
}

/** Savings = mrp − price, only when there is a positive saving. */
fun savingsOrNull(price: Double, mrp: Double?): Double? =
    if (mrp != null && mrp > price) mrp - price else null
