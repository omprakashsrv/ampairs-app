package com.ampairs.cbstore.data.repository

import com.ampairs.cbstore.data.db.entity.toEntity
import com.ampairs.cbstore.data.db.entity.toStore
import com.ampairs.cbstore.data.db.entity.toZonalOffice
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.domain.model.ZonalOffice
import com.ampairs.cbstore.util.CbStoreLogger
import com.ampairs.cbstore.data.db.dao.StoreDao
import com.ampairs.cbstore.data.db.dao.ZonalOfficeDao
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for California Burrito outlets + zonal offices. The [CbStoreApi] is owned
 * by the sync delegates; writes here persist to Room as unsynced and mark the entity PENDING_PUSH.
 */
@OptIn(ExperimentalTime::class)
@Inject
class StoreRepository(
    private val storeDao: StoreDao,
    private val zonalOfficeDao: ZonalOfficeDao,
    private val syncStateDao: SyncStateDao,
) : StoreLookup {

    fun observeStores(): Flow<List<Store>> =
        storeDao.getAllStores().map { list -> list.map { it.toStore() } }

    fun observeStoresByZone(zoneId: String): Flow<List<Store>> =
        storeDao.getStoresByZone(zoneId).map { list -> list.map { it.toStore() } }

    fun observeZonalOffices(): Flow<List<ZonalOffice>> =
        zonalOfficeDao.getAllZonalOffices().map { list -> list.map { it.toZonalOffice() } }

    suspend fun saveStore(store: Store): Result<Store> = runCatching {
        require(store.uid.isNotBlank()) { "UID must be set by ViewModel" }
        storeDao.insertStore(store.toEntity().copy(synced = false))
        markPending(SyncEntity.CB_STORE)
        store
    }.onFailure { CbStoreLogger.e("StoreRepository", "saveStore failed", it) }

    suspend fun deleteStore(id: String): Result<Unit> = runCatching {
        val existing = storeDao.getStoreById(id)
        if (existing != null) {
            storeDao.insertStore(existing.copy(active = false, synced = false))
            markPending(SyncEntity.CB_STORE)
        }
        Unit
    }.onFailure { CbStoreLogger.e("StoreRepository", "deleteStore failed", it) }

    suspend fun saveZonalOffice(office: ZonalOffice): Result<ZonalOffice> = runCatching {
        require(office.uid.isNotBlank()) { "UID must be set by ViewModel" }
        zonalOfficeDao.insertZonalOffice(office.toEntity().copy(synced = false))
        markPending(SyncEntity.CB_ZONAL_OFFICE)
        office
    }.onFailure { CbStoreLogger.e("StoreRepository", "saveZonalOffice failed", it) }

    suspend fun deleteZonalOffice(id: String): Result<Unit> = runCatching {
        val existing = zonalOfficeDao.getZonalOfficeById(id)
        if (existing != null) {
            zonalOfficeDao.insertZonalOffice(existing.copy(active = false, synced = false))
            markPending(SyncEntity.CB_ZONAL_OFFICE)
        }
        Unit
    }.onFailure { CbStoreLogger.e("StoreRepository", "deleteZonalOffice failed", it) }

    // --- StoreLookup (cross-feature reads) --------------------------------------------------
    override suspend fun activeStores(): List<Store> =
        storeDao.getAllStores().first().map { it.toStore() }

    override suspend fun getStore(storeId: String): Store? =
        storeDao.getStoreById(storeId)?.toStore()

    override suspend fun zonalOfficeIdFor(storeId: String): String? =
        storeDao.getStoreById(storeId)?.zonalOfficeId

    override suspend fun activeZonalOffices(): List<ZonalOffice> =
        zonalOfficeDao.getAllZonalOffices().first().map { it.toZonalOffice() }

    private suspend fun markPending(entity: SyncEntity) {
        syncStateDao.markPendingPush(entity, Clock.System.now().toEpochMilliseconds())
    }
}
