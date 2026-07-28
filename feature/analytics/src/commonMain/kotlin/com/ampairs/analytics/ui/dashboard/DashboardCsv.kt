package com.ampairs.analytics.ui.dashboard

import com.ampairs.analytics.domain.DashboardData

/**
 * Builds a machine-readable CSV of the dashboard aggregates (feature 022, T033). Non-composable, so
 * the caller (a composable that read `LocalAppLocale`) passes the resolved [periodLabel] and
 * [currencySymbol] in as plain strings — never format money/read the CompositionLocal here.
 *
 * Amounts are raw base-currency values (rupees); the symbol is recorded once in the header note so
 * the numeric columns stay parseable.
 */
fun buildDashboardCsv(
    data: DashboardData,
    periodLabel: String,
    currencySymbol: String,
): String {
    val sb = StringBuilder()
    sb.append("# Ampairs dashboard export\n")
    sb.append("# period,").append(csv(periodLabel)).append('\n')
    sb.append("# amounts_in,").append(csv(currencySymbol)).append("\n\n")

    val k = data.kpis
    sb.append("section,metric,value\n")
    sb.append("kpi,gross_sales,").append(k.grossSales).append('\n')
    sb.append("kpi,net_sales,").append(k.netSales).append('\n')
    sb.append("kpi,tax_collected,").append(k.totalTax).append('\n')
    sb.append("kpi,invoices,").append(k.invoiceCount).append('\n')
    sb.append("kpi,avg_invoice,").append(k.averageInvoiceValue).append('\n')
    sb.append("kpi,collections,").append(k.collectionsReceived).append('\n')
    sb.append("kpi,stock_value,").append(k.stockValue).append('\n')
    sb.append("kpi,low_stock_items,").append(k.lowStockCount).append('\n')
    sb.append("kpi,outstanding,").append(k.outstandingReceivable).append('\n')
    sb.append("kpi,inventory_turns,").append(k.inventoryTurns).append('\n')

    val g = data.gst
    sb.append("\nsection,gst,taxable,tax\n")
    sb.append("gst,intra_state,").append(g.taxableIntra).append(',').append(g.intraStateTax).append('\n')
    sb.append("gst,inter_state,").append(g.taxableInter).append(',').append(g.interStateTax).append('\n')
    g.byRate.forEach { r ->
        sb.append("gst_rate,").append(csv(r.taxCode)).append(',').append(r.taxable).append(',').append(r.tax).append('\n')
    }

    sb.append("\nsection,trend_date,total\n")
    data.trend.forEach { p -> sb.append("trend,").append(csv(p.bucket)).append(',').append(p.total).append('\n') }

    sb.append("\nsection,aging_bucket,invoices,amount\n")
    data.aging.buckets.forEach { b ->
        sb.append("aging,").append(csv(b.label)).append(',').append(b.count).append(',').append(b.amount).append('\n')
    }

    return sb.toString()
}

/** Minimal CSV field quoting: wrap in quotes and double any embedded quotes when needed. */
private fun csv(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
