package com.ampairs.analytics.domain

/**
 * A dashboard KPI a natural-language question can resolve to (feature 022, T049). [moneyValued]
 * decides whether the UI renders the answer with `formatMoney` or as a plain count/ratio.
 */
enum class NlMetric(val moneyValued: Boolean) {
    GROSS_SALES(true),
    NET_SALES(true),
    TAX(true),
    INVOICES(false),
    AVG_INVOICE(true),
    COLLECTIONS(true),
    STOCK_VALUE(true),
    LOW_STOCK(false),
    OUTSTANDING(true),
    INVENTORY_TURNS(false),
}

/** The outcome of an NL question against the current dashboard aggregates. */
sealed interface NlAnswer {
    /** The question mapped deterministically to a KPI tile; [value] is the raw figure to render. */
    data class Answered(val metric: NlMetric, val value: Double) : NlAnswer

    /** No common-question mapping matched — the UI shows a "couldn't answer" hint (FR-023). */
    data object Unanswered : NlAnswer
}

/**
 * Deterministic natural-language → KPI mapping (T049). Common questions ("total sales this period",
 * "how many invoices", "what's outstanding") resolve offline to a tile the dashboard already computes
 * — no LLM, no network. Anything unmatched returns [NlAnswer.Unanswered]; free-form NL/SQL is handled
 * by the separate agent assistant (which owns the on-device model), so this stays module-boundary-clean.
 *
 * Pure and side-effect-free for unit testing. Match order matters: more specific phrases first
 * (e.g. "net sales" before the broader "sales").
 */
object NlQueryMatcher {

    fun match(question: String, kpis: DashboardKpis): NlAnswer {
        val q = question.lowercase().trim()
        if (q.isEmpty()) return NlAnswer.Unanswered
        val metric = when {
            q.contains("net sale") || (q.contains("net") && q.contains("sale")) -> NlMetric.NET_SALES
            q.contains("gross") || q.contains("revenue") || q.contains("sale") || q.contains("turnover") -> NlMetric.GROSS_SALES
            q.contains("tax") || q.contains("gst") -> NlMetric.TAX
            q.contains("average") || q.contains("avg") || q.contains("mean invoice") -> NlMetric.AVG_INVOICE
            q.contains("collect") || q.contains("received") || q.contains("payment") -> NlMetric.COLLECTIONS
            q.contains("low stock") || q.contains("reorder") || q.contains("running low") -> NlMetric.LOW_STOCK
            q.contains("stock value") || q.contains("inventory value") || q.contains("stock worth") -> NlMetric.STOCK_VALUE
            q.contains("outstanding") || q.contains("receivable") || q.contains("owed") || q.contains("unpaid") || q.contains("due") -> NlMetric.OUTSTANDING
            q.contains("turn") -> NlMetric.INVENTORY_TURNS
            q.contains("invoice") || q.contains("bill") -> NlMetric.INVOICES
            else -> null
        }
        return metric?.let { NlAnswer.Answered(it, valueOf(it, kpis)) } ?: NlAnswer.Unanswered
    }

    fun valueOf(metric: NlMetric, kpis: DashboardKpis): Double = when (metric) {
        NlMetric.GROSS_SALES -> kpis.grossSales
        NlMetric.NET_SALES -> kpis.netSales
        NlMetric.TAX -> kpis.totalTax
        NlMetric.INVOICES -> kpis.invoiceCount.toDouble()
        NlMetric.AVG_INVOICE -> kpis.averageInvoiceValue
        NlMetric.COLLECTIONS -> kpis.collectionsReceived
        NlMetric.STOCK_VALUE -> kpis.stockValue
        NlMetric.LOW_STOCK -> kpis.lowStockCount.toDouble()
        NlMetric.OUTSTANDING -> kpis.outstandingReceivable
        NlMetric.INVENTORY_TURNS -> kpis.inventoryTurns
    }
}
