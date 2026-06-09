package com.ampairs.store.data.repository

import com.ampairs.store.data.api.StoreSettingApi
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import com.ampairs.store.data.db.entity.toDefinition
import com.ampairs.store.data.db.entity.toEntity
import com.ampairs.store.data.db.entity.toStoreSetting
import com.ampairs.store.domain.definition.StoreSettingDefinition
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
 * Local data access for store settings. Overrides are local-only and pushed by
 * [com.ampairs.store.sync.StoreSyncDelegate]; the [StoreSettingApi] is kept here ONLY for the
 * non-sync, UI-invoked definition catalog fetch (allowed exception — see /offline-sync).
 */
@OptIn(ExperimentalTime::class)
@Inject
class StoreSettingRepository(
    private val dao: StoreSettingDao,
    private val definitionDao: StoreSettingDefinitionDao,
    private val api: StoreSettingApi,
    private val syncStateDao: SyncStateDao,
) {

    fun observeSettings(): Flow<List<StoreSetting>> =
        dao.observeAll().map { rows -> rows.map { it.toStoreSetting() } }

    fun observeDefinitions(): Flow<List<StoreSettingDefinition>> =
        definitionDao.observeAll().map { rows -> rows.map { it.toDefinition() } }

    /** Fetch the workspace's definition catalog from the server and replace the local cache. */
    suspend fun refreshDefinitions(): Result<Int> {
        return try {
            val response = api.definitions()
            if (response.data != null && response.error == null) {
                val defs = response.data!!
                definitionDao.replaceAll(defs.map { it.toEntity() })
                Result.success(defs.size)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load definitions"))
            }
        } catch (e: Exception) {
            StoreLogger.e("StoreSettingRepository", "Failed to refresh definitions", e)
            Result.failure(e)
        }
    }

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
