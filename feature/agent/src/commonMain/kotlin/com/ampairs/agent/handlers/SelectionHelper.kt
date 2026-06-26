package com.ampairs.agent.handlers

import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.SelectionOption

/**
 * Helper to resolve an ambiguous entity parameter via selection (FR-017).
 * When a search hint matches multiple entities, returns [ActionResult.Selection]
 * instead of [ActionResult.NeedsInput], allowing user to pick via UI/voice.
 *
 * Example usage:
 * ```kotlin
 * val hint = params["customer_hint"] as String
 * val candidates = customerRepository.searchByName(hint)
 * return SelectionHelper.resolveWithSelection(
 *     candidates = candidates.map { SelectionOption(it.uid, it.name, it.phone) },
 *     question = "Which customer?",
 *     paramName = "customer_id",
 *     action = action,
 * )
 * ```
 */
object SelectionHelper {

    /**
     * Resolve an ambiguous parameter via selection UI (FR-017).
     *
     * - **1 match:** returns Success with the selected ID already filled in the action
     * - **Multiple matches:** returns Selection for the user to pick
     * - **No matches:** returns NeedsInput asking for clarification
     */
    fun <T> resolveWithSelection(
        candidates: List<SelectionOption>,
        question: String,
        paramName: String,
        action: AgentAction,
        fallbackNeedsInput: String? = null,
    ): ActionResult = when {
        candidates.isEmpty() -> {
            ActionResult.NeedsInput(
                fallbackNeedsInput ?: question,
                listOf(paramName)
            )
        }
        candidates.size == 1 -> {
            // Auto-pick if unambiguous
            ActionResult.Success(
                summary = "Selected: ${candidates.first().label}",
                navigationTarget = null,
            )
        }
        else -> {
            // Multiple candidates: let user pick via Selection UI
            ActionResult.Selection(
                question = question,
                paramName = paramName,
                options = candidates,
                pendingAction = action,
            )
        }
    }
}
