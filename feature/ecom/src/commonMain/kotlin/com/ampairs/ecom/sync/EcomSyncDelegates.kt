package com.ampairs.ecom.sync

import com.ampairs.common.di.AppScope
import com.ampairs.ecom.data.repository.AddressRepository
import com.ampairs.ecom.data.repository.CatalogRepository
import com.ampairs.ecom.data.repository.EcomOrderRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/** Catalog incremental sync (cursor-based, pull-only). */
@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.ECOM_PRODUCT)
class EcomCatalogSyncDelegate(
    private val catalogRepository: CatalogRepository,
) : SyncDelegate {
    override val entity: SyncEntity = SyncEntity.ECOM_PRODUCT

    override suspend fun pullFromServer(): SyncResult =
        catalogRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

/** Saved-address push/pull (offline-first CRUD). */
@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.ECOM_ADDRESS)
class EcomAddressSyncDelegate(
    private val addressRepository: AddressRepository,
) : SyncDelegate {
    override val entity: SyncEntity = SyncEntity.ECOM_ADDRESS

    override suspend fun pullFromServer(): SyncResult =
        addressRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        addressRepository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

/** Order-history pull (orders are placed via checkout, never pushed). */
@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.ECOM_ORDER)
class EcomOrderSyncDelegate(
    private val orderRepository: EcomOrderRepository,
) : SyncDelegate {
    override val entity: SyncEntity = SyncEntity.ECOM_ORDER

    override suspend fun pullFromServer(): SyncResult =
        orderRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}
