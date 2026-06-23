package com.ampairs.inventory.util

object InventoryConstants {
    const val ITEM_UID_PREFIX = "INV"
    const val TXN_UID_PREFIX = "TXN"
    const val SETTING_UID_PREFIX = "IST"

    // ── Workspace settings (persisted via the store-settings mechanism, module = "inventory") ──
    const val SETTINGS_MODULE = "inventory"
    const val FLAG_AUTO_DEDUCT_ON_SALE = "auto_deduct_stock_on_sale"
    const val FLAG_BLOCK_ORDERS_WHEN_OUT = "block_orders_when_out_of_stock"
    const val FLAG_ALLOW_NEGATIVE_STOCK = "allow_negative_stock"
    const val FLAG_ALLOW_MANUAL_ADJUSTMENTS = "allow_manual_adjustments"
    const val FLAG_LOW_STOCK_ALERTS = "enable_low_stock_alerts"

    // Defaults (caller-owned, so guardrails work offline before any override/definition syncs).
    const val DEFAULT_AUTO_DEDUCT_ON_SALE = true
    const val DEFAULT_BLOCK_ORDERS_WHEN_OUT = true
    const val DEFAULT_ALLOW_NEGATIVE_STOCK = false
    const val DEFAULT_ALLOW_MANUAL_ADJUSTMENTS = true
    const val DEFAULT_LOW_STOCK_ALERTS = true

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
