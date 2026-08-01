package com.ampairs.sync.di

import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

@ContributesTo(AppScope::class)
interface SyncModule {
    /** App-scope declaration — intentionally empty; actual delegates live in WorkspaceScope. */
    @Multibinds(allowEmpty = true)
    fun syncDelegates(): Map<SyncEntity, SyncDelegate>
}

@ContributesTo(WorkspaceScope::class)
interface WorkspaceSyncModule {
    /** Workspace-scope multibinding map — populated by @ContributesIntoMap(WorkspaceScope) delegates. */
    @Multibinds
    fun syncDelegates(): Map<SyncEntity, SyncDelegate>

    // SyncStateDao is provided by the composition root's consolidated workspace database module
    // (:data:database for the main app, shared-ecom's storefront database for the client apps).
}
