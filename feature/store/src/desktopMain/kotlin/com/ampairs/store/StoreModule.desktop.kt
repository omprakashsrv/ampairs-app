package com.ampairs.store

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.store.data.db.StoreDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface StoreDesktopModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideStoreDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): StoreDatabase = factory.createDatabase<StoreDatabase>(
            moduleName = "store",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
