package com.ampairs.connector.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.postList
import com.ampairs.common.put
import com.ampairs.connector.domain.ConfigUpdateRequest
import com.ampairs.connector.domain.ConnectionTestRequest
import com.ampairs.connector.domain.ConnectionTestResult
import com.ampairs.connector.domain.ConnectorConfigDto
import com.ampairs.connector.domain.ConnectorInstallationDto
import com.ampairs.connector.domain.FieldMappingDto
import com.ampairs.connector.domain.SparseUpsertResult
import com.ampairs.connector.domain.SparseUpsertRow
import com.ampairs.connector.domain.SyncCheckpointDto
import com.ampairs.connector.domain.SyncRunDto
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ConnectorApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : ConnectorApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun installations(): Response<List<ConnectorInstallationDto>> =
        get(client, ApiUrlBuilder.connectorUrl("v1/installations"))

    override suspend fun sync(lastSync: String?, page: Int, size: Int): Response<PageResponse<ConnectorInstallationDto>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sort_by" to "updatedAt",
            "sort_dir" to "ASC",
        )
        lastSync?.takeIf { it.isNotBlank() }?.let { params["last_sync"] = it }
        return get(client, ApiUrlBuilder.connectorUrl("v1/sync"), params)
    }

    override suspend fun config(installationUid: String): Response<ConnectorConfigDto> =
        get(client, ApiUrlBuilder.connectorUrl("v1/installations/$installationUid/config"))

    override suspend fun updateConfig(installationUid: String, request: ConfigUpdateRequest): Response<ConnectorConfigDto> =
        put(client, ApiUrlBuilder.connectorUrl("v1/installations/$installationUid/config"), request)

    override suspend fun mappings(installationUid: String): Response<List<FieldMappingDto>> =
        get(client, ApiUrlBuilder.connectorUrl("v1/installations/$installationUid/mappings"))

    override suspend fun upsert(
        installationUid: String,
        entityType: String,
        rows: List<SparseUpsertRow>,
    ): Response<List<SparseUpsertResult>> =
        postList(client, ApiUrlBuilder.connectorUrl("v1/installations/$installationUid/data/$entityType/upsert"), rows)

    override suspend fun putCheckpoint(installationUid: String, checkpoint: SyncCheckpointDto): Response<SyncCheckpointDto> =
        put(client, ApiUrlBuilder.connectorUrl("v1/installations/$installationUid/checkpoints"), checkpoint)

    override suspend fun recordRun(installationUid: String, run: SyncRunDto): Response<SyncRunDto> =
        post(client, ApiUrlBuilder.connectorUrl("v1/installations/$installationUid/runs"), run)

    override suspend fun testConnection(installationUid: String, request: ConnectionTestRequest): Response<ConnectionTestResult> =
        post(client, ApiUrlBuilder.connectorUrl("v1/installations/$installationUid/config/test"), request)
}
