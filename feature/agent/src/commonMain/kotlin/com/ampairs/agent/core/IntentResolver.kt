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

    /**
     * Read-only, single-module SQL fallback when no typed action matched (FR-016). [sql] is a
     * model-proposed `SELECT` over [moduleName]'s curated schema; the orchestrator validates it
     * (`SafeSqlValidator`) before executing on a reader connection. Only an opt-in resolver (the
     * on-device LLM tier) emits this.
     */
    data class SafeQuery(val moduleName: String, val sql: String) : ResolvedIntent()

    /** Failed to parse */
    data class Error(val message: String) : ResolvedIntent()
}
