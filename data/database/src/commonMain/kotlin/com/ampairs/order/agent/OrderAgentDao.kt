package com.ampairs.order.agent

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query

/** An order hit for the assistant's chat search/list (id + number + status). */
data class OrderSummaryRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "order_number") val orderNumber: String,
    @ColumnInfo(name = "status") val status: String,
)

/** One (status → count) bucket of active orders for the dashboard orders breakdown (feature 022). */
data class OrderStatusCountRow(
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "cnt") val count: Int,
)

/**
 * Read-only report/search queries backing the assistant's order actions — separate from the
 * operational [com.ampairs.order.db.OrderDao]. Over the existing `orderEntity` table (no schema
 * impact).
 */
@Dao
interface OrderAgentDao {
    /** Count of active orders. */
    @Query("SELECT count(*) FROM orderEntity WHERE active = 1")
    suspend fun countActive(): Int

    /** Active-order counts grouped by status (most common first) — dashboard orders breakdown. */
    @Query("SELECT status, count(*) AS cnt FROM orderEntity WHERE active = 1 GROUP BY status ORDER BY cnt DESC")
    suspend fun statusCounts(): List<OrderStatusCountRow>

    /** Chat search by order number or (whitespace-normalized, case-insensitive) customer name. */
    @Query(
        """
        SELECT id, order_number, status FROM orderEntity
        WHERE active = 1
          AND (order_number LIKE '%' || :term || '%'
               OR REPLACE(REPLACE(REPLACE(REPLACE(LOWER(customer_name), '    ', ' '), '   ', ' '), '  ', ' '), '  ', ' ') LIKE '%' || LOWER(:term) || '%')
        ORDER BY order_date DESC LIMIT :limit
        """
    )
    suspend fun search(term: String, limit: Long): List<OrderSummaryRow>

    /** Resolve a single order by exact id (for the READ action). */
    @Query("SELECT id, order_number, status FROM orderEntity WHERE id = :id AND active = 1 LIMIT 1")
    suspend fun byId(id: String): OrderSummaryRow?

    /** Resolve orders by (partial) order number (READ fallback). */
    @Query(
        "SELECT id, order_number, status FROM orderEntity WHERE order_number LIKE '%' || :number || '%' AND active = 1 ORDER BY order_date DESC LIMIT :limit",
    )
    suspend fun byNumber(number: String, limit: Long): List<OrderSummaryRow>

    /** List orders filtered by status (most recent first). */
    @Query(
        "SELECT id, order_number, status FROM orderEntity WHERE status = :status AND active = 1 ORDER BY order_date DESC LIMIT :limit",
    )
    suspend fun byStatus(status: String, limit: Long): List<OrderSummaryRow>

    /** List most recent active orders. */
    @Query("SELECT id, order_number, status FROM orderEntity WHERE active = 1 ORDER BY order_date DESC LIMIT :limit")
    suspend fun recent(limit: Long): List<OrderSummaryRow>
}
