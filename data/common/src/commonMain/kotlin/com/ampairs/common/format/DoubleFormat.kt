package com.ampairs.common.format

import kotlin.math.abs
import kotlin.math.round

fun Double?.toDecimal(): String {
    if (this == null) return ""
    val asInt = toInt()
    if (asInt.toDouble() == this) return asInt.toString()
    val rounded = round(this * 100).toLong()
    val wholePart = rounded / 100
    val fractional = abs(rounded % 100)
    return "$wholePart.${fractional.toString().padStart(2, '0')}"
}


fun Double.toNumber(): String {
    return this.toInt().toString()
}

/**
 * Format a money amount as ₹ with Indian digit grouping and exactly two decimals,
 * e.g. 920710.5 → "₹9,20,710.50". Used by the order/invoice editor (spec 010 v2 design):
 * the last three digits group together, then groups of two.
 */
fun Double?.toInr(): String {
    if (this == null) return ""
    val negative = this < 0.0
    val paise = round(abs(this) * 100).toLong()
    val whole = (paise / 100).toString()
    val fraction = (paise % 100).toString().padStart(2, '0')
    val grouped = if (whole.length <= 3) whole else {
        val last3 = whole.takeLast(3)
        val rest = whole.dropLast(3)
        val parts = ArrayDeque<String>()
        var i = rest.length
        while (i > 0) {
            val start = maxOf(0, i - 2)
            parts.addFirst(rest.substring(start, i))
            i = start
        }
        parts.joinToString(",") + "," + last3
    }
    return (if (negative) "-₹" else "₹") + grouped + "." + fraction
}