package com.ampairs.inventory.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["active"]),
    ],
)
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String?,
    val productId: String?,
    val productVariantId: String?,
    val unitId: String?,
    val warehouseId: String?,
    val currentStock: Double,
    val reservedStock: Double,
    val availableStock: Double,
    val reorderLevel: Double,
    val costPrice: Double,
    val sellingPrice: Double,
    val mrp: Double,
    val active: Boolean,
    val synced: Boolean = false,
    val createdAt: String?,
    val updatedAt: String?,
)
