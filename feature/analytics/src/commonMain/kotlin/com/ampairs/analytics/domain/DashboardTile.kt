package com.ampairs.analytics.domain

/**
 * A configurable KPI tile on the dashboard (feature 022, T050). [key] is the stable token persisted in
 * the `analytics/dashboard_layout` workspace setting (a CSV of enabled keys, in display order); the
 * dashboard renders exactly the tiles in the stored list, in that order.
 */
enum class DashboardTile(val key: String) {
    GROSS_SALES("gross_sales"),
    NET_SALES("net_sales"),
    TAX("tax"),
    INVOICES("invoices"),
    AVG_INVOICE("avg_invoice"),
    COLLECTIONS("collections"),
    STOCK_VALUE("stock_value"),
    LOW_STOCK("low_stock"),
    OUTSTANDING("outstanding"),
    INVENTORY_TURNS("inventory_turns"),
    ;

    companion object {
        /** Canonical full layout — the default when no override is stored. */
        val DEFAULT_ORDER: List<DashboardTile> = entries

        fun fromKey(key: String): DashboardTile? = entries.firstOrNull { it.key == key }

        /** Decode the stored CSV to an ordered tile list; blank/unknown → the default full layout. */
        fun decode(csv: String?): List<DashboardTile> {
            if (csv.isNullOrBlank()) return DEFAULT_ORDER
            val parsed = csv.split(",").mapNotNull { fromKey(it.trim()) }
            return parsed.ifEmpty { DEFAULT_ORDER }
        }

        /** Encode an ordered tile list back to the stored CSV. */
        fun encode(tiles: List<DashboardTile>): String = tiles.joinToString(",") { it.key }
    }
}
