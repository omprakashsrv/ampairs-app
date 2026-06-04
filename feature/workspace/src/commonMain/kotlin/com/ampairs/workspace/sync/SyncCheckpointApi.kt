package com.ampairs.workspace.sync

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

/**
 * Fetches the workspace sync checkpoints (max `updatedAt` per entity) used by the bootstrap
 * reconciliation. The authenticated client automatically attaches the bearer token and the
 * `X-Workspace-ID` header via `defaultRequest`, so the call is workspace-scoped server-side.
 */
@Inject
@SingleIn(AppScope::class)
class SyncCheckpointApi(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) {
    private val client = httpClient(engine, tokenRepository)

    suspend fun getCheckpoints(): Response<SyncCheckpointResponse> =
        get(client, ApiUrlBuilder.eventUrl("v1/sync/checkpoints"))
}
