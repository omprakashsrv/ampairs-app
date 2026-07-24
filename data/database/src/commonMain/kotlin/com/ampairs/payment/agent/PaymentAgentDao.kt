package com.ampairs.payment.agent

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query

/**
 * One open (partly/un-paid) invoice for the dashboard aging report (feature 022). The ViewModel
 * computes outstanding = [total] − [allocatedMinor] ÷ 100 and buckets by the age of [invoiceDate];
 * money units differ (invoice = rupees `Double`, allocation = paise `Long`), so both are returned raw.
 */
data class OpenInvoiceRow(
    @ColumnInfo(name = "invoiceId") val invoiceId: String,
    @ColumnInfo(name = "invoiceDate") val invoiceDate: String,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "allocatedMinor") val allocatedMinor: Long,
)

/**
 * Read-only aggregate queries that back the assistant's curated payment reports — kept **separate**
 * from the operational [com.ampairs.payment.data.db.dao.PaymentVoucherDao] so report concerns don't
 * leak into the CRUD/sync DAO. Queries are over the existing `payment_voucher` table (no schema
 * impact).
 *
 * `voucher_date` is stored as an ISO-8601 instant string (Instant.toString()), so the period bounds
 * must also be ISO instants — lexical comparison over ISO-8601 UTC is the correct order.
 */
@Dao
interface PaymentAgentDao {

    /** Total payments by direction ("RECEIVED"/"PAID"), all time, in minor units (paise). */
    @Query("SELECT COALESCE(SUM(total_minor), 0) FROM payment_voucher WHERE direction = :direction AND active = 1")
    suspend fun sumByDirection(direction: String): Long

    /** Total payments by direction within a half-open period, in minor units (paise). */
    @Query(
        "SELECT COALESCE(SUM(total_minor), 0) FROM payment_voucher " +
            "WHERE direction = :direction AND active = 1 AND voucher_date >= :start AND voucher_date < :end",
    )
    suspend fun sumByDirectionBetween(direction: String, start: String, end: String): Long

    // ── Dashboard collections/aging (feature 022) ────────────────────────────────

    /** Total outstanding receivable across all parties (cached closing balance), in minor units (paise). */
    @Query("SELECT COALESCE(SUM(cached_closing_minor), 0) FROM party_balance WHERE active = 1")
    suspend fun sumOutstanding(): Long

    /**
     * Every active invoice with its allocated-payment total, for aging bucketing in the ViewModel.
     * `outstanding = total (rupees) − allocatedMinor/100`; only rows still owing are returned.
     */
    @Query(
        "SELECT i.id AS invoiceId, i.invoice_date AS invoiceDate, i.total_cost AS total, " +
            "COALESCE(SUM(a.amount_minor), 0) AS allocatedMinor " +
            "FROM invoiceEntity i " +
            "LEFT JOIN payment_allocation a ON a.target_type = 'INVOICE' AND a.target_uid = i.id AND a.active = 1 " +
            "WHERE i.active = 1 " +
            "GROUP BY i.id " +
            "HAVING (i.total_cost - COALESCE(SUM(a.amount_minor), 0) / 100.0) > 0.01",
    )
    suspend fun openInvoicesForAging(): List<OpenInvoiceRow>
}
