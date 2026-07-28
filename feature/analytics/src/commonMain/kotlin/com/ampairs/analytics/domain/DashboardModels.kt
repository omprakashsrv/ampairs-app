package com.ampairs.analytics.domain

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

/** Everything the dashboard renders for one period, composed from the per-module agent DAOs. */
data class DashboardData(
    val kpis: DashboardKpis = DashboardKpis(),
    val gst: GstSummary = GstSummary(),
    val trend: List<SalesTrendPoint> = emptyList(),
    val aging: AgingReport = AgingReport(),
)
