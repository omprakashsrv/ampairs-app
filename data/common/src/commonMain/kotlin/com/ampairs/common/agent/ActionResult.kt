package com.ampairs.common.agent

sealed class ActionResult {

    data class Success(
        val summary: String,
        val data: Any? = null,
        val navigationTarget: NavigationTarget? = null,
    ) : ActionResult()

    data class Error(val message: String) : ActionResult()

    data class NeedsInput(
        val question: String,
        val missingParams: List<String>,
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
    ) : ActionResult()
}

data class NavigationTarget(
    val routeDescription: String,
    val routeData: Map<String, String> = emptyMap(),
)
