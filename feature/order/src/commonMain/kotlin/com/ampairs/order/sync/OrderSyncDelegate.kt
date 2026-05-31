package com.ampairs.order.sync

import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.Inject

// Not registered in the sync map — order sync is not yet implemented.
@Inject
class OrderSyncDelegate : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.ORDER

    override suspend fun pullFromServer(): SyncResult = SyncResult.Success(0)

    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        SyncResult.Success(0)
}
