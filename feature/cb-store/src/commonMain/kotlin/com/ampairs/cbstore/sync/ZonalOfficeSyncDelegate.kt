package com.ampairs.cbstore.sync

import com.ampairs.cbstore.data.api.CbStoreApi
import com.ampairs.cbstore.data.db.dao.ZonalOfficeDao
import com.ampairs.cbstore.data.db.entity.toEntity
import com.ampairs.cbstore.data.db.entity.toZonalOffice
import com.ampairs.cbstore.domain.model.ZonalOffice
import com.ampairs.cbstore.util.CbStoreLogger
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/** Owns all ZonalOffice ↔ server traffic (canonical `/cb_store/v1/zonal-offices/sync`). */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_ZONAL_OFFICE)
class ZonalOfficeSyncDelegate(
    private val api: CbStoreApi,
    private val zonalOfficeDao: ZonalOfficeDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CB_ZONAL_OFFICE

    override suspend fun pullFromServer(): SyncResult =
        pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    private suspend fun pushPending(): Result<Int> = runCatching {
        val unsynced = zonalOfficeDao.getUnsyncedZonalOffices()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.bulkUpdateZonalOffices(batch.map { it.toZonalOffice() })
                for (e in batch) {
                    if (!e.active) zonalOfficeDao.hardDeleteZonalOffice(e.id)
                    else zonalOfficeDao.insertZonalOffice(e.copy(synced = true))
                }
                synced += batch.size
            } catch (err: Exception) {
                CbStoreLogger.w("ZonalOfficeSyncDelegate", "Batch push failed", err)
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) throw Exception("$failed zonal office(s) failed to push") else synced
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_ZONAL_OFFICE) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        do {
            val resp = api.getZonalOfficesSync(lastSync, page, batchSize, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { server ->
                    val existing = zonalOfficeDao.getZonalOfficeById(server.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !server.active -> { zonalOfficeDao.hardDeleteZonalOffice(server.uid); null }
                        else -> server.toEntity().copy(synced = true)
                    }
                }
                if (toInsert.isNotEmpty()) zonalOfficeDao.insertZonalOffices(toInsert)
                val batchMax = maxUpdatedAt(rows)
                if (batchMax > maxTime) maxTime = batchMax
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < 10000)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_ZONAL_OFFICE, maxTime)
        total
    }

    private fun maxUpdatedAt(rows: List<ZonalOffice>): String =
        rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
}
