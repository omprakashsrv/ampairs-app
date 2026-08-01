package com.ampairs.common.agent

sealed class ActionResult {

    data class Success(
        val summary: String,
        val data: Any? = null,
        val navigationTarget: NavigationTarget? = null,
        /** Optional money total for the UI to render via `formatMoney(LocalAppLocale.current)`. */
        val amount: Double? = null,
        /**
         * Optional ranked report lines (e.g. top customers/products/debtors). The bubble renders
         * each row's [ReportRow.amount] via `formatMoney(LocalAppLocale.current)` so currency follows
         * the active workspace — handlers never format money into text themselves.
         */
        val rows: List<ReportRow>? = null,
    ) : ActionResult()

    data class Error(val message: String) : ActionResult()

    data class NeedsInput(
        val question: String,
        val missingParams: List<String>,
    ) : ActionResult()

    /**
     * Multiple candidates found for a required parameter; user must pick one (FR-017).
     * The pending action carries the original request; handler adds resolved IDs to the
     * selected option. On selection, the orchestrator re-dispatches the action with the
     * selected ID filled in [pendingAction.params]. Works across text (tap), UI (buttons),
     * and voice (say number: "1", "2", "3").
     */
    data class Selection(
        val question: String,  // "Which customer?" or "Which product?"
        val paramName: String,  // e.g. "customer_id", "product_id" — the ambiguous param
        val options: List<SelectionOption>,
        val pendingAction: AgentAction,  // Action with unfilled/ambiguous param; awaits selection
    ) : ActionResult()

    /**
     * A money-mutating (or otherwise destructive) action that is fully resolved but **not yet
     * persisted** — the assistant must show [summary] and get explicit user confirmation first
     * (FR-006). On confirm, the orchestrator re-dispatches [pendingAction] (which carries
     * [CONFIRMED_PARAM] = "true" plus the already-resolved ids), and the handler persists then.
     * Nothing is written until that confirm turn.
     */
    data class Confirm(
        val summary: String,
        val pendingAction: AgentAction,
        /** Optional money total for the UI to render via `formatMoney(LocalAppLocale.current)`. */
        val amount: Double? = null,
    ) : ActionResult()
}

/**
 * A selectable option in a [Selection] result. Each option is a candidate for a single
 * parameter (e.g. a customer or product that matches the user's search hint).
 */
data class SelectionOption(
    val id: String,  // unique entity ID (customer UID, product UID, etc.)
    val label: String,  // primary display: "John Smith" or "Widget Pro"
    val secondaryLabel: String? = null,  // optional: phone, location, SKU, price, etc.
    val metadata: Map<String, Any> = emptyMap(),  // optional extra data (used for sorting/grouping)
)

data class NavigationTarget(
    val routeDescription: String,
    val routeData: Map<String, String> = emptyMap(),
)
