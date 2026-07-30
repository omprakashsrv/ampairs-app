package com.ampairs.analytics.data.query

import com.ampairs.analytics.data.db.dao.DemandForecastDao
import com.ampairs.analytics.domain.AgingBuckets
import com.ampairs.analytics.domain.AgingReport
import com.ampairs.analytics.domain.DashboardData
import com.ampairs.analytics.domain.DashboardKpis
import com.ampairs.analytics.domain.DemandForecasting
import com.ampairs.analytics.domain.ForecastSource
import com.ampairs.analytics.domain.GstRateLine
import com.ampairs.analytics.domain.GstSummary
import com.ampairs.analytics.domain.ProductForecast
import com.ampairs.analytics.domain.RankedItem
import com.ampairs.analytics.domain.SalesTrendPoint
import com.ampairs.common.agent.DateRange
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.inventory.agent.InventoryAgentDao
import com.ampairs.invoice.agent.InvoiceAgentDao
import com.ampairs.payment.agent.PaymentAgentDao
import com.ampairs.product.agent.ProductAgentDao
import dev.zacsweers.metro.Inject
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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
    private val demandForecastDao: DemandForecastDao,
    private val productAgentDao: ProductAgentDao,
) {

    suspend fun load(range: DateRange, today: LocalDate, tz: TimeZone): DashboardData {
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
            forecasts = loadForecasts(today, tz),
            topCustomers = invoiceAgentDao.topCustomersBetween(invStart, invEnd, TOP_LIMIT)
                .map { RankedItem(it.label?.takeIf { l -> l.isNotBlank() } ?: "—", it.total) },
            topProducts = invoiceAgentDao.topProductsBetween(invStart, invEnd, TOP_LIMIT)
                .map { RankedItem(it.label?.takeIf { l -> l.isNotBlank() } ?: "—", it.total) },
        )
    }

    /**
     * The forward-looking demand section (T045). Server-computed forecasts in the `demand_forecast`
     * mirror win; if the mirror is empty (offline first launch / nightly batch not yet run) it falls
     * back to an on-device [DemandForecasting] EWMA over the top-selling products (T044). The forecast
     * window is a fixed trailing [SPARK_DAYS] regardless of the dashboard period — a forecast is a
     * property of the product, not of the reporting window.
     */
    private suspend fun loadForecasts(today: LocalDate, tz: TimeZone): List<ProductForecast> {
        val startDate = today.minus(SPARK_DAYS - 1, DateTimeUnit.DAY)
        val start = DateTimeAdapter.toDateTimeString(startDate.atStartOfDayIn(tz))
        val end = DateTimeAdapter.toDateTimeString(today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz))

        val serverRows = demandForecastDao.latestPerProduct(FORECAST_LIMIT)
        if (serverRows.isNotEmpty()) {
            return serverRows.map { row ->
                val series = densifyDailyUnits(row.productId, start, end, startDate)
                val stock = inventoryAgentDao.stockByProduct(row.productId)?.onHand ?: 0.0
                val horizon = row.horizon.coerceAtLeast(1)
                ProductForecast(
                    productId = row.productId,
                    productName = productAgentDao.nameById(row.productId) ?: row.productId,
                    expectedDemand = row.meanQty,
                    perDayDemand = row.meanQty / horizon,
                    horizonDays = row.horizon,
                    confidence = row.confidence,
                    source = ForecastSource.SERVER,
                    recentDailyUnits = series,
                    currentStock = stock,
                    reorderCandidate = stock < row.meanQty,
                )
            }
        }

        // Fallback: no server forecast yet — estimate from local sales history via EWMA.
        val horizon = DemandForecasting.DEFAULT_HORIZON_DAYS
        return invoiceAgentDao.topProductUnitsBetween(start, end, FORECAST_LIMIT)
            .filter { it.units > 0.0 }
            .map { ref ->
                val series = densifyDailyUnits(ref.productId, start, end, startDate)
                val stock = inventoryAgentDao.stockByProduct(ref.productId)?.onHand ?: 0.0
                val expected = DemandForecasting.expectedDemand(series, horizon)
                ProductForecast(
                    productId = ref.productId,
                    productName = ref.label?.takeIf { it.isNotBlank() } ?: ref.productId,
                    expectedDemand = expected,
                    perDayDemand = expected / horizon,
                    horizonDays = horizon,
                    confidence = "",
                    source = ForecastSource.EWMA,
                    recentDailyUnits = series,
                    currentStock = stock,
                    reorderCandidate = stock < expected,
                )
            }
    }

    /**
     * The earliest business date present in the local invoice mirror — the sync-window floor used to
     * decide whether a requested period extends into deep history (T030a). `null` when there are no
     * local invoices yet. The date part of the device-local `invoice_date` (`yyyy-MM-dd ...`).
     */
    suspend fun earliestLocalBusinessDate(): LocalDate? {
        val raw = invoiceAgentDao.minInvoiceDate() ?: return null
        return runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    }

    /** Dense trailing daily-units series (zero-filled) for the sparkline / EWMA input. */
    private suspend fun densifyDailyUnits(
        productId: String,
        start: String,
        end: String,
        startDate: LocalDate,
    ): List<Double> {
        val byDate = invoiceAgentDao.productDailyUnits(productId, start, end)
            .associate { it.bucket to it.total }
        return (0 until SPARK_DAYS).map { i ->
            byDate[startDate.plus(i, DateTimeUnit.DAY).toString()] ?: 0.0
        }
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
        val entries = paymentAgentDao.openInvoicesForAging().map { row ->
            AgingBuckets.Entry(row.invoiceDate, row.total - row.allocatedMinor / 100.0)
        }
        return AgingBuckets.compute(entries, today)
    }

    private companion object {
        /** Trailing window (days) for the forecast sparkline series and EWMA fallback. */
        const val SPARK_DAYS = 14

        /** Max products shown in the forecast section (bounds the per-product query fan-out). */
        const val FORECAST_LIMIT = 6

        /** Max entries shown in the top-customers / top-products ranked breakdowns. */
        const val TOP_LIMIT = 5
    }
}
