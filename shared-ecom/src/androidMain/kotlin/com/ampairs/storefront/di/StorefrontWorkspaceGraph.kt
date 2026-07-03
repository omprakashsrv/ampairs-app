package com.ampairs.storefront.di

import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.common.workspace.WorkspaceResources
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * Slim workspace child graph for the Storefront ecom app. Mirrors [com.ampairs.di.WorkspaceGraph] from
 * :shared but exposes only what the ecom + store sync path needs — no business-locale provider and
 * no push-token registrar (those live in the full app). Every workspace-aware DB / sync delegate
 * (ecom catalog, order, address, store settings) is contributed to [WorkspaceScope] and materialises
 * inside this graph, so a workspace switch tears the whole thing down cleanly.
 */
@GraphExtension(WorkspaceScope::class)
interface StorefrontWorkspaceGraph : ViewModelGraph {
    val workspaceResources: WorkspaceResources
    val syncDelegates: Map<SyncEntity, SyncDelegate>
    val syncStateDatabase: SyncStateDatabase

    @GraphExtension.Factory
    @ContributesTo(AppScope::class)
    fun interface Factory {
        fun create(@Provides config: WorkspaceConfig): StorefrontWorkspaceGraph
    }
}
