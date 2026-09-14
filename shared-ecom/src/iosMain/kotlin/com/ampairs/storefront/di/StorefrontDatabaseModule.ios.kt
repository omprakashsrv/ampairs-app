package com.ampairs.storefront.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.database.migrations.STOREFRONT_APP_DOWNGRADE_2_1
import com.ampairs.database.migrations.WORKSPACE_MIGRATION_1_2
import com.ampairs.storefront.db.StorefrontAppDatabase
import com.ampairs.storefront.db.StorefrontDirectoryDatabase
import com.ampairs.storefront.db.StorefrontWorkspaceDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * iOS *builders* for the storefront databases (`Documents/storefront_app.db`,
 * `Documents/storefront_directory.db`, `Documents/workspace_{slug}/main.db`). The DAO accessors are
 * platform-agnostic (see [StorefrontAppDaoModule] / [StorefrontWorkspaceDaoModule] in commonMain).
 * Mirrors `AppDatabaseIosModule` / `WorkspaceDatabaseIosModule` in `:data:database` for the slim
 * storefront feature set.
 */
@ContributesTo(AppScope::class)
interface StorefrontAppDatabaseIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideStorefrontAppDatabase(): StorefrontAppDatabase =
            Room.databaseBuilder<StorefrontAppDatabase>(name = getIosDatabasePath("storefront_app.db"))
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(DispatcherProvider.io)
                .addMigrations(STOREFRONT_APP_DOWNGRADE_2_1)
                .build()

        @Provides
        @SingleIn(AppScope::class)
        fun provideStorefrontDirectoryDatabase(): StorefrontDirectoryDatabase =
            Room.databaseBuilder<StorefrontDirectoryDatabase>(name = getIosDatabasePath("storefront_directory.db"))
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(DispatcherProvider.io)
                .build()
    }
}

@ContributesTo(WorkspaceScope::class)
interface StorefrontWorkspaceDatabaseIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideStorefrontWorkspaceDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): StorefrontWorkspaceDatabase {
            val slug = config.workspaceSlug
            val pathProvider = factory.databasePathProvider
            return Room.databaseBuilder<StorefrontWorkspaceDatabase>(
                name = pathProvider.getWorkspaceDatabasePath(slug, "main"),
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(factory.queryDispatcher)
                .addMigrations(WORKSPACE_MIGRATION_1_2)
                .build()
                .also { closableRegistry.register { it.close() } }
        }
    }
}
