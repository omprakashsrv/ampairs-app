package com.ampairs.inventory.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.ampairs.inventory.domain.InventoryItem

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

fun InventoryItemEntity.toInventoryItem(): InventoryItem = InventoryItem(
    uid = id,
    name = name,
    sku = sku,
    productId = productId,
    productVariantId = productVariantId,
    unitId = unitId,
    warehouseId = warehouseId,
    currentStock = currentStock,
    reservedStock = reservedStock,
    availableStock = availableStock,
    reorderLevel = reorderLevel,
    costPrice = costPrice,
    sellingPrice = sellingPrice,
    mrp = mrp,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pendingSync = !synced,
)

fun InventoryItem.toEntity(): InventoryItemEntity = InventoryItemEntity(
    id = uid,
    name = name,
    sku = sku,
    productId = productId,
    productVariantId = productVariantId,
    unitId = unitId,
    warehouseId = warehouseId,
    currentStock = currentStock,
    reservedStock = reservedStock,
    availableStock = availableStock,
    reorderLevel = reorderLevel,
    costPrice = costPrice,
    sellingPrice = sellingPrice,
    mrp = mrp,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
