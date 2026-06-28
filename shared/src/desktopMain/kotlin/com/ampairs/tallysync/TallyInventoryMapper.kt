package com.ampairs.tallysync

import com.ampairs.inventory.data.db.InventoryItemEntity
import com.ampairs.inventory.data.db.InventoryTransactionEntity
import com.ampairs.inventory.util.InventoryConstants
import com.ampairs.product.db.entity.ProductEntity

/**
 * Maps Tally stock (closing balances) into the inventory module's item + transaction model.
 *
 * Each product with stock becomes one [InventoryItemEntity] (metadata + local on-hand) plus a single
 * deterministic opening [InventoryTransactionEntity] (STOCK_IN / OPENING) carrying the Tally closing
 * quantity. Ids are derived from the Tally GUID so re-syncing a changed balance upserts in place
 * (no duplicate items or opening movements). The backend recomputes the authoritative balance from
 * the transaction feed on the next pull.
 */
internal object TallyInventoryMapper {

    private const val ID_PREFIX_ITEM = "INVTLY"
    private const val ID_PREFIX_TXN = "TXNTLY"

    private fun sanitize(guid: String): String = guid.filter { it.isLetterOrDigit() }

    /** Deterministic inventory-item id for a Tally stock GUID. */
    fun inventoryItemId(guid: String): String = ID_PREFIX_ITEM + sanitize(guid)

    /** Inventory item carrying product metadata + the local on-hand for immediate display. */
    fun ProductEntity.toInventoryItemEntity(itemId: String, currentStock: Double): InventoryItemEntity =
        InventoryItemEntity(
            id = itemId,
            name = name,
            sku = code.takeIf { it.isNotBlank() },
            productId = id,
            productVariantId = null,
            unitId = base_unit,
            warehouseId = null,
            currentStock = currentStock,
            reservedStock = 0.0,
            availableStock = currentStock,
            reorderLevel = 0.0,
            costPrice = dp,
            sellingPrice = selling_price,
            mrp = mrp,
            active = true,
            synced = false,
            createdAt = null,
            updatedAt = null,
        )

    /**
     * Single opening movement (STOCK_IN / OPENING) that puts the whole Tally closing quantity on the
     * ledger. Deterministic id from the GUID → re-sync upserts the one opening row in place rather
     * than accumulating, so a changed Tally balance simply updates this row's quantity.
     */
    fun openingMovement(
        guid: String,
        inventoryItemId: String,
        quantity: Double,
        unitCost: Double,
    ): InventoryTransactionEntity =
        InventoryTransactionEntity(
            id = ID_PREFIX_TXN + sanitize(guid),
            transactionNumber = "TALLY-OPEN-" + sanitize(guid).takeLast(10),
            transactionType = InventoryConstants.TXN_TYPE_STOCK_IN,
            transactionReason = InventoryConstants.REASON_OPENING,
            inventoryItemId = inventoryItemId,
            warehouseId = null,
            quantity = quantity,
            balanceAfter = quantity,
            unitCost = unitCost,
            totalCost = quantity * unitCost,
            sourceType = "TALLY",
            sourceId = guid,
            sourceLineUid = null,
            referenceNumber = null,
            transactionDate = null,
            performedBy = "TallySync",
            notes = "Opening stock imported from Tally",
            synced = false,
            createdAt = null,
            updatedAt = null,
        )
}
