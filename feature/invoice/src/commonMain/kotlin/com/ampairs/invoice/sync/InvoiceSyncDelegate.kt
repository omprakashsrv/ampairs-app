package com.ampairs.invoice.sync

import com.ampairs.common.di.AppScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.INVOICE)
class InvoiceSyncDelegate : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.INVOICE

    override suspend fun pullFromServer(): SyncResult = SyncResult.Success(0)

    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        SyncResult.Success(0)
}
