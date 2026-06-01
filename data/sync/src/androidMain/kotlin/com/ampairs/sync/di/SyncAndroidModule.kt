package com.ampairs.sync.di

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.sync.SyncDatabaseFactory
import com.ampairs.sync.db.SyncStateDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface SyncAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideSyncDatabaseFactory(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
        ): SyncDatabaseFactory = SyncDatabaseFactory { workspaceSlug ->
            factory.createAndroidDatabase<SyncStateDatabase>(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "sync_state",
                workspaceSlug = workspaceSlug,
            )
        }
    }
}
