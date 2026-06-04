package com.ampairs.sync.di

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sync.db.SyncStateDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.sync.db.migrations.SYNC_MIGRATION_1_2
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface SyncAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideSyncStateDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): SyncStateDatabase = factory.createAndroidDatabase<SyncStateDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "sync",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(SYNC_MIGRATION_1_2),
        ).also { closableRegistry.register { it.close() } }
    }
}
