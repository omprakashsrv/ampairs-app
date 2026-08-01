package com.ampairs.inventory.domain

/**
 * Lightweight, read-side stock snapshot for cross-feature use (spec 014).
 *
 * Exposed by [com.ampairs.inventory.data.InventoryDataService] so features like order/invoice can show
 * availability/low-stock in their line-item editors WITHOUT depending on the full `feature/inventory`
 * implementation. Stock mutation stays server-authoritative; this is display-only.
 */
data class InventoryStockInfo(
    val productId: String,
    val productVariantId: String? = null,
    val onHand: Double,
    val available: Double,
    val reorderLevel: Double,
    val isLowStock: Boolean,
)
