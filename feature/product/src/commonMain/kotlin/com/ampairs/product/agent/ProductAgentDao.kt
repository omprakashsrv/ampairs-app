package com.ampairs.product.agent

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

/** A low-stock product line for the assistant's LOW_STOCK report (name + on-hand quantity). */
data class LowStockProduct(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "stock_quantity") val quantity: Double,
)

/**
 * Read-only stock-report queries that back the assistant's curated product reports — kept **separate**
 * from the operational [com.ampairs.product.db.dao.ProductDao] so report concerns don't leak into the
 * CRUD/sync DAO. All queries are over the existing `productEntity` table (no schema impact).
 *
 * Only products that actually track stock count: `low_stock_alert` / `stock_quantity` must be set (a
 * NULL `stock_quantity` means "not tracked", never "zero"), so untracked items aren't reported as
 * out-of-stock or low-stock.
 */
@Dao
interface ProductAgentDao {

    /** Count of stock-tracked products at or below their configured low-stock alert level. */
    @Query(
        "SELECT count(*) FROM productEntity WHERE active = 1 AND low_stock_alert IS NOT NULL " +
            "AND low_stock_alert > 0 AND stock_quantity IS NOT NULL AND stock_quantity <= low_stock_alert",
    )
    suspend fun countLowStock(): Int

    /** Lowest-stock products at or below their alert level (most urgent first), name + on-hand qty. */
    @Query(
        "SELECT name, stock_quantity FROM productEntity WHERE active = 1 AND low_stock_alert IS NOT NULL " +
            "AND low_stock_alert > 0 AND stock_quantity IS NOT NULL AND stock_quantity <= low_stock_alert " +
            "ORDER BY stock_quantity ASC LIMIT :limit",
    )
    suspend fun lowStockProducts(limit: Int): List<LowStockProduct>

    /** Count of stock-tracked products with zero (or negative) on-hand quantity. */
    @Query("SELECT count(*) FROM productEntity WHERE active = 1 AND stock_quantity IS NOT NULL AND stock_quantity <= 0")
    suspend fun countOutOfStock(): Int

    /** Total stock valuation at cost (Σ on-hand × cost price) across stock-tracked products. */
    @Query("SELECT COALESCE(SUM(stock_quantity * dp), 0) FROM productEntity WHERE active = 1 AND stock_quantity IS NOT NULL")
    suspend fun inventoryValueAtCost(): Double
}
