package com.ampairs.common.agent

import kotlinx.serialization.Serializable

@Serializable
data class AgentAction(
    val actionType: ActionType,
    val moduleName: String,
    val params: Map<String, String> = emptyMap(),
)

/**
 * Reserved [AgentAction.params] key. A handler that returns [ActionResult.Confirm] sets this to
 * `"true"` on the `pendingAction`; on the confirm turn the orchestrator re-dispatches that action and
 * the handler — seeing the flag — persists instead of proposing again. Keeps money actions
 * confirm-before-write (FR-006).
 */
const val CONFIRMED_PARAM = "__confirmed"
