package com.ampairs.agent.llm.transport

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Primary cloud transport: routes completions through the **Ampairs backend proxy**
 * (`POST /agent/v1/chat/completions`). The backend holds the provider key and forwards to the hosted
 * model — keys stay server-side, JWT + `X-Workspace-ID` ride along via the shared [httpClient] (same
 * pattern as `ChatLogApiImpl`), and the model lineup can change without an app release.
 */
@Inject
@SingleIn(WorkspaceScope::class)
@ContributesIntoMap(WorkspaceScope::class)
@TransportKey(AmpairsProxyTransport.ID)
class AmpairsProxyTransport(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : LlmTransport {

    override val id: String = ID

    private val client = httpClient(engine, tokenRepository)

    override suspend fun complete(request: LlmRequest): LlmResponse {
        val body = ChatCompletionRequest(
            model = request.remoteModelId,
            messages = request.messages.map { ChatMessageDto(role = it.role, content = it.content) },
            maxTokens = request.maxTokens,
            temperature = request.temperature,
            responseFormat = request.responseFormat?.let {
                ResponseFormatDto(type = "json_schema", schema = it.jsonSchema)
            },
        )
        val response: Response<ChatCompletionResponse> =
            post(client, ApiUrlBuilder.agentUrl("v1/chat/completions"), body)
        val data = response.data
        if (data != null && response.error == null) {
            return LlmResponse(data.content)
        }
        throw IllegalStateException(
            response.error?.message?.ifBlank { null } ?: "Cloud completion failed",
        )
    }

    companion object {
        const val ID: String = "ampairs"
    }
}

/** Request body for `POST /agent/v1/chat/completions` (snake_case, server-defined). */
@Serializable
data class ChatCompletionRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessageDto>,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("temperature") val temperature: Float,
    @SerialName("response_format") val responseFormat: ResponseFormatDto? = null,
)

@Serializable
data class ChatMessageDto(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String,
)

@Serializable
data class ResponseFormatDto(
    @SerialName("type") val type: String,
    @SerialName("schema") val schema: String,
)

/** Response body — the assistant's completion text plus the resolved provider model id. */
@Serializable
data class ChatCompletionResponse(
    @SerialName("content") val content: String = "",
    @SerialName("model_id") val modelId: String? = null,
)
