package com.ampairs.file.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.file.domain.FileRepositoryImpl
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.FILE)
class FileSyncDelegate(
    private val repository: FileRepositoryImpl,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.FILE

    override suspend fun pushPendingToServer(): SyncResult =
        repository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pullFromServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        SyncResult.Success(0)
}
