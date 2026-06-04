package com.ampairs.customer.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER)
class CustomerSyncDelegate(
    private val customerRepository: CustomerRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER

    // Customer rows reference a group and a type — pull those first so names resolve locally.
    override val dependsOn: List<SyncEntity> =
        listOf(SyncEntity.CUSTOMER_GROUP, SyncEntity.CUSTOMER_TYPE)

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
