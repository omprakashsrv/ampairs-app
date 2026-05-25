package com.ampairs.agent.core

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.AgentAction

/**
 * Resolves natural language text into an AgentAction.
 *
 * Two implementations:
 * - LlmIntentResolver: Online, uses Claude tool_use via backend proxy
 * - RuleBasedIntentResolver: Offline fallback with regex patterns
 */
interface IntentResolver {

    suspend fun resolve(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        availableActions: List<ActionDescriptor>,
    ): ResolvedIntent
}

sealed class ResolvedIntent {

    /** Successfully parsed into an action */
    data class Action(val action: AgentAction) : ResolvedIntent()

    /** Clarification needed from the user */
    data class Clarification(val question: String) : ResolvedIntent()

    /** General conversation (not an action request) */
    data class Conversation(val response: String) : ResolvedIntent()

    /** Failed to parse */
    data class Error(val message: String) : ResolvedIntent()
}
