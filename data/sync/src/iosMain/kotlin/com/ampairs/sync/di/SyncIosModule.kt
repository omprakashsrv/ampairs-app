package com.ampairs.sync.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.sync.SyncDatabaseFactory
import com.ampairs.sync.db.SyncStateDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface SyncIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideSyncDatabaseFactory(
            factory: WorkspaceAwareDatabaseFactory,
        ): SyncDatabaseFactory = SyncDatabaseFactory { workspaceSlug ->
            factory.createDatabase(
                klass = SyncStateDatabase::class,
                moduleName = "sync_state",
                workspaceSlug = workspaceSlug,
            )
        }
    }
}
