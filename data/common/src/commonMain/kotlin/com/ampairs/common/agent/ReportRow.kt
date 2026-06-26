package com.ampairs.common.agent

import kotlinx.serialization.Serializable

/**
 * One ranked line of a curated business report (e.g. a top-customer or top-product entry).
 * [amount] is a raw money value in the workspace's base currency — the chat bubble formats it with
 * `formatMoney(amount, LocalAppLocale.current)` so the currency symbol/grouping always follow the
 * active workspace's business currency (never hardcoded). See `/cmp-practices` §12.
 */
@Serializable
data class ReportRow(
    val label: String,
    val amount: Double,
)
