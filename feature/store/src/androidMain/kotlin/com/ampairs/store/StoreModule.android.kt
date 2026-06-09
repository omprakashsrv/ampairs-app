package com.ampairs.store

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.store.data.db.StoreDatabase
import com.ampairs.store.data.db.migrations.STORE_MIGRATION_1_2
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(WorkspaceScope::class)
interface StoreAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideStoreDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): StoreDatabase = factory.createAndroidDatabase<StoreDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "store",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(STORE_MIGRATION_1_2),
        ).also { closableRegistry.register { it.close() } }
    }
}
