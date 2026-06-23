package com.ampairs.inventory.domain

import com.ampairs.inventory.util.InventoryConstants

/**
 * The five workspace-scoped inventory behaviour flags (spec — Inventory settings). Persisted through
 * the shared store-settings mechanism (`module = "inventory"`); see [com.ampairs.inventory.domain.InventorySettingsProvider].
 * Defaults are caller-owned so guardrails work offline before any override/definition has synced.
 */
data class InventorySettings(
    val autoDeductOnSale: Boolean = InventoryConstants.DEFAULT_AUTO_DEDUCT_ON_SALE,
    val blockOrdersWhenOut: Boolean = InventoryConstants.DEFAULT_BLOCK_ORDERS_WHEN_OUT,
    val allowNegativeStock: Boolean = InventoryConstants.DEFAULT_ALLOW_NEGATIVE_STOCK,
    val allowManualAdjustments: Boolean = InventoryConstants.DEFAULT_ALLOW_MANUAL_ADJUSTMENTS,
    val lowStockAlerts: Boolean = InventoryConstants.DEFAULT_LOW_STOCK_ALERTS,
) {
    companion object {
        val Default = InventorySettings()
    }
}
