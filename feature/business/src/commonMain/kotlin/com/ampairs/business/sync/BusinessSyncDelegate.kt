package com.ampairs.business.sync

import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Brings the workspace business profile onto the CentralSyncService pipeline so an edit on one
 * device propagates to the others — the same way customers, invoices, etc. do.
 *
 * The backend already emits a `business` change signal (`EntityChangePublisher.updated("business", …)`)
 * on every profile save and contributes a `business` checkpoint, but before this delegate the app had
 * no `SyncEntity.BUSINESS` handler, so both the live event and the connect/reconnect/hourly checkpoint
 * reconcile were dropped — the profile only refreshed when a business screen re-opened and called
 * `syncFromRemote()` itself.
 *
 * Like the workspace-module delegate, business writes are RPC-style: [BusinessRepository.upsertBusiness]
 * already pushes edits to the backend directly (offline-first, retried by that path), so there is no
 * local pending-push queue here and [pushPendingToServer] is a no-op. The value is the PULL side:
 * on a backend `business` signal, and on every checkpoint reconcile, re-pull the single business
 * profile via [BusinessRepository.syncFromRemote] (a full fetch-and-replace).
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.BUSINESS)
class BusinessSyncDelegate(
    private val repository: BusinessRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.BUSINESS

    override suspend fun pullFromServer(): SyncResult =
        repository.syncFromRemote().fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )

    // Business edits already reach the server through BusinessRepository.upsertBusiness();
    // there is no local pending-push queue for the profile, so nothing to push here.
    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()
}
