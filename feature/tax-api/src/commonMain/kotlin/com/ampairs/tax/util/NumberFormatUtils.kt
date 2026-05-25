package com.ampairs.tax.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

fun Double.formatDecimal(decimals: Int = 2): String {
    val factor = 10.0.pow(decimals).toLong()
    val scaled = round(this * factor).toLong()
    val intPart = scaled / factor
    val fracPart = abs(scaled % factor)
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}
