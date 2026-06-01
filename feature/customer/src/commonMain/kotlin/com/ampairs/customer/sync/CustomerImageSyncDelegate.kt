package com.ampairs.customer.sync

import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.repository.CustomerImageRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER_IMAGE)
class CustomerImageSyncDelegate(
    private val repository: CustomerImageRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER_IMAGE

    override suspend fun pullFromServer(): SyncResult =
        repository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        repository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        repository.syncCustomerImages(entityId).fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )
}
