package com.ampairs.inventory.data.db

import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventoryMovement

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the inventory domain models.

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

fun InventoryTransactionEntity.toMovement(): InventoryMovement = InventoryMovement(
    uid = id,
    transactionNumber = transactionNumber,
    transactionType = transactionType,
    transactionReason = transactionReason,
    inventoryItemId = inventoryItemId,
    warehouseId = warehouseId,
    quantity = quantity,
    balanceAfter = balanceAfter,
    unitCost = unitCost,
    totalCost = totalCost,
    sourceType = sourceType,
    sourceId = sourceId,
    sourceLineUid = sourceLineUid,
    referenceNumber = referenceNumber,
    transactionDate = transactionDate,
    performedBy = performedBy,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun InventoryMovement.toEntity(): InventoryTransactionEntity = InventoryTransactionEntity(
    id = uid,
    transactionNumber = transactionNumber,
    transactionType = transactionType,
    transactionReason = transactionReason,
    inventoryItemId = inventoryItemId,
    warehouseId = warehouseId,
    quantity = quantity,
    balanceAfter = balanceAfter,
    unitCost = unitCost,
    totalCost = totalCost,
    sourceType = sourceType,
    sourceId = sourceId,
    sourceLineUid = sourceLineUid,
    referenceNumber = referenceNumber,
    transactionDate = transactionDate,
    performedBy = performedBy,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
