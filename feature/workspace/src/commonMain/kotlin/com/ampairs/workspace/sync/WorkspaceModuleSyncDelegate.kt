package com.ampairs.workspace.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.workspace.db.WorkspaceModuleRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Brings the workspace module-enablement list onto the CentralSyncService pipeline so a module
 * installed / uninstalled / reordered on one device propagates to the others — the same way
 * customers, invoices, etc. do.
 *
 * Unlike the standard entities, module install/uninstall is RPC-style: the write path already hits
 * the backend directly through [WorkspaceModuleRepository] (`installModuleOfflineFirst` /
 * `uninstallModuleOfflineFirst`), and there is no local `synced = false` queue for it. So there is
 * nothing to bulk-push here — [pushPendingToServer] is a no-op. The value this delegate adds is the
 * PULL side: on a backend "module" change signal, and on every connect/reconnect/hourly checkpoint
 * reconcile, it re-pulls the installed-module list into Room via
 * [WorkspaceModuleRepository.syncInstalledModules], which fully replaces the local set (so
 * uninstalls on other devices are reflected too).
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.MODULE)
class WorkspaceModuleSyncDelegate(
    private val moduleRepository: WorkspaceModuleRepository,
    private val config: WorkspaceConfig,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.MODULE

    override suspend fun pullFromServer(): SyncResult =
        moduleRepository.syncInstalledModules(config.workspaceId).fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )

    // Module install/uninstall already reaches the server through the repository's RPC calls;
    // there is no local pending-push queue for modules, so nothing to push here.
    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()
}
