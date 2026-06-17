package com.ampairs.connector.sync

import co.touchlab.kermit.Logger
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.connector.data.api.ConnectorApi
import com.ampairs.connector.domain.ConnectorInstallationDto
import com.ampairs.connector.domain.ConnectorMirrorStore
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Pull-only offline mirror of the backend connector metadata (spec 013, US5). Refreshes the
 * workspace's installation cache from `GET /connector/v1/sync` on sync events, app restart, and
 * backend WebSocket events. Connector metadata is authored on the backend (and via the
 * config/mapping endpoints) — there is nothing to push from this cache.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CONNECTOR)
class ConnectorSyncDelegate(
    private val api: ConnectorApi,
    private val mirror: ConnectorMirrorStore,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CONNECTOR

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    /** Page through the connector metadata feed and refresh the offline installation cache. */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val all = mutableListOf<ConnectorInstallationDto>()
            var page = 0
            do {
                val response = api.sync(lastSync = null, page = page, size = batchSize)
                if (response.error != null) throw Exception(response.error?.message ?: "Network error")
                val batch = response.data?.content ?: emptyList()
                all += batch
                page++
            } while (batch.size == batchSize && all.size < 10000)
            mirror.cacheInstallations(all)
            Result.success(all.size)
        } catch (e: Exception) {
            log.e(e) { "Connector mirror pull failed" }
            Result.failure(e)
        }
    }

    private companion object {
        val log = Logger.withTag("ConnectorSyncDelegate")
    }
}
