package com.ampairs.common.format

actual fun Double?.toDecimal(): String {
    val toInt = this?.toInt()
    return if (toInt?.toDouble() != this) {
        String.format("%.2f", this)
    } else {
        toInt.toString()
    }
}