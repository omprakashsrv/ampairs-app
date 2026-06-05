package com.ampairs.invoice.sync

import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.Inject

// Not registered in the sync map — invoice sync is not yet implemented.
@Inject
class InvoiceSyncDelegate : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.INVOICE

    // Invoices reference orders, customers, products and tax — pull those first.
    override val dependsOn: List<SyncEntity> =
        listOf(SyncEntity.ORDER, SyncEntity.CUSTOMER, SyncEntity.PRODUCT, SyncEntity.TAX)

    override suspend fun pullFromServer(): SyncResult = SyncResult.Success(0)

    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        SyncResult.Success(0)
}
