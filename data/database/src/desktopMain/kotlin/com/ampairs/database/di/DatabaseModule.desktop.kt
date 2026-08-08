package com.ampairs.database.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.desktop.DataDirectoryManager
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.database.AmpairsAppDatabase
import com.ampairs.database.AmpairsWorkspaceDatabase
import com.ampairs.database.migrations.WORKSPACE_MIGRATION_1_2
import com.ampairs.database.migrations.WORKSPACE_MIGRATION_2_3
import com.ampairs.database.migrations.WORKSPACE_MIGRATION_3_4
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlinx.coroutines.Dispatchers

/**
 * Desktop provider for the consolidated [AmpairsAppDatabase] (`{dataDir}/ampairs_app.db`),
 * replacing the three legacy app-scoped database providers. See the Android counterpart for details.
 */
@ContributesTo(AppScope::class)
interface AppDatabaseDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideAmpairsAppDatabase(): AmpairsAppDatabase {
            val dbFile = File(DataDirectoryManager.getDatabaseDir(), "ampairs_app.db")
            return Room.databaseBuilder<AmpairsAppDatabase>(name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}

/**
 * Desktop provider for the consolidated per-workspace [AmpairsWorkspaceDatabase]
 * (`{dataDir}/workspace_{slug}/main.db`); absorbed all 22 legacy per-module workspace
 * databases. See the Android counterpart for details.
 */
@ContributesTo(WorkspaceScope::class)
interface WorkspaceDatabaseDesktopModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideAmpairsWorkspaceDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): AmpairsWorkspaceDatabase {
            val slug = config.workspaceSlug
            val pathProvider = factory.databasePathProvider
            return Room.databaseBuilder<AmpairsWorkspaceDatabase>(
                name = pathProvider.getWorkspaceDatabasePath(slug, "main"),
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(factory.queryDispatcher)
                .addMigrations(WORKSPACE_MIGRATION_1_2, WORKSPACE_MIGRATION_2_3, WORKSPACE_MIGRATION_3_4)
                .build()
                .also { closableRegistry.register { it.close() } }
        }
    }
}
