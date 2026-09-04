package com.ampairs.cbstore.sync

import com.ampairs.cbstore.data.api.CbStoreApi
import com.ampairs.cbstore.data.db.dao.StoreDao
import com.ampairs.cbstore.data.db.entity.toEntity
import com.ampairs.cbstore.data.db.entity.toStore
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.util.CbStoreLogger
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/** Owns all Store ↔ server traffic (canonical `/cb_store/v1/stores/sync`). */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_STORE)
class StoreSyncDelegate(
    private val api: CbStoreApi,
    private val storeDao: StoreDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CB_STORE

    // A store references its zonal office; pull/push the offices first.
    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.CB_ZONAL_OFFICE)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { refreshOne(entityId) }.fold({ SyncResult.Success(1) }, { SyncResult.Failure(it) })

    private suspend fun pushPending(): Result<Int> = runCatching {
        val unsynced = storeDao.getUnsyncedStores()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.bulkUpdateStores(batch.map { it.toStore() })
                for (e in batch) if (!e.active) storeDao.hardDeleteStore(e.id) else storeDao.insertStore(e.copy(synced = true))
                synced += batch.size
            } catch (err: Exception) {
                CbStoreLogger.w("StoreSyncDelegate", "Batch push failed", err)
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) throw Exception("$failed store(s) failed to push") else synced
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_STORE) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        do {
            val resp = api.getStoresSync(lastSync, page, batchSize, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { server ->
                    val existing = storeDao.getStoreById(server.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !server.active -> { storeDao.hardDeleteStore(server.uid); null }
                        else -> server.toEntity().copy(synced = true)
                    }
                }
                if (toInsert.isNotEmpty()) storeDao.insertStores(toInsert)
                val batchMax = maxUpdatedAt(rows)
                if (batchMax > maxTime) maxTime = batchMax
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < 10000)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_STORE, maxTime)
        total
    }

    private suspend fun refreshOne(id: String) {
        val resp = api.getStoreById(id)
        if (resp.data != null && resp.error == null) {
            val existing = storeDao.getStoreById(id)
            if (existing == null || existing.synced) storeDao.insertStore(resp.data!!.toEntity().copy(synced = true))
        }
    }

    private fun maxUpdatedAt(rows: List<Store>): String =
        rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
}
