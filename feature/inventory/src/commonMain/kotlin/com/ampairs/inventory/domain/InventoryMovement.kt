package com.ampairs.inventory.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Inventory movement (transaction) domain model — mirrors the backend InventoryTransactionSync DTO.
 * Append-only ledger entry; serialized field names match `/inventory/v1/transactions/sync`.
 */
@Serializable
data class InventoryMovement(
    val uid: String = "",
    @SerialName("transaction_number") val transactionNumber: String = "",
    @SerialName("transaction_type") val transactionType: String = "",
    @SerialName("transaction_reason") val transactionReason: String = "",
    @SerialName("inventory_item_id") val inventoryItemId: String = "",
    @SerialName("warehouse_id") val warehouseId: String? = null,
    val quantity: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
    @SerialName("total_cost") val totalCost: Double = 0.0,
    @SerialName("source_type") val sourceType: String? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("source_line_uid") val sourceLineUid: String? = null,
    @SerialName("reference_number") val referenceNumber: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null,
    @SerialName("performed_by") val performedBy: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    /**
     * Signed quantity for display. STOCK_IN/STOCK_OUT carry a magnitude in [quantity] (sign comes from
     * the type); COUNT/ADJUSTMENT carry an already-signed [quantity] (the variance/delta), so it is
     * returned as-is.
     */
    val signedQuantity: Double
        get() = if (transactionType == "STOCK_OUT") -quantity else quantity
}
