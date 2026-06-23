package com.ampairs.agent.llm

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

    suspend fun close()
}
