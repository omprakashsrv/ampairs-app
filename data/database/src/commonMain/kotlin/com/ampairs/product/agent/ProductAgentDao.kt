package com.ampairs.product.agent

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query

/** A low-stock product line for the assistant's LOW_STOCK report (name + on-hand quantity). */
data class LowStockProduct(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "stock_quantity") val quantity: Double,
)

/** A product hit for the assistant's chat search (id + name + code + selling price). */
data class ProductSearchHit(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "selling_price") val sellingPrice: Double,
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

    /** Display name for a product id — labels the dashboard forecast rows (feature 022, T045). */
    @Query("SELECT name FROM productEntity WHERE id = :id AND active = 1 LIMIT 1")
    suspend fun nameById(id: String): String?

    /**
     * Chat-triggered product search. Case-insensitive and whitespace-normalized on `name` so a query
     * with single spaces still matches product names that contain double (or more) spaces — the cause
     * of "couldn't find the product" reports from the assistant. Also matches `code` (contains) and
     * `tax_code` (prefix). Caller passes a single already-trimmed word/phrase; multi-word OR-matching
     * (typo/word-order tolerance) is composed in [ProductActionHandler] by calling this per word.
     */
    @Query(
        """
        SELECT id, name, code, selling_price FROM productEntity
        WHERE active = 1
          AND (REPLACE(REPLACE(REPLACE(REPLACE(LOWER(name), '    ', ' '), '   ', ' '), '  ', ' '), '  ', ' ') LIKE '%' || LOWER(:term) || '%'
               OR LOWER(code) LIKE '%' || LOWER(:term) || '%'
               OR LOWER(tax_code) LIKE LOWER(:term) || '%')
        ORDER BY name ASC LIMIT :limit
        """
    )
    suspend fun searchProducts(term: String, limit: Long): List<ProductSearchHit>
}
