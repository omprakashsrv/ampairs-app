package com.ampairs.agent.offline

import co.touchlab.kermit.Logger
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.core.ResolvedIntent
import com.ampairs.common.agent.ActionDescriptor

/**
 * The `@OfflineIntentResolver` (T030, FR-004/FR-012).
 *
 * **No silent fall-back to rule-based.** When the user has an LLM/cloud model selected, that model IS
 * the assistant — we never quietly drop to the regex [RuleBasedIntentResolver] just because the engine
 * is momentarily not loaded (e.g. it was unloaded when the app went to the background) or a single
 * inference failed (e.g. a cloud network hiccup). Those surface to the user instead:
 *  - selected but not loaded yet → a "still getting ready, please retry" clarification, while the
 *    engine reloads lazily underneath;
 *  - selected and the inference throws → an error message.
 *
 * The rule-based resolver only runs when **no** LLM model is selected — i.e. it is a separate, explicit
 * choice (the baseline when the user hasn't picked/loaded any model), not an automatic degradation.
 *
 * [isLlmReady] / [isLlmSelected] are supplied by `ProviderRegistry` (T023) as lambdas so the composite
 * stays engine-agnostic and unit-testable without DI.
 */
class CompositeOfflineResolver(
    private val llm: IntentResolver,
    private val rule: IntentResolver,
    private val isLlmReady: suspend () -> Boolean,
    private val isLlmSelected: suspend () -> Boolean,
) : IntentResolver {

    override suspend fun resolve(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        availableActions: List<ActionDescriptor>,
    ): ResolvedIntent {
        // Bias toward "a model is selected" if the check itself fails, so a transient error never
        // silently demotes the user's chosen model to the rule-based resolver.
        val selected = runCatching { isLlmSelected() }.getOrDefault(true)
        if (selected) {
            val ready = runCatching { isLlmReady() }.getOrDefault(false)
            if (!ready) {
                Logger.i(tag = LOG_TAG) { "Selected LLM not ready yet — asking to retry (no rule-based fallback)" }
                return ResolvedIntent.Clarification(MODEL_NOT_READY)
            }
            return runCatching { llm.resolve(userMessage, conversationHistory, availableActions) }
                .onSuccess { Logger.i(tag = LOG_TAG) { "Resolved via selected LLM (${it::class.simpleName})" } }
                .getOrElse { e ->
                    Logger.e(throwable = e, tag = LOG_TAG) { "Selected LLM failed — surfacing error (no rule-based fallback)" }
                    ResolvedIntent.Error(MODEL_FAILED)
                }
        }
        // No LLM model selected → the rule-based resolver is the active engine (an explicit choice).
        Logger.i(tag = LOG_TAG) { "No LLM model selected — using rule-based resolver" }
        return rule.resolve(userMessage, conversationHistory, availableActions)
    }

    private companion object {
        const val LOG_TAG = "AgentLlm"
        const val MODEL_NOT_READY =
            "The assistant model is still getting ready — please try again in a moment."
        const val MODEL_FAILED =
            "The assistant couldn't process that right now. Please try again."
    }
}
