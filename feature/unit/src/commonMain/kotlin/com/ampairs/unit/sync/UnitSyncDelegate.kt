package com.ampairs.unit.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.unit.data.repository.UnitRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.UNIT)
class UnitSyncDelegate(
    private val unitRepository: UnitRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.UNIT

    override suspend fun pullFromServer(): SyncResult =
        unitRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        unitRepository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { unitRepository.handleExternalEvent(entityId, eventType) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )
}
