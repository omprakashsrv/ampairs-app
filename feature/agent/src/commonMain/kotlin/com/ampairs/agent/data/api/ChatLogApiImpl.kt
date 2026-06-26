package com.ampairs.agent.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ChatLogApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : ChatLogApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun upload(logs: List<ChatLogRequest>): Response<ChatLogUploadResponse> =
        post(client, ApiUrlBuilder.agentUrl("v1/chat-logs"), logs)
}
