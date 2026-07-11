package com.ampairs.database.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.database.AmpairsAppDatabase
import com.ampairs.database.AmpairsWorkspaceDatabase
import com.ampairs.common.database.legacy.LegacyDatabaseImporter
import com.ampairs.database.legacy.LegacySchemas
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * iOS provider for the consolidated [AmpairsAppDatabase] (`Documents/ampairs_app.db`); absorbed the three legacy app-scoped databases. See the Android counterpart for details.
 */
@ContributesTo(AppScope::class)
interface AppDatabaseIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideAmpairsAppDatabase(): AmpairsAppDatabase =
            Room.databaseBuilder<AmpairsAppDatabase>(name = getIosDatabasePath("ampairs_app.db"))
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(DispatcherProvider.io)
                .addCallback(
                    LegacyDatabaseImporter.callback {
                        LegacySchemas.appSources { fileName -> getIosDatabasePath(fileName) }
                    }
                )
                .build()
    }
}

/**
 * iOS provider for the consolidated per-workspace [AmpairsWorkspaceDatabase]
 * (`Documents/workspace_{slug}/main.db`); absorbed all 22 legacy per-module workspace
 * databases. See the Android counterpart for details.
 */
@ContributesTo(WorkspaceScope::class)
interface WorkspaceDatabaseIosModule {
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
                .addCallback(
                    LegacyDatabaseImporter.callback {
                        LegacySchemas.workspaceSources { module ->
                            pathProvider.getWorkspaceDatabasePath(slug, module)
                        }
                    }
                )
                .build()
                .also { closableRegistry.register { it.close() } }
        }
    }
}
