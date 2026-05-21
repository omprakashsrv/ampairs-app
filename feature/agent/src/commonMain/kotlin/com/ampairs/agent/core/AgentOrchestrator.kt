package com.ampairs.agent.core

import com.ampairs.agent.di.OfflineIntentResolver
import com.ampairs.agent.di.OnlineIntentResolver
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
            is ResolvedIntent.Action -> {
                val result = actionRegistry.dispatch(intent.action)
                AgentResponse(
                    text = when (result) {
                        is ActionResult.Success -> result.summary
                        is ActionResult.Error -> result.message
                        is ActionResult.NeedsInput -> result.question
                    },
                    actionResult = result,
                    resolvedAction = intent.action,
                )
            }

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
}

data class AgentResponse(
    val text: String,
    val actionResult: ActionResult? = null,
    val resolvedAction: AgentAction? = null,
)
