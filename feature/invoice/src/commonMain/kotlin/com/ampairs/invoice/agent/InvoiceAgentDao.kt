package com.ampairs.invoice.agent

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

/** One row of the "top customers by sales" report. */
data class InvoiceCustomerSalesRow(
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "total") val total: Double,
)

/** One row of the "top products by sales" report (invoice ⨝ invoice-item). */
data class InvoiceProductSalesRow(
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "total") val total: Double,
)

/**
 * Read-only aggregate queries that back the assistant's curated invoice reports — kept **separate**
 * from the operational [com.ampairs.invoice.db.dao.InvoiceDao] so report concerns don't leak into the
 * CRUD/sync DAO. All queries are over existing tables (no schema impact).
 *
 * Period bounds are half-open device-local "yyyy-MM-dd HH:mm:ss" strings (see ReportPeriod +
 * DateTimeAdapter.toDateTimeString); `invoice_date` is stored in that format, so `>= start AND < end`
 * is the correct half-open comparison (no BETWEEN boundary off-by-one).
 */
@Dao
interface InvoiceAgentDao {

    /** Total sales (sum of invoice totals), all time. */
    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE active = 1")
    suspend fun sumSales(): Double?

    /** Total sales within a half-open period. */
    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE active = 1 AND invoice_date >= :start AND invoice_date < :end")
    suspend fun sumSalesBetween(start: String, end: String): Double?

    /** Average invoice value, all time. */
    @Query("SELECT AVG(total_cost) FROM invoiceEntity WHERE active = 1")
    suspend fun averageInvoiceValue(): Double?

    /** Average invoice value within a half-open period. */
    @Query("SELECT AVG(total_cost) FROM invoiceEntity WHERE active = 1 AND invoice_date >= :start AND invoice_date < :end")
    suspend fun averageInvoiceValueBetween(start: String, end: String): Double?

    /** Highest-revenue customers, all time. */
    @Query(
        "SELECT customer_name AS label, SUM(total_cost) AS total FROM invoiceEntity " +
            "WHERE active = 1 GROUP BY customer_id ORDER BY total DESC LIMIT :limit",
    )
    suspend fun topCustomers(limit: Int): List<InvoiceCustomerSalesRow>

    /** Highest-revenue customers within a half-open period. */
    @Query(
        "SELECT customer_name AS label, SUM(total_cost) AS total FROM invoiceEntity " +
            "WHERE active = 1 AND invoice_date >= :start AND invoice_date < :end " +
            "GROUP BY customer_id ORDER BY total DESC LIMIT :limit",
    )
    suspend fun topCustomersBetween(start: String, end: String, limit: Int): List<InvoiceCustomerSalesRow>

    /** Best-selling products by sales value, all time (invoice-item ⨝ invoice). */
    @Query(
        "SELECT ii.description AS label, SUM(ii.total_cost) AS total FROM invoiceItemEntity ii " +
            "JOIN invoiceEntity i ON ii.invoice_id = i.id " +
            "WHERE i.active = 1 AND ii.active = 1 GROUP BY ii.product_id ORDER BY total DESC LIMIT :limit",
    )
    suspend fun topProducts(limit: Int): List<InvoiceProductSalesRow>

    /** Best-selling products by sales value within a half-open period. */
    @Query(
        "SELECT ii.description AS label, SUM(ii.total_cost) AS total FROM invoiceItemEntity ii " +
            "JOIN invoiceEntity i ON ii.invoice_id = i.id " +
            "WHERE i.active = 1 AND ii.active = 1 AND i.invoice_date >= :start AND i.invoice_date < :end " +
            "GROUP BY ii.product_id ORDER BY total DESC LIMIT :limit",
    )
    suspend fun topProductsBetween(start: String, end: String, limit: Int): List<InvoiceProductSalesRow>
}
