package com.ampairs.common.format

actual fun Double?.toDecimal(): String {
    val value = this ?: return ""
    val toInt = value.toInt()
    return if (toInt.toDouble() != value) {
        val wholePart = value.toLong()
        val fractional = ((value - wholePart) * 100).toInt()
        "$wholePart.${fractional.toString().padStart(2, '0')}"
    } else {
        toInt.toString()
    }
}