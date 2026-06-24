package com.ampairs.agent.core

import com.ampairs.agent.di.OfflineIntentResolver
import com.ampairs.agent.di.OnlineIntentResolver
import com.ampairs.agent.query.SafeQueryService
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import dev.zacsweers.metro.Inject

/**
 * Coordinates the full pipeline: input -> intent -> action -> result -> response.
 * Handles online/offline mode transparently.
 *
 * DI-agnostic: depends only on interfaces from agent/core.
 */
@Inject
class AgentOrchestrator(
    private val actionRegistry: ActionRegistry,
    @OnlineIntentResolver private val onlineResolver: IntentResolver,
    @OfflineIntentResolver private val offlineResolver: IntentResolver,
    private val safeQueryService: SafeQueryService,
) {

    /**
     * Process a user message and return a response.
     * Tries online resolver first, falls back to offline on failure.
     */
    suspend fun processMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        isOnline: Boolean,
    ): AgentResponse {
        val availableActions = actionRegistry.getAllActions()

        // The on-device LLM is the assistant's brain. The offline resolver is the
        // CompositeOfflineResolver: it uses the loaded model when ready and already falls back to the
        // rule-based resolver when no model is present or inference fails. We therefore route every
        // message through it regardless of [isOnline] — the previous online-first branch sent every
        // message to the rule-based stub while online, so a downloaded/loaded model was never used.
        // The rule-based [onlineResolver] stays as a last-resort guard if the offline path itself
        // throws (it shouldn't — the composite catches LLM errors internally).
        val intent = try {
            offlineResolver.resolve(userMessage, conversationHistory, availableActions)
        } catch (e: Exception) {
            onlineResolver.resolve(userMessage, conversationHistory, availableActions)
        }

        return when (intent) {
            is ResolvedIntent.Action -> respondToResult(actionRegistry.dispatch(intent.action), intent.action)

            is ResolvedIntent.Clarification -> {
                AgentResponse(text = intent.question)
            }

            is ResolvedIntent.Conversation -> {
                AgentResponse(text = intent.response)
            }

            is ResolvedIntent.SafeQuery -> {
                // Read-only SQL fallback (FR-016): validate + execute on the module's reader connection.
                AgentResponse(text = safeQueryService.run(intent.moduleName, intent.sql).toText())
            }

            is ResolvedIntent.Error -> {
                AgentResponse(text = "I had trouble understanding that. ${intent.message}")
            }
        }
    }

    /**
     * Execute a user-confirmed pending action (from a prior [ActionResult.Confirm]). The action
     * already carries `CONFIRMED_PARAM = "true"` and the resolved ids, so the handler persists this
     * time. This is the only path that turns a proposed money action into a written one (FR-006).
     */
    suspend fun confirmAction(pendingAction: AgentAction): AgentResponse =
        respondToResult(actionRegistry.dispatch(pendingAction), pendingAction)

    private fun respondToResult(result: ActionResult, action: AgentAction): AgentResponse =
        AgentResponse(
            text = when (result) {
                is ActionResult.Success -> result.summary
                is ActionResult.Error -> result.message
                is ActionResult.NeedsInput -> result.question
                is ActionResult.Confirm -> result.summary
            },
            actionResult = result,
            resolvedAction = action,
            // Surfaced so the ViewModel can render confirm/cancel and re-dispatch on confirm.
            pendingConfirmation = (result as? ActionResult.Confirm)?.pendingAction,
            amount = (result as? ActionResult.Confirm)?.amount ?: (result as? ActionResult.Success)?.amount,
        )
}

data class AgentResponse(
    val text: String,
    val actionResult: ActionResult? = null,
    val resolvedAction: AgentAction? = null,
    /** Non-null when the action needs explicit user confirmation before it will persist (FR-006). */
    val pendingConfirmation: AgentAction? = null,
    /** Money total for the UI to format via `formatMoney(LocalAppLocale.current)` (FR-013). */
    val amount: Double? = null,
)
