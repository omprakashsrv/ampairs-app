package com.ampairs.agent.llm

import com.ampairs.agent.core.AgentStreamEvent
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The single `commonMain` port the pipeline sees for on-device generation. Each platform contributes
 * one or more adapters (LiteRtLmEngine primary; LlamaCppEngine fallback) selected at runtime by the
 * `ProviderRegistry` from `PlatformDefaults` + `AssistantConfig`. The resolver only ever calls
 * [generateConstrained] with an [OutputSchema]; the adapter renders the schema to its native
 * constraint format (JSON-schema / function-calling for LiteRT-LM, GBNF for llama.cpp).
 */
interface LlmEngine {
    suspend fun load(model: ModelDescriptor, params: LlmParams = model.defaultParams)
    fun isLoaded(): Boolean

    /** Free-form generation (e.g. phrasing a query result). */
    suspend fun generate(prompt: String, maxTokens: Int = 512): String

    /** Constrained generation — output is forced to satisfy [schema] (a valid AgentAction JSON). */
    suspend fun generateConstrained(prompt: String, schema: OutputSchema, maxTokens: Int = 512): String

    /**
     * True when this engine supports **native tool-calling** (LiteRT-LM `ToolSet`/`@Tool`): the model
     * streams a clean prose reply and invokes tools for actions, instead of us prompting for intent
     * JSON. When true, the chat layer uses [chatStream]; otherwise it stays on the resolver pipeline.
     */
    val supportsToolCalling: Boolean get() = false

    /**
     * Stream a Gallery-style tool-calling turn: emits [AgentStreamEvent.TextDelta]s as the model
     * types, and dispatches tool calls through [dispatch], emitting [AgentStreamEvent.ActionProposed]
     * / [AgentStreamEvent.ActionExecuted]. [recentHistory] is pre-rendered "User:"/"Assistant:" lines
     * used to seed context on the **first** turn of a session; after that the engine keeps the
     * conversation (and its KV cache) alive across turns, so prior turns are reused rather than
     * re-prefilled. Default: empty (engine has no tools).
     */
    fun chatStream(
        userMessage: String,
        recentHistory: List<String>,
        dispatch: suspend (AgentAction) -> ActionResult,
    ): Flow<AgentStreamEvent> = emptyFlow()

    /**
     * Drop any retained chat conversation/KV-cache so the next [chatStream] turn starts a fresh
     * session (call when the user clears the chat). Default no-op for engines that keep no state.
     */
    fun resetChat() {}

    suspend fun close()
}
