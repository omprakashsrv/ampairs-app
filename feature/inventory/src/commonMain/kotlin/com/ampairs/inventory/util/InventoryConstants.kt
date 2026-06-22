package com.ampairs.inventory.util

object InventoryConstants {
    const val ITEM_UID_PREFIX = "INV"
    const val TXN_UID_PREFIX = "TXN"

    // Movement types (match backend Constants.TXN_TYPE_*)
    const val TXN_TYPE_STOCK_IN = "STOCK_IN"
    const val TXN_TYPE_STOCK_OUT = "STOCK_OUT"
    const val TXN_TYPE_ADJUSTMENT = "ADJUSTMENT"
    const val TXN_TYPE_COUNT = "COUNT"

    // Movement reasons (match backend Constants.REASON_*)
    const val REASON_PURCHASE = "PURCHASE"
    const val REASON_SALE = "SALE"
    const val REASON_RETURN = "RETURN"
    const val REASON_DAMAGE = "DAMAGE"
    const val REASON_LOSS = "LOSS"
    const val REASON_OPENING = "OPENING"
    const val REASON_CORRECTION = "CORRECTION"
    const val REASON_COUNT_ADJUSTMENT = "COUNT_ADJUSTMENT"
}
