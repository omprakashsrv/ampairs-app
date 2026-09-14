package com.ampairs.storefront.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
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
import kotlinx.coroutines.Dispatchers

/**
 * Android *builders* for the two consolidated storefront databases plus the standalone directory
 * cache. The DAO accessors are platform-agnostic and live in
 * [com.ampairs.storefront.di.StorefrontAppDaoModule] / `StorefrontWorkspaceDaoModule` (commonMain);
 * the iOS builders live in `StorefrontDatabaseModule.ios.kt`. Mirrors the main app's
 * `:data:database` DB-module split for the slim storefront feature set (auth + ecom + store + file +
 * sync-state).
 */
@ContributesTo(AppScope::class)
interface StorefrontAppDatabaseModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideStorefrontAppDatabase(context: Context): StorefrontAppDatabase {
            val dbFile = context.getDatabasePath("storefront_app.db")
            return Room.databaseBuilder<StorefrontAppDatabase>(
                context = context,
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .enableMultiInstanceInvalidation()
                // Recovers devices whose storefront_app.db was briefly bumped to v2 (the directory
                // cache used to live here); the migration drops that stray table, keeping auth.
                .addMigrations(STOREFRONT_APP_DOWNGRADE_2_1)
                .build()
        }

        // Storefront-directory offline cache — its OWN disposable database, decoupled from the
        // durable auth DB above so a cache schema change never risks the auth store's version.
        @Provides
        @SingleIn(AppScope::class)
        fun provideStorefrontDirectoryDatabase(context: Context): StorefrontDirectoryDatabase {
            val dbFile = context.getDatabasePath("storefront_directory.db")
            return Room.databaseBuilder<StorefrontDirectoryDatabase>(
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

@ContributesTo(WorkspaceScope::class)
interface StorefrontWorkspaceDatabaseModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideStorefrontWorkspaceDatabase(
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): StorefrontWorkspaceDatabase {
            val slug = config.workspaceSlug
            val dbFile = context.getDatabasePath("workspace_${slug}_main.db")
            return Room.databaseBuilder<StorefrontWorkspaceDatabase>(
                context = context,
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .enableMultiInstanceInvalidation()
                .addMigrations(WORKSPACE_MIGRATION_1_2)
                .build()
                .also { closableRegistry.register { it.close() } }
        }
    }
}
