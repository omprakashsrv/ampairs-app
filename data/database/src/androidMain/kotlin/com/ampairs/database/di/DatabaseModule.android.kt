package com.ampairs.database.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.database.AmpairsAppDatabase
import com.ampairs.database.AmpairsWorkspaceDatabase
import com.ampairs.database.migrations.WORKSPACE_MIGRATION_1_2
import com.ampairs.database.migrations.WORKSPACE_MIGRATION_2_3
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

/**
 * Android provider for the consolidated [AmpairsAppDatabase]; absorbed the three legacy
 * app-scoped databases (`auth.db` / `workspace.db` / `agent_catalog.db`).
 */
@ContributesTo(AppScope::class)
interface AppDatabaseAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideAmpairsAppDatabase(context: Context): AmpairsAppDatabase {
            val dbFile = context.getDatabasePath("ampairs_app.db")
            return Room.databaseBuilder<AmpairsAppDatabase>(
                context = context,
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .enableMultiInstanceInvalidation()
                .build()
        }
    }
}

/**
 * Android provider for the consolidated per-workspace [AmpairsWorkspaceDatabase]
 * (`workspace_{slug}_main.db`); absorbed all 22 legacy per-module workspace databases.
 */
@ContributesTo(WorkspaceScope::class)
interface WorkspaceDatabaseAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideAmpairsWorkspaceDatabase(
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): AmpairsWorkspaceDatabase {
            val slug = config.workspaceSlug
            val dbFile = context.getDatabasePath("workspace_${slug}_main.db")
            return Room.databaseBuilder<AmpairsWorkspaceDatabase>(
                context = context,
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .enableMultiInstanceInvalidation()
                .addMigrations(WORKSPACE_MIGRATION_1_2, WORKSPACE_MIGRATION_2_3)
                .build()
                .also { closableRegistry.register { it.close() } }
        }
    }
}
