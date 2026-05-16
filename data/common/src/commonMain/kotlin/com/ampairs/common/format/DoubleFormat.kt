package com.ampairs.common.format


expect fun Double?.toDecimal(): String


fun Double.toNumber(): String {
    return this.toInt().toString()
}