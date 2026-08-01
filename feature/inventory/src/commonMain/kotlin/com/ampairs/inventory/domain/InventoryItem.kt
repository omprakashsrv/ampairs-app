package com.ampairs.inventory.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Inventory item domain model — mirrors the backend InventoryItemSync DTO (spec 014).
 * Serialized field names match the backend `/inventory/v1/items/sync` contract (snake_case).
 */
@Serializable
data class InventoryItem(
    val uid: String = "",
    val name: String = "",
    val sku: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_variant_id") val productVariantId: String? = null,
    @SerialName("unit_id") val unitId: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("current_stock") val currentStock: Double = 0.0,
    @SerialName("reserved_stock") val reservedStock: Double = 0.0,
    @SerialName("available_stock") val availableStock: Double = 0.0,
    @SerialName("reorder_level") val reorderLevel: Double = 0.0,
    @SerialName("cost_price") val costPrice: Double = 0.0,
    @SerialName("selling_price") val sellingPrice: Double = 0.0,
    val mrp: Double = 0.0,
    @SerialName("is_active") val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    /** UI-only: true when this row has local edits not yet pushed. Excluded from the wire model. */
    @Transient val pendingSync: Boolean = false,
) {
    val isLowStock: Boolean get() = reorderLevel > 0.0 && currentStock > 0.0 && currentStock <= reorderLevel
    val isOutOfStock: Boolean get() = currentStock <= 0.0
    val stockValue: Double get() = currentStock * costPrice
}
