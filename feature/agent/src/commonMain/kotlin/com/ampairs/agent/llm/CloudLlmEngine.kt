package com.ampairs.agent.llm

import co.touchlab.kermit.Logger
import com.ampairs.agent.llm.transport.LlmMessage
import com.ampairs.agent.llm.transport.LlmRequest
import com.ampairs.agent.llm.transport.LlmTransport
import com.ampairs.agent.llm.transport.ResponseFormat
import kotlin.concurrent.Volatile

/**
 * Cloud [LlmEngine] — runs the model **server-side** through an [LlmTransport] (the wire adapter).
 *
 * **Load/unload.** [load] resolves the transport by [ModelDescriptor.transportId] and stashes the
 * model + params (no file, no native memory); [close] drops them. [ProviderRegistry] owns the
 * lifecycle exactly as for on-device engines (lazy load on first use, close on model/workspace switch
 * or the background-unload policy).
 *
 * **Pipeline fit.** Implements [generate] / [generateConstrained] so it slots straight into the
 * existing resolver / text-to-SQL pipeline (`LlmIntentResolver` calls `generateConstrained` with the
 * [OutputSchema]). [supportsToolCalling] stays `false`, so the chat layer routes through the
 * orchestrator rather than the native streaming `chatStream` path — native streaming tool-calling is a
 * follow-up.
 */
class CloudLlmEngine(
    private val transports: Map<String, LlmTransport>,
) : LlmEngine {

    @Volatile private var transport: LlmTransport? = null
    @Volatile private var model: ModelDescriptor? = null
    @Volatile private var params: LlmParams = LlmParams()

    override suspend fun load(model: ModelDescriptor, params: LlmParams) {
        val transportId = model.transportId
            ?: throw IllegalStateException("Cloud model '${model.id}' has no transportId")
        val resolved = transports[transportId]
            ?: throw IllegalStateException("No transport '$transportId' registered for model '${model.id}'")
        this.transport = resolved
        this.model = model
        this.params = params
        Logger.i(tag = LOG_TAG) {
            "Cloud LLM ready: ${model.id} via '$transportId' (${model.remoteModelId})"
        }
    }

    override fun isLoaded(): Boolean = transport != null

    override suspend fun generate(prompt: String, maxTokens: Int): String =
        complete(prompt, schemaJson = null, maxTokens = maxTokens)

    override suspend fun generateConstrained(prompt: String, schema: OutputSchema, maxTokens: Int): String =
        complete(prompt, schemaJson = schema.toJsonSchema(), maxTokens = maxTokens)

    private suspend fun complete(prompt: String, schemaJson: String?, maxTokens: Int): String {
        val resolved = transport
            ?: throw IllegalStateException("CloudLlmEngine.generate() called before load()")
        val remoteModelId = model?.remoteModelId
            ?: throw IllegalStateException("Cloud model has no remoteModelId")
        val request = LlmRequest(
            remoteModelId = remoteModelId,
            messages = listOf(LlmMessage(role = "user", content = prompt)),
            maxTokens = if (maxTokens > 0) maxTokens else params.maxTokens,
            temperature = params.temperature,
            responseFormat = schemaJson?.let { ResponseFormat(jsonSchema = it) },
        )
        return resolved.complete(request).text
    }

    override suspend fun close() {
        transport = null
        model = null
    }

    private companion object {
        const val LOG_TAG = "AgentLlm"
    }
}
