package com.ampairs.inventory.agent

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query

/** An inventory item hit for the assistant's chat search/low-stock list. */
data class InventorySearchRow(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "currentStock") val currentStock: Double,
    @ColumnInfo(name = "reorderLevel") val reorderLevel: Double,
)

/** On-hand / available stock for one product (for the GET_INVENTORY action). */
data class InventoryStockRow(
    @ColumnInfo(name = "currentStock") val onHand: Double,
    @ColumnInfo(name = "availableStock") val available: Double,
)

/**
 * Read-only report/search queries backing the assistant's inventory actions — separate from the
 * operational [com.ampairs.inventory.data.db.InventoryItemDao]. Over the existing `inventory_items`
 * table (no schema impact).
 */
@Dao
interface InventoryAgentDao {
    /** Count of active inventory items. */
    @Query("SELECT count(*) FROM inventory_items WHERE active = 1")
    suspend fun countActive(): Int

    /** Chat search by (whitespace-normalized, case-insensitive) name or SKU. */
    @Query(
        """
        SELECT name, currentStock, reorderLevel FROM inventory_items
        WHERE active = 1
          AND (REPLACE(REPLACE(REPLACE(REPLACE(LOWER(name), '    ', ' '), '   ', ' '), '  ', ' '), '  ', ' ') LIKE '%' || LOWER(:term) || '%'
               OR LOWER(sku) LIKE '%' || LOWER(:term) || '%')
        ORDER BY name ASC LIMIT :limit
        """
    )
    suspend fun search(term: String, limit: Long): List<InventorySearchRow>

    /** Count of active items matching a search term (name or SKU). */
    @Query(
        """
        SELECT count(*) FROM inventory_items
        WHERE active = 1
          AND (REPLACE(REPLACE(REPLACE(REPLACE(LOWER(name), '    ', ' '), '   ', ' '), '  ', ' '), '  ', ' ') LIKE '%' || LOWER(:term) || '%'
               OR LOWER(sku) LIKE '%' || LOWER(:term) || '%')
        """
    )
    suspend fun countMatching(term: String): Int

    /** Items at or below their reorder level (most urgent first). */
    @Query(
        "SELECT name, currentStock, reorderLevel FROM inventory_items WHERE active = 1 AND reorderLevel > 0 AND currentStock <= reorderLevel AND currentStock > 0 ORDER BY currentStock ASC LIMIT :limit",
    )
    suspend fun lowStock(limit: Long): List<InventorySearchRow>

    /** On-hand / available stock for a specific product. */
    @Query("SELECT currentStock, availableStock FROM inventory_items WHERE productId = :productId AND active = 1 LIMIT 1")
    suspend fun stockByProduct(productId: String): InventoryStockRow?

    // ── Dashboard KPI aggregates (feature 022) ──────────────────────────────────

    /** Total stock value at current cost: Σ(currentStock × costPrice) over active items. */
    @Query("SELECT SUM(currentStock * costPrice) FROM inventory_items WHERE active = 1")
    suspend fun stockValue(): Double?

    /** Total units on hand over active items (inventory-turns denominator). */
    @Query("SELECT SUM(currentStock) FROM inventory_items WHERE active = 1")
    suspend fun sumCurrentStock(): Double?

    /** Count of active items at/below their (non-zero) reorder level. */
    @Query("SELECT count(*) FROM inventory_items WHERE active = 1 AND reorderLevel > 0 AND currentStock <= reorderLevel")
    suspend fun countLowStock(): Int

    /** Units moved OUT (sales/consumption) within a half-open period — turns numerator. */
    @Query(
        "SELECT SUM(quantity) FROM inventory_transactions WHERE transactionType = 'STOCK_OUT' " +
            "AND transactionDate >= :start AND transactionDate < :end",
    )
    suspend fun sumStockOutBetween(start: String, end: String): Double?
}
