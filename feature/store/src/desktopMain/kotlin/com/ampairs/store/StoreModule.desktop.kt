package com.ampairs.store

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.store.data.db.StoreDatabase
import com.ampairs.store.data.db.migrations.STORE_MIGRATION_1_2
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
            migrations = listOf(STORE_MIGRATION_1_2),
        ).also { closableRegistry.register { it.close() } }
    }
}
