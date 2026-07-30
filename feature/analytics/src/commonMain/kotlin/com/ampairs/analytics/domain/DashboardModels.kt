package com.ampairs.analytics.domain

import kotlinx.datetime.LocalDate

/**
 * The selectable reporting window for the analytics dashboard. [keyword] is the string
 * [com.ampairs.common.agent.ReportPeriod] understands; `ALL_TIME` maps to a null range (no filter).
 */
enum class DashboardPeriod(val keyword: String) {
    TODAY("today"),
    THIS_WEEK("this_week"),
    THIS_MONTH("this_month"),
    LAST_MONTH("last_month"),
    THIS_YEAR("this_year"),
    ALL_TIME("all"),
}

/**
 * Headline KPI figures for the selected period. All money values are in the workspace's base
 * currency **rupees** (already divided out of paise where the source stored minor units); the UI
 * renders them with `formatMoney(amount, LocalAppLocale.current)`.
 */
data class DashboardKpis(
    val grossSales: Double = 0.0,
    val netSales: Double = 0.0,
    val totalTax: Double = 0.0,
    val invoiceCount: Int = 0,
    val averageInvoiceValue: Double = 0.0,
    val collectionsReceived: Double = 0.0,
    val stockValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outstandingReceivable: Double = 0.0,
    val inventoryTurns: Double = 0.0,
)

/** One tax-rate bucket in the GST summary (grouped by the line's tax code). */
data class GstRateLine(
    val taxCode: String,
    val taxable: Double,
    val tax: Double,
)

/**
 * GST summary for the period: intra-state (CGST+SGST) vs inter-state (IGST) split plus the
 * per-rate breakdown. Intra iff `place_of_supply == seller_place_of_supply`.
 */
data class GstSummary(
    val intraStateTax: Double = 0.0,
    val interStateTax: Double = 0.0,
    val taxableIntra: Double = 0.0,
    val taxableInter: Double = 0.0,
    val byRate: List<GstRateLine> = emptyList(),
) {
    val totalTax: Double get() = intraStateTax + interStateTax
}

/** One point of the daily sales trend (bucket = `yyyy-MM-dd`). */
data class SalesTrendPoint(
    val bucket: String,
    val total: Double,
)

/**
 * One entry of a ranked "top N" breakdown (top customers by sales, top products by revenue) — a
 * display label and its money value in workspace base currency, most-valuable first.
 */
data class RankedItem(
    val label: String,
    val value: Double,
)

/** One receivables-aging bucket (e.g. "0–30 days"): number of open invoices and amount owed. */
data class AgingBucket(
    val label: String,
    val count: Int,
    val amount: Double,
)

/** The full receivables-aging report, most-current bucket first. */
data class AgingReport(
    val buckets: List<AgingBucket> = emptyList(),
) {
    val totalOutstanding: Double get() = buckets.sumOf { it.amount }
}

/** Where a [ProductForecast]'s expected-demand figure came from. */
enum class ForecastSource {
    /** Server-computed forecast pulled into the `demand_forecast` mirror (Holt-Winters / MA). */
    SERVER,

    /** On-device EWMA fallback ([DemandForecasting]) — the mirror was empty (offline / not yet run). */
    EWMA,
}

/**
 * One product's forward-looking demand signal for the dashboard forecast section (feature 022, T045).
 *
 * [expectedDemand] is the **horizon-total** expected quantity over [horizonDays] (matching the server
 * `mean_qty` contract); [perDayDemand] is that divided by the horizon. [recentDailyUnits] is the
 * trailing daily units-sold series that feeds the sparkline (and the EWMA fallback). A product is a
 * [reorderCandidate] when on-hand [currentStock] can't cover the expected demand over the horizon.
 */
data class ProductForecast(
    val productId: String,
    val productName: String,
    val expectedDemand: Double,
    val perDayDemand: Double,
    val horizonDays: Int,
    val confidence: String,
    val source: ForecastSource,
    val recentDailyUnits: List<Double> = emptyList(),
    val currentStock: Double = 0.0,
    val reorderCandidate: Boolean = false,
)

/** Everything the dashboard renders for one period, composed from the per-module agent DAOs. */
data class DashboardData(
    val kpis: DashboardKpis = DashboardKpis(),
    val gst: GstSummary = GstSummary(),
    val trend: List<SalesTrendPoint> = emptyList(),
    val aging: AgingReport = AgingReport(),
    val forecasts: List<ProductForecast> = emptyList(),
    val topCustomers: List<RankedItem> = emptyList(),
    val topProducts: List<RankedItem> = emptyList(),
)

/**
 * How complete the rendered [DashboardData] is for the selected period (feature 022, T030a / FR-011).
 * [Full] = the local sync window covers the whole period, or the earlier remainder was merged from the
 * server. [Reduced] = the period extends before the local window and the device is offline, so figures
 * only cover data from [fromDate] onward — surfaced as a "showing data from {date}" badge rather than
 * silently undercounting.
 */
sealed interface DashboardCoverage {
    data object Full : DashboardCoverage
    data class Reduced(val fromDate: LocalDate) : DashboardCoverage
}

/**
 * Additive KPI + trend totals for the slice of a period that predates the local sync window, fetched
 * from the backend deep-history reads (T030). Snapshot figures (stock value, outstanding, aging) are
 * NOT range-additive and are intentionally absent — they stay local-only.
 */
data class DeepHistorySlice(
    val grossSales: Double = 0.0,
    val netSales: Double = 0.0,
    val totalTax: Double = 0.0,
    val invoiceCount: Int = 0,
    val collectionsReceived: Double = 0.0,
    val trend: List<SalesTrendPoint> = emptyList(),
)

/**
 * Merge a server-fetched pre-window [slice] into local aggregates (T030a). The slice covers a date
 * range strictly BEFORE the local window, so the ranges are disjoint and additive — no double count.
 * Only additive figures merge; snapshot KPIs (stock/outstanding/turns), GST detail, aging and
 * forecasts are left as the local values.
 */
fun DashboardData.mergePriorSlice(slice: DeepHistorySlice): DashboardData {
    val gross = kpis.grossSales + slice.grossSales
    val count = kpis.invoiceCount + slice.invoiceCount
    val mergedKpis = kpis.copy(
        grossSales = gross,
        netSales = kpis.netSales + slice.netSales,
        totalTax = kpis.totalTax + slice.totalTax,
        invoiceCount = count,
        averageInvoiceValue = if (count > 0) gross / count else 0.0,
        collectionsReceived = kpis.collectionsReceived + slice.collectionsReceived,
    )
    val mergedTrend = (slice.trend + trend)
        .groupBy { it.bucket }
        .map { (bucket, points) -> SalesTrendPoint(bucket, points.sumOf { it.total }) }
        .sortedBy { it.bucket }
    return copy(kpis = mergedKpis, trend = mergedTrend)
}
