package com.ampairs.supplier.domain

import dev.zacsweers.metro.Inject
import com.ampairs.supplier.data.repository.SupplierRepository
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent

@Inject
class SupplierStore(
    private val repository: SupplierRepository,
    private val syncService: CentralSyncService,
) {

    fun observeSuppliers() = repository.observeSuppliers()

    fun filterSuppliers(query: String) = repository.filterSuppliers(query)

    fun observeSupplier(supplierId: String) = repository.observeSupplier(supplierId)

    suspend fun getSupplier(supplierId: String): Supplier? = repository.getSupplier(supplierId)

    suspend fun createSupplier(supplier: Supplier): Result<Supplier> = repository.createSupplier(supplier)

    suspend fun updateSupplier(supplier: Supplier): Result<Supplier> = repository.updateSupplier(supplier)

    suspend fun deleteSupplier(supplierId: String): Result<Unit> = repository.deleteSupplier(supplierId)

    /** Manual refresh: fire a full (push + pull) sync through the central coordinator. */
    fun syncSuppliers() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.SUPPLIER))
}
