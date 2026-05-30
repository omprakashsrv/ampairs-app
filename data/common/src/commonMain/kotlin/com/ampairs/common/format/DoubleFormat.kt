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