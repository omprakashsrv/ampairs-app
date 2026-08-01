package com.ampairs.inventory.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
