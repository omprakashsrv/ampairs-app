package com.ampairs.analytics.data.query

import com.ampairs.analytics.domain.AgingBucket
import com.ampairs.analytics.domain.AgingReport
import com.ampairs.analytics.domain.DashboardData
import com.ampairs.analytics.domain.DashboardKpis
import com.ampairs.analytics.domain.GstRateLine
import com.ampairs.analytics.domain.GstSummary
import com.ampairs.analytics.domain.SalesTrendPoint
import com.ampairs.common.agent.DateRange
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.inventory.agent.InventoryAgentDao
import com.ampairs.invoice.agent.InvoiceAgentDao
import com.ampairs.payment.agent.PaymentAgentDao
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Cross-module read facade (feature 022, T029): composes the per-module read-only agent DAOs into
 * one [DashboardData] snapshot. There is **no cross-DB join** — each figure comes from its own
 * module's agent DAO and the facade stitches them together (R4).
 *
 * Date-column formats differ by source and MUST be matched exactly or a query silently returns the
 * wrong sum:
 *  - `invoiceEntity.invoice_date` → device-local `"yyyy-MM-dd HH:mm:ss"` → [DateTimeAdapter.toDateTimeString]
 *  - `payment_voucher.voucher_date` and `inventory_transactions.transactionDate` → ISO-8601 → `Instant.toString()`
 *
 * The caller (ViewModel) computes [range] in the **business** time zone via `ReportPeriod`, and
 * supplies [today] (business-zone local date) for aging bucketing.
 */
@Inject
class DashboardReadFacade(
    private val invoiceAgentDao: InvoiceAgentDao,
    private val inventoryAgentDao: InventoryAgentDao,
    private val paymentAgentDao: PaymentAgentDao,
) {

    suspend fun load(range: DateRange, today: LocalDate): DashboardData {
        // Half-open bounds in each column's own storage format.
        val invStart = DateTimeAdapter.toDateTimeString(range.startInclusive)
        val invEnd = DateTimeAdapter.toDateTimeString(range.endExclusive)
        val isoStart = range.startInclusive.toString()
        val isoEnd = range.endExclusive.toString()

        return DashboardData(
            kpis = loadKpis(invStart, invEnd, isoStart, isoEnd),
            gst = loadGst(invStart, invEnd),
            trend = invoiceAgentDao.salesTrendDaily(invStart, invEnd)
                .map { SalesTrendPoint(bucket = it.bucket, total = it.total) },
            aging = loadAging(today),
        )
    }

    private suspend fun loadKpis(
        invStart: String,
        invEnd: String,
        isoStart: String,
        isoEnd: String,
    ): DashboardKpis {
        val gross = invoiceAgentDao.sumSalesBetween(invStart, invEnd) ?: 0.0
        val net = invoiceAgentDao.sumNetBetween(invStart, invEnd) ?: 0.0
        val tax = invoiceAgentDao.sumTaxBetween(invStart, invEnd) ?: 0.0
        val count = invoiceAgentDao.countInvoicesBetween(invStart, invEnd)
        val avg = invoiceAgentDao.averageInvoiceValueBetween(invStart, invEnd) ?: 0.0

        // payment_voucher.total_minor is paise (minor units) → rupees.
        val collections = paymentAgentDao.sumByDirectionBetween("RECEIVED", isoStart, isoEnd) / 100.0
        val outstanding = paymentAgentDao.sumOutstanding() / 100.0

        val stockValue = inventoryAgentDao.stockValue() ?: 0.0
        val lowStock = inventoryAgentDao.countLowStock()

        // Inventory turns ≈ units moved out this period / units currently on hand.
        val stockOut = inventoryAgentDao.sumStockOutBetween(isoStart, isoEnd) ?: 0.0
        val onHand = inventoryAgentDao.sumCurrentStock() ?: 0.0
        val turns = if (onHand > 0.0) stockOut / onHand else 0.0

        return DashboardKpis(
            grossSales = gross,
            netSales = net,
            totalTax = tax,
            invoiceCount = count,
            averageInvoiceValue = avg,
            collectionsReceived = collections,
            stockValue = stockValue,
            lowStockCount = lowStock,
            outstandingReceivable = outstanding,
            inventoryTurns = turns,
        )
    }

    private suspend fun loadGst(invStart: String, invEnd: String): GstSummary = GstSummary(
        intraStateTax = invoiceAgentDao.sumIntraStateTaxBetween(invStart, invEnd) ?: 0.0,
        interStateTax = invoiceAgentDao.sumInterStateTaxBetween(invStart, invEnd) ?: 0.0,
        taxableIntra = invoiceAgentDao.sumTaxableIntraBetween(invStart, invEnd) ?: 0.0,
        taxableInter = invoiceAgentDao.sumTaxableInterBetween(invStart, invEnd) ?: 0.0,
        byRate = invoiceAgentDao.gstByRateBetween(invStart, invEnd).map {
            GstRateLine(taxCode = it.taxCode ?: "", taxable = it.taxable, tax = it.tax)
        },
    )

    /**
     * Receivables aging from the open-invoice rows: `outstanding = total (rupees) − allocated/100`.
     * Bucketed by whole days between the invoice date and [today]; the date part is the first 10
     * chars of the device-local `invoice_date` (`yyyy-MM-dd ...`).
     */
    private suspend fun loadAging(today: LocalDate): AgingReport {
        val rows = paymentAgentDao.openInvoicesForAging()
        val counts = IntArray(BUCKETS.size)
        val amounts = DoubleArray(BUCKETS.size)
        rows.forEach { row ->
            val outstanding = row.total - row.allocatedMinor / 100.0
            if (outstanding <= 0.01) return@forEach
            val idx = bucketIndex(ageInDays(row.invoiceDate, today))
            counts[idx]++
            amounts[idx] += outstanding
        }
        return AgingReport(
            buckets = BUCKETS.indices.map { i ->
                AgingBucket(label = BUCKETS[i].label, count = counts[i], amount = amounts[i])
            },
        )
    }

    private fun ageInDays(invoiceDate: String, today: LocalDate): Int {
        val datePart = invoiceDate.take(10)
        val invoiced = runCatching { LocalDate.parse(datePart) }.getOrNull() ?: return 0
        return invoiced.daysUntil(today).coerceAtLeast(0)
    }

    private fun bucketIndex(ageDays: Int): Int {
        BUCKETS.forEachIndexed { i, b -> if (ageDays <= b.maxDays) return i }
        return BUCKETS.lastIndex
    }

    private data class Bucket(val label: String, val maxDays: Int)

    private companion object {
        // Ordered youngest-first; the last bucket is the catch-all (maxDays = Int.MAX_VALUE).
        val BUCKETS = listOf(
            Bucket("0–30 days", 30),
            Bucket("31–60 days", 60),
            Bucket("61–90 days", 90),
            Bucket("90+ days", Int.MAX_VALUE),
        )
    }
}
