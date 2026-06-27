package com.ampairs.supplier.data.repository

import dev.zacsweers.metro.Inject
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.supplier.data.db.toDomain
import com.ampairs.supplier.data.db.toEntity
import com.ampairs.supplier.domain.Supplier
import com.ampairs.supplier.domain.SupplierListItem
import com.ampairs.supplier.domain.toListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.ampairs.supplier.util.SupplierConstants.ERROR_SUPPLIER_UID_REQUIRED
import com.ampairs.common.cache.CacheCleanable
import com.ampairs.supplier.util.SupplierLogger

/**
 * Local-only data access for suppliers. This layer NEVER touches [SupplierApi] — the API is a
 * separate concern owned by the sync layer ([SupplierSyncDelegate], driven by CentralSyncService).
 *
 * Every write goes to Room with `synced = false` and then marks the SUPPLIER entity as
 * PENDING_PUSH via [SyncStateDao]. CentralSyncService's reactive observer picks that up and runs
 * the bulk push automatically. Pulls and pushes live in the delegate, not here.
 */
@OptIn(ExperimentalTime::class)
@Inject
class SupplierRepository(
    private val supplierDao: SupplierDao,
    private val syncStateDao: SyncStateDao,
) : CacheCleanable {

    fun observeSuppliers(): Flow<List<SupplierListItem>> {
        return supplierDao.getAllSuppliers()
            .map { entities -> entities.map { entity -> entity.toDomain().toListItem() } }
    }

    fun observeSupplier(supplierId: String): Flow<Supplier?> {
        return supplierDao.observeSupplierById(supplierId)
            .map { it?.toDomain() }
    }

    /** Combined search filter across name / phone / email / gst / state. Blank query means "no filter". */
    fun filterSuppliers(query: String): Flow<List<SupplierListItem>> {
        return supplierDao.filterSuppliers(searchQuery = query)
            .map { entities -> entities.map { it.toDomain().toListItem() } }
    }

    suspend fun getSupplier(supplierId: String): Supplier? {
        return supplierDao.getSupplierById(supplierId)?.toDomain()
    }

    override suspend fun clearCache() { supplierDao.clearWorkspaceSuppliers() }

    /** Reactive count of locally unsynced rows — drives PendingPush status in CentralSyncService. */
    fun observeUnsyncedCount(): Flow<Int> = supplierDao.observeUnsyncedCount()

    suspend fun getSupplierCount(): Int = supplierDao.getSupplierCount()

    /**
     * Offline-first create: persist locally as unsynced and flag the entity for an automatic
     * bulk push. The UID must already be set by the ViewModel.
     */
    suspend fun createSupplier(supplier: Supplier): Result<Supplier> {
        require(supplier.uid.isNotBlank()) { ERROR_SUPPLIER_UID_REQUIRED }
        supplierDao.insertSupplier(supplier.toEntity().copy(synced = false))
        markPending()
        SupplierLogger.i("SupplierRepository", "✅ Supplier saved locally (unsynced): ${supplier.uid}")
        return Result.success(supplier)
    }

    /** Offline-first update: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun updateSupplier(supplier: Supplier): Result<Supplier> {
        supplierDao.insertSupplier(supplier.toEntity().copy(synced = false))
        markPending()
        SupplierLogger.i("SupplierRepository", "✅ Supplier updated locally (unsynced): ${supplier.uid}")
        return Result.success(supplier)
    }

    /**
     * Offline-first delete: mark inactive + unsynced locally and flag for automatic bulk push.
     * The delegate's push sends the soft-deleted row and then hard-deletes the local row.
     */
    suspend fun deleteSupplier(supplierId: String): Result<Unit> {
        val supplier = supplierDao.getSupplierById(supplierId)
        if (supplier != null) {
            supplierDao.insertSupplier(supplier.copy(active = false, synced = false))
            markPending()
            SupplierLogger.i("SupplierRepository", "✅ Supplier marked deleted locally (unsynced): $supplierId")
        } else {
            SupplierLogger.w("SupplierRepository", "⚠️ Supplier not found in local database: $supplierId")
        }
        return Result.success(Unit)
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.SUPPLIER, Clock.System.now().toEpochMilliseconds())
    }
}
