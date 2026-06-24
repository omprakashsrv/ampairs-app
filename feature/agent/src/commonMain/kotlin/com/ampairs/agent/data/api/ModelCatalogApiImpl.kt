package com.ampairs.agent.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelCatalogApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : ModelCatalogApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun catalog(): Response<List<RemoteModelDescriptor>> =
        get(client, ApiUrlBuilder.workspaceUrl("v1/llm-models/catalog"))
}
