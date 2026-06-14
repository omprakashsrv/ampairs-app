package com.ampairs.di

import com.ampairs.business.domain.BusinessLocaleProvider
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.common.workspace.WorkspaceResources
import com.ampairs.event.EventSyncBridge
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@GraphExtension(WorkspaceScope::class)
interface WorkspaceGraph : ViewModelGraph {
    val workspaceResources: WorkspaceResources
    val syncDelegates: Map<SyncEntity, SyncDelegate>
    val syncStateDatabase: SyncStateDatabase
    val eventSyncBridge: EventSyncBridge
    val businessLocaleProvider: BusinessLocaleProvider

    @GraphExtension.Factory
    @ContributesTo(AppScope::class)
    fun interface Factory {
        fun create(@Provides config: WorkspaceConfig): WorkspaceGraph
    }
}
