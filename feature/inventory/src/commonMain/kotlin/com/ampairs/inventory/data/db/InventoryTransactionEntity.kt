package com.ampairs.inventory.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.ampairs.inventory.domain.InventoryMovement

@Entity(
    tableName = "inventory_transactions",
    indices = [
        Index(value = ["inventoryItemId"]),
    ],
)
data class InventoryTransactionEntity(
    @PrimaryKey val id: String,
    val transactionNumber: String,
    val transactionType: String,
    val transactionReason: String,
    val inventoryItemId: String,
    val warehouseId: String?,
    val quantity: Double,
    val balanceAfter: Double,
    val unitCost: Double,
    val totalCost: Double,
    val sourceType: String?,
    val sourceId: String?,
    val sourceLineUid: String?,
    val referenceNumber: String?,
    val transactionDate: String?,
    val performedBy: String?,
    val notes: String?,
    val synced: Boolean = false,
    val createdAt: String?,
    val updatedAt: String?,
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
