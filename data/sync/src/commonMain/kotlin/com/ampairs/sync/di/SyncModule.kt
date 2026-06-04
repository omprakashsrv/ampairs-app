package com.ampairs.sync.di

import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides

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

    companion object {
        @Provides
        fun provideSyncStateDao(db: SyncStateDatabase): SyncStateDao = db.syncStateDao()
    }
}
