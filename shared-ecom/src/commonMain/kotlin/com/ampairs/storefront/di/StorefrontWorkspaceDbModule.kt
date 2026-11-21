package com.ampairs.storefront.di

import com.ampairs.common.agent.WorkspaceDatabaseProvider
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.storefront.db.StorefrontWorkspaceDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Binds the [WorkspaceDatabaseProvider] handle — injected by the agent SAFE_QUERY executors such
 * as `EcomQueryExecutor` — to the storefront's consolidated workspace database.
 */
@ContributesTo(WorkspaceScope::class)
interface StorefrontWorkspaceDbModule {
    companion object {
        @Provides
        fun provideWorkspaceDatabaseProvider(db: StorefrontWorkspaceDatabase): WorkspaceDatabaseProvider =
            WorkspaceDatabaseProvider { db }
    }
}
