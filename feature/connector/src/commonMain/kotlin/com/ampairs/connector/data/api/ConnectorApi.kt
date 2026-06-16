package com.ampairs.connector.data.api

import com.ampairs.common.model.Response
import com.ampairs.connector.domain.ConnectionTestRequest
import com.ampairs.connector.domain.ConnectionTestResult
import com.ampairs.connector.domain.ConnectorConfigDto
import com.ampairs.connector.domain.ConnectorInstallationDto
import com.ampairs.connector.domain.FieldMappingDto
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
    suspend fun installations(): Response<List<ConnectorInstallationDto>>
    suspend fun config(installationUid: String): Response<ConnectorConfigDto>
    suspend fun mappings(installationUid: String): Response<List<FieldMappingDto>>
    suspend fun upsert(
        installationUid: String,
        entityType: String,
        rows: List<SparseUpsertRow>,
    ): Response<List<SparseUpsertResult>>

    suspend fun putCheckpoint(installationUid: String, checkpoint: SyncCheckpointDto): Response<SyncCheckpointDto>
    suspend fun recordRun(installationUid: String, run: SyncRunDto): Response<SyncRunDto>
    suspend fun testConnection(installationUid: String, request: ConnectionTestRequest): Response<ConnectionTestResult>
}
