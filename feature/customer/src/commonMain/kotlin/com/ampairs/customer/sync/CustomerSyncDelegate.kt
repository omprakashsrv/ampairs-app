package com.ampairs.customer.sync

import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER)
class CustomerSyncDelegate(
    private val customerRepository: CustomerRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER

    override suspend fun pullFromServer(): SyncResult =
        customerRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        customerRepository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { customerRepository.handleExternalEvent(entityId, eventType) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )
}
