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
import com.ampairs.common.database.legacy.LegacyDatabaseImporter
import com.ampairs.database.legacy.LegacySchemas
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

/**
 * Android provider for the consolidated [AmpairsAppDatabase]; absorbed the three legacy
 * app-scoped databases. First open runs [LegacyDatabaseImporter] to absorb
 * `auth.db` / `workspace.db` and drop `agent_catalog.db`.
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
                .addCallback(
                    LegacyDatabaseImporter.callback {
                        LegacySchemas.appSources { fileName -> context.getDatabasePath(fileName).absolutePath }
                    }
                )
                .build()
        }
    }
}

/**
 * Android provider for the consolidated per-workspace [AmpairsWorkspaceDatabase]
 * (`workspace_{slug}_main.db`); absorbed all 22 legacy per-module workspace databases.
 * First open per workspace runs [LegacyDatabaseImporter] to absorb the legacy module files.
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
                .addCallback(
                    LegacyDatabaseImporter.callback {
                        LegacySchemas.workspaceSources { module ->
                            context.getDatabasePath("workspace_${slug}_${module}.db").absolutePath
                        }
                    }
                )
                .build()
                .also { closableRegistry.register { it.close() } }
        }
    }
}
