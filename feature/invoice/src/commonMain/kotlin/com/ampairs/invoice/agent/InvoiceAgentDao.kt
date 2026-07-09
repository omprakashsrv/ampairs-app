package com.ampairs.invoice.agent

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query

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

/** An invoice hit for the assistant's chat search/list (id + number + status). */
data class InvoiceSummaryRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "invoice_number") val invoiceNumber: String,
    @ColumnInfo(name = "status") val status: String,
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

    /** Count of active invoices. */
    @Query("SELECT count(*) FROM invoiceEntity WHERE active = 1")
    suspend fun countInvoices(): Int

    /** Chat search by invoice number or (whitespace-normalized, case-insensitive) customer name. */
    @Query(
        """
        SELECT id, invoice_number, status FROM invoiceEntity
        WHERE active = 1
          AND (invoice_number LIKE '%' || :term || '%'
               OR REPLACE(REPLACE(REPLACE(REPLACE(LOWER(customer_name), '    ', ' '), '   ', ' '), '  ', ' '), '  ', ' ') LIKE '%' || LOWER(:term) || '%')
        ORDER BY invoice_date DESC LIMIT :limit
        """,
    )
    suspend fun search(term: String, limit: Long): List<InvoiceSummaryRow>

    /** Resolve a single invoice by exact id (for the READ action). */
    @Query("SELECT id, invoice_number, status FROM invoiceEntity WHERE id = :id AND active = 1 LIMIT 1")
    suspend fun byId(id: String): InvoiceSummaryRow?

    /** Resolve invoices by (partial) invoice number (READ fallback). */
    @Query(
        "SELECT id, invoice_number, status FROM invoiceEntity WHERE invoice_number LIKE '%' || :number || '%' AND active = 1 ORDER BY invoice_date DESC LIMIT :limit",
    )
    suspend fun byNumber(number: String, limit: Long): List<InvoiceSummaryRow>

    /** List invoices filtered by status (most recent first). */
    @Query(
        "SELECT id, invoice_number, status FROM invoiceEntity WHERE status = :status AND active = 1 ORDER BY invoice_date DESC LIMIT :limit",
    )
    suspend fun byStatus(status: String, limit: Long): List<InvoiceSummaryRow>

    /** List most recent active invoices. */
    @Query("SELECT id, invoice_number, status FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC LIMIT :limit")
    suspend fun recent(limit: Long): List<InvoiceSummaryRow>
}
