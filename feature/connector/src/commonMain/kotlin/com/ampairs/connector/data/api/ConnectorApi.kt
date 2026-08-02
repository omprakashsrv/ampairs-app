package com.ampairs.connector.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.connector.domain.CatalogueConnectorDto
import com.ampairs.connector.domain.ConfigUpdateRequest
import com.ampairs.connector.domain.ConnectionTestRequest
import com.ampairs.connector.domain.ConnectionTestResult
import com.ampairs.connector.domain.ConnectorConfigDto
import com.ampairs.connector.domain.ConnectorInstallationDto
import com.ampairs.connector.domain.FieldMappingDto
import com.ampairs.connector.domain.InstallConnectorRequest
import com.ampairs.connector.domain.SparseUpsertResult
import com.ampairs.connector.domain.SparseUpsertRow
import com.ampairs.connector.domain.SyncCheckpointDto
import com.ampairs.connector.domain.SyncRunDto

/**
 * Client for the backend connector platform (`/connector/v1/...`). Client-side connectors (Tally)
 * read their config/mapping from here and push mapped data via [upsert] — the dedicated connector
 * sparse-upsert endpoint (NOT the global `/sync`).
 */
interface ConnectorApi {
    /** Available connectors for this workspace (`GET /connector/v1/catalogue`) — drives browse/install. */
    suspend fun catalogue(): Response<List<CatalogueConnectorDto>>

    /** Install a connector into the current workspace (`POST /connector/v1/installations`). */
    suspend fun install(request: InstallConnectorRequest): Response<ConnectorInstallationDto>

    /** Uninstall an installation (`DELETE /connector/v1/installations/{uid}`). */
    suspend fun uninstall(installationUid: String): Response<Unit>

    suspend fun installations(): Response<List<ConnectorInstallationDto>>

    /**
     * Metadata pull feed for client mirroring (`GET /connector/v1/sync`) — installations changed
     * since [lastSync], including uninstalled rows so removals propagate. Config/mappings are
     * fetched per installation.
     */
    suspend fun sync(lastSync: String?, page: Int, size: Int): Response<PageResponse<ConnectorInstallationDto>>
    suspend fun config(installationUid: String): Response<ConnectorConfigDto>
    suspend fun updateConfig(installationUid: String, request: ConfigUpdateRequest): Response<ConnectorConfigDto>
    suspend fun pause(installationUid: String): Response<ConnectorInstallationDto>
    suspend fun resume(installationUid: String): Response<ConnectorInstallationDto>
    suspend fun mappings(installationUid: String): Response<List<FieldMappingDto>>

    /** Upsert the mapping for one entity type (`PUT /connector/v1/installations/{uid}/mappings`). */
    suspend fun updateMapping(installationUid: String, mapping: FieldMappingDto): Response<FieldMappingDto>
    suspend fun upsert(
        installationUid: String,
        entityType: String,
        rows: List<SparseUpsertRow>,
    ): Response<List<SparseUpsertResult>>

    suspend fun putCheckpoint(installationUid: String, checkpoint: SyncCheckpointDto): Response<SyncCheckpointDto>
    suspend fun recordRun(installationUid: String, run: SyncRunDto): Response<SyncRunDto>
    suspend fun testConnection(installationUid: String, request: ConnectionTestRequest): Response<ConnectionTestResult>
}
