package com.ampairs.agent.core

import com.ampairs.agent.di.OfflineIntentResolver
import com.ampairs.agent.di.OnlineIntentResolver
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

        val intent = if (isOnline) {
            try {
                onlineResolver.resolve(userMessage, conversationHistory, availableActions)
            } catch (e: Exception) {
                // Network failed — fall back to offline
                offlineResolver.resolve(userMessage, conversationHistory, availableActions)
            }
        } else {
            offlineResolver.resolve(userMessage, conversationHistory, availableActions)
        }

        return when (intent) {
            is ResolvedIntent.Action -> respondToResult(actionRegistry.dispatch(intent.action), intent.action)

            is ResolvedIntent.Clarification -> {
                AgentResponse(text = intent.question)
            }

            is ResolvedIntent.Conversation -> {
                AgentResponse(text = intent.response)
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
        )
}

data class AgentResponse(
    val text: String,
    val actionResult: ActionResult? = null,
    val resolvedAction: AgentAction? = null,
    /** Non-null when the action needs explicit user confirmation before it will persist (FR-006). */
    val pendingConfirmation: AgentAction? = null,
)
