package com.ampairs.pricing.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.pricing.data.api.PricingApi
import com.ampairs.pricing.data.db.dao.GeoZoneDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toGeoZone
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.util.PricingLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/** Owns all geo-zone ↔ server traffic via the unified `/v1/geo-zones/sync` contract. */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.GEO_ZONE)
class GeoZoneSyncDelegate(
    private val api: PricingApi,
    private val geoZoneDao: GeoZoneDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.GEO_ZONE

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = geoZoneDao.getUnsyncedGeoZones()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0
            for (batch in unsynced.chunked(100)) {
                try {
                    api.bulkUpdateGeoZones(batch.map { it.toGeoZone() })
                    for (z in batch) {
                        if (!z.active) geoZoneDao.hardDeleteGeoZone(z.id)
                        else geoZoneDao.insertGeoZone(z.copy(synced = true))
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    PricingLogger.w("GeoZoneSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }
            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount geo zone(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            PricingLogger.e("GeoZoneSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.GEO_ZONE) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val page = api.getGeoZonesSync(lastSync, currentPage, batchSize, "updatedAt", "ASC")
                val rows: List<GeoZone> = page.content
                for (server in rows) {
                    val existing = geoZoneDao.getGeoZoneById(server.uid)
                    when {
                        existing != null && !existing.synced -> Unit
                        !server.active -> geoZoneDao.hardDeleteGeoZone(server.uid)
                        else -> geoZoneDao.insertGeoZone(server.toEntity().copy(synced = true))
                    }
                }
                if (rows.isNotEmpty()) {
                    val batchMax = rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
                    if (batchMax > maxServerTime) maxServerTime = batchMax
                    totalSynced += rows.size
                }
                currentPage++
            } while (page.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.GEO_ZONE, maxServerTime)
            }
            Result.success(totalSynced)
        } catch (e: Exception) {
            PricingLogger.e("GeoZoneSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }
}
