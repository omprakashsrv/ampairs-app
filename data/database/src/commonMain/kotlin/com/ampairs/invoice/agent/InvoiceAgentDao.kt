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

/** One recent invoice for the home activity feed (id + number + date-only + status). */
data class RecentInvoiceRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "number") val number: String,
    @ColumnInfo(name = "doc_date") val docDate: String,
    @ColumnInfo(name = "status") val status: String,
)

/** One GST-by-rate row for the dashboard GST summary (feature 022), grouped by the line tax code. */
data class GstByRateRow(
    @ColumnInfo(name = "code") val taxCode: String?,
    @ColumnInfo(name = "taxable") val taxable: Double,
    @ColumnInfo(name = "tax") val tax: Double,
)

/** One day/period bucket of sales for the dashboard trend (feature 022). */
data class SalesTrendRow(
    @ColumnInfo(name = "bucket") val bucket: String,
    @ColumnInfo(name = "total") val total: Double,
)

/** A product reference with its units sold, for the forecast EWMA fallback (feature 022, T045). */
data class ProductUnitsRow(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "units") val units: Double,
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

    /** Earliest local `invoice_date` (device-local "yyyy-MM-dd HH:mm:ss") — the sync-window floor (T030a). */
    @Query("SELECT MIN(invoice_date) FROM invoiceEntity WHERE active = 1")
    suspend fun minInvoiceDate(): String?

    // ── Dashboard KPI aggregates (feature 022) ──────────────────────────────────

    /** Taxable (pre-tax) net sales within a half-open period. */
    @Query("SELECT SUM(base_price) FROM invoiceEntity WHERE active = 1 AND invoice_date >= :start AND invoice_date < :end")
    suspend fun sumNetBetween(start: String, end: String): Double?

    /** Total output tax within a half-open period. */
    @Query("SELECT SUM(total_tax) FROM invoiceEntity WHERE active = 1 AND invoice_date >= :start AND invoice_date < :end")
    suspend fun sumTaxBetween(start: String, end: String): Double?

    /** Count of active invoices within a half-open period. */
    @Query("SELECT count(*) FROM invoiceEntity WHERE active = 1 AND invoice_date >= :start AND invoice_date < :end")
    suspend fun countInvoicesBetween(start: String, end: String): Int

    // ── GST summary (feature 022): intra-state (CGST+SGST) vs inter-state (IGST) ──
    // Intra iff place_of_supply == seller_place_of_supply. SQLite `IS`/`IS NOT` is null-safe
    // (NULL IS NULL → intra), matching the backend's `placeOfSupply == sellerPlaceOfSupply`.

    /** Output tax on intra-state invoices (CGST+SGST) within a half-open period. */
    @Query(
        "SELECT SUM(total_tax) FROM invoiceEntity WHERE active = 1 " +
            "AND invoice_date >= :start AND invoice_date < :end AND place_of_supply IS seller_place_of_supply",
    )
    suspend fun sumIntraStateTaxBetween(start: String, end: String): Double?

    /** Output tax on inter-state invoices (IGST) within a half-open period. */
    @Query(
        "SELECT SUM(total_tax) FROM invoiceEntity WHERE active = 1 " +
            "AND invoice_date >= :start AND invoice_date < :end AND place_of_supply IS NOT seller_place_of_supply",
    )
    suspend fun sumInterStateTaxBetween(start: String, end: String): Double?

    /** Taxable base on intra-state invoices within a half-open period. */
    @Query(
        "SELECT SUM(base_price) FROM invoiceEntity WHERE active = 1 " +
            "AND invoice_date >= :start AND invoice_date < :end AND place_of_supply IS seller_place_of_supply",
    )
    suspend fun sumTaxableIntraBetween(start: String, end: String): Double?

    /** Taxable base on inter-state invoices within a half-open period. */
    @Query(
        "SELECT SUM(base_price) FROM invoiceEntity WHERE active = 1 " +
            "AND invoice_date >= :start AND invoice_date < :end AND place_of_supply IS NOT seller_place_of_supply",
    )
    suspend fun sumTaxableInterBetween(start: String, end: String): Double?

    /** GST breakdown by line tax code (taxable base + tax) within a half-open period. */
    @Query(
        "SELECT ii.tax_code AS code, SUM(ii.base_price) AS taxable, SUM(ii.total_tax) AS tax " +
            "FROM invoiceItemEntity ii JOIN invoiceEntity i ON ii.invoice_id = i.id " +
            "WHERE i.active = 1 AND ii.active = 1 AND i.invoice_date >= :start AND i.invoice_date < :end " +
            "GROUP BY ii.tax_code ORDER BY tax DESC",
    )
    suspend fun gstByRateBetween(start: String, end: String): List<GstByRateRow>

    // ── Sales trend (feature 022): daily buckets from the "yyyy-MM-dd HH:mm:ss" invoice_date ──

    /** Daily gross-sales buckets within a half-open period (bucket = the date part of invoice_date). */
    @Query(
        "SELECT substr(invoice_date, 1, 10) AS bucket, SUM(total_cost) AS total FROM invoiceEntity " +
            "WHERE active = 1 AND invoice_date >= :start AND invoice_date < :end GROUP BY bucket ORDER BY bucket",
    )
    suspend fun salesTrendDaily(start: String, end: String): List<SalesTrendRow>

    // ── Demand forecast section (feature 022, T045) ─────────────────────────────

    /** Daily units sold of ONE product within a half-open period — the forecast sparkline series. */
    @Query(
        "SELECT substr(i.invoice_date, 1, 10) AS bucket, SUM(ii.quantity) AS total " +
            "FROM invoiceItemEntity ii JOIN invoiceEntity i ON ii.invoice_id = i.id " +
            "WHERE i.active = 1 AND ii.active = 1 AND ii.product_id = :productId " +
            "AND i.invoice_date >= :start AND i.invoice_date < :end GROUP BY bucket ORDER BY bucket",
    )
    suspend fun productDailyUnits(productId: String, start: String, end: String): List<SalesTrendRow>

    /** Top products by units sold within a half-open period — seeds the EWMA fallback when the mirror is empty. */
    @Query(
        "SELECT ii.product_id AS product_id, ii.description AS label, SUM(ii.quantity) AS units " +
            "FROM invoiceItemEntity ii JOIN invoiceEntity i ON ii.invoice_id = i.id " +
            "WHERE i.active = 1 AND ii.active = 1 AND i.invoice_date >= :start AND i.invoice_date < :end " +
            "GROUP BY ii.product_id ORDER BY units DESC LIMIT :limit",
    )
    suspend fun topProductUnitsBetween(start: String, end: String, limit: Int): List<ProductUnitsRow>

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

    /** Recent invoices for the home activity feed (number + date-only + status), newest first. */
    @Query(
        "SELECT id, invoice_number AS number, substr(invoice_date, 1, 10) AS doc_date, status " +
            "FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC LIMIT :limit",
    )
    suspend fun recentActivity(limit: Int): List<RecentInvoiceRow>
}
