package com.ampairs.agent.llm.transport

import dev.zacsweers.metro.MapKey

/**
 * The **model transport layer adapter**: the wire protocol for one cloud LLM provider. The
 * [com.ampairs.agent.llm.CloudLlmEngine] builds an engine-agnostic [LlmRequest]; a transport turns it
 * into an HTTP call and parses the reply into an [LlmResponse]. This keeps the provider/wire details
 * out of the engine, so a new online provider = one more transport (no engine change).
 *
 * Contributed into a `Map<String, LlmTransport>` via
 * `@Inject @ContributesIntoMap(WorkspaceScope::class) @TransportKey("<id>")`, and selected by
 * [com.ampairs.agent.llm.ModelDescriptor.transportId]. Transports live in the workspace graph
 * (matching `Set<LlmBackend>` and the `CloudLlmBackend` consumer); the per-workspace `X-Workspace-ID`
 * + JWT come from the shared `httpClient`.
 */
interface LlmTransport {
    val id: String

    /** One-shot (non-streaming) completion. Streaming is a follow-up. */
    suspend fun complete(request: LlmRequest): LlmResponse
}

/** Metro map key — contributes an [LlmTransport] into the transport map keyed by id (e.g. "ampairs"). */
@MapKey
annotation class TransportKey(val value: String)

/** One chat message in an [LlmRequest]. [role] is "system" | "user" | "assistant". */
data class LlmMessage(val role: String, val content: String)

/** Optional structured-output constraint: the JSON schema the reply must satisfy (text-to-SQL/intent). */
data class ResponseFormat(val jsonSchema: String)

/** Engine-agnostic completion request handed to a transport. */
data class LlmRequest(
    val remoteModelId: String,
    val messages: List<LlmMessage>,
    val maxTokens: Int,
    val temperature: Float,
    val responseFormat: ResponseFormat? = null,
)

/** Engine-agnostic completion result (the assistant's text). */
data class LlmResponse(val text: String)
