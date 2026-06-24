package com.ampairs.agent.offline

import co.touchlab.kermit.Logger
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.core.ResolvedIntent
import com.ampairs.common.agent.ActionDescriptor

/**
 * The `@OfflineIntentResolver` (T030, FR-004/FR-012): use the on-device [LlmIntentResolver] when a
 * model is loaded, otherwise fall back to the regex [RuleBasedIntentResolver]. Also falls back to the
 * rule-based resolver if the LLM path throws, so the assistant degrades gracefully on a low-RAM
 * device or a model load failure rather than erroring.
 *
 * [isLlmReady] is supplied by `ProviderRegistry` (T023); kept as a lambda so the composite stays
 * engine-agnostic and unit-testable without DI.
 */
class CompositeOfflineResolver(
    private val llm: IntentResolver,
    private val rule: IntentResolver,
    private val isLlmReady: suspend () -> Boolean,
) : IntentResolver {

    override suspend fun resolve(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        availableActions: List<ActionDescriptor>,
    ): ResolvedIntent {
        if (isLlmReady()) {
            runCatching { llm.resolve(userMessage, conversationHistory, availableActions) }
                .onSuccess {
                    Logger.i(tag = LOG_TAG) { "Resolved via on-device LLM (${it::class.simpleName})" }
                    return it
                }
                .onFailure { e ->
                    // Surface why we silently dropped to rule-based — otherwise a loaded model that
                    // throws on every inference looks exactly like "the model isn't being used".
                    Logger.e(throwable = e, tag = LOG_TAG) { "On-device LLM resolve failed — falling back to rule-based" }
                }
        } else {
            Logger.i(tag = LOG_TAG) { "On-device LLM not ready — using rule-based resolver" }
        }
        return rule.resolve(userMessage, conversationHistory, availableActions)
    }

    private companion object {
        const val LOG_TAG = "AgentLlm"
    }
}
