package com.ampairs.customer.sync

import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER_TYPE)
class CustomerTypeSyncDelegate(
    private val customerTypeRepository: CustomerTypeRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER_TYPE

    override suspend fun pullFromServer(): SyncResult =
        customerTypeRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        customerTypeRepository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()
}
