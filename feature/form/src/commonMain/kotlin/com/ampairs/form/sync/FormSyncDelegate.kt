package com.ampairs.form.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.FORM)
class FormSyncDelegate(
    private val configRepository: ConfigRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.FORM

    override suspend fun pullFromServer(): SyncResult =
        configRepository.syncFormConfigs().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    // Form configs are server-authoritative — no local writes to push
    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        configRepository.syncFormConfigs().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )
}
