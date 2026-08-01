package com.ampairs.analytics.sync

import com.ampairs.analytics.data.api.AnalyticsApi
import com.ampairs.analytics.data.api.toEntity
import com.ampairs.analytics.data.db.dao.DemandForecastDao
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns the demand-forecast ↔ server traffic via the pull-only `/api/analytics/v1/forecasts/sync`
 * contract. Forecasts are server-generated, so push is a no-op; the batched pull upserts the mirror
 * and advances the incremental `updatedAt` checkpoint.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.DEMAND_FORECAST)
class DemandForecastSyncDelegate(
    private val api: AnalyticsApi,
    private val demandForecastDao: DemandForecastDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.DEMAND_FORECAST

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    /** Forecasts are computed on the backend — nothing local is ever pushed. */
    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.DEMAND_FORECAST) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val page = api.getForecastsSync(lastSync, currentPage, batchSize, "updatedAt", "ASC")
                val rows = page.content
                for (server in rows) {
                    if (!server.active) demandForecastDao.deleteByUid(server.uid)
                    else demandForecastDao.upsert(server.toEntity())
                }
                if (rows.isNotEmpty()) {
                    val batchMax = rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
                    if (batchMax > maxServerTime) maxServerTime = batchMax
                    totalSynced += rows.size
                }
                currentPage++
            } while (page.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.DEMAND_FORECAST, maxServerTime)
            }
            Result.success(totalSynced)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
