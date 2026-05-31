package com.ampairs.customer.sync

import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER_GROUP)
class CustomerGroupSyncDelegate(
    private val customerGroupRepository: CustomerGroupRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER_GROUP

    override suspend fun pullFromServer(): SyncResult =
        customerGroupRepository.syncCustomerGroups().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        customerGroupRepository.syncCustomerGroups().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()
}
