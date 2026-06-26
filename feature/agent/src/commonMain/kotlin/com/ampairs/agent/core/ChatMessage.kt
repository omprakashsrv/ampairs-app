package com.ampairs.agent.core

import com.ampairs.common.agent.ReportRow
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = 0L,
    val actionResult: ActionResultSummary? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    /** Money total to render via `formatMoney(LocalAppLocale.current)` in the bubble (FR-013). */
    val amount: Double? = null,
    /** Ranked report lines (top customers/products/debtors); each rendered with `formatMoney`. */
    val rows: List<ReportRow>? = null,
)

@Serializable
data class ActionResultSummary(
    val actionType: String,
    val moduleName: String,
    val success: Boolean,
    val navigationRouteDescription: String? = null,
    val navigationRouteData: Map<String, String>? = null,
)
