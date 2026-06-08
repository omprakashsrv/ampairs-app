package com.ampairs.store.data.repository

import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.entity.toEntity
import com.ampairs.store.data.db.entity.toStoreSetting
import com.ampairs.store.domain.model.StoreSetting
import com.ampairs.store.util.StoreLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for store settings. The API is owned by [com.ampairs.store.sync.StoreSyncDelegate];
 * writes here persist to Room as unsynced and flag STORE as PENDING_PUSH for the automatic bulk push.
 */
@OptIn(ExperimentalTime::class)
@Inject
class StoreSettingRepository(
    private val dao: StoreSettingDao,
    private val syncStateDao: SyncStateDao,
) {

    fun observeSettings(): Flow<List<StoreSetting>> =
        dao.observeAll().map { rows -> rows.map { it.toStoreSetting() } }

    suspend fun getByModuleKey(module: String, key: String): StoreSetting? =
        dao.getByModuleKey(module, key)?.toStoreSetting()

    /** Offline-first upsert: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun upsert(setting: StoreSetting): Result<StoreSetting> {
        return try {
            require(setting.uid.isNotBlank()) { "UID must be set by ViewModel" }
            dao.insert(setting.toEntity().copy(synced = false))
            markPending()
            Result.success(setting)
        } catch (e: Exception) {
            StoreLogger.e("StoreSettingRepository", "Failed to upsert setting ${setting.module}/${setting.key}", e)
            Result.failure(e)
        }
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.STORE, Clock.System.now().toEpochMilliseconds())
    }
}
