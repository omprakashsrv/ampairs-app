package com.ampairs.storefront.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.ecom.data.db.dao.AddressDao
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.dao.EcomOrderDao
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.dao.StorefrontDao
import com.ampairs.ecom.data.db.dao.SyncCursorDao
import com.ampairs.ecom.data.db.dao.TaxonomyImageDao
import com.ampairs.file.db.dao.FileDao
import com.ampairs.storefront.db.StorefrontAppDatabase
import com.ampairs.storefront.db.StorefrontWorkspaceDatabase
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

/**
 * Providers for the two consolidated storefront databases plus every DAO they own. Mirrors the
 * main app's `:data:database` module for the slim storefront feature set (auth + ecom + store +
 * file + sync-state).
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
                .build()
        }

        @Provides @SingleIn(AppScope::class)
        fun provideUserDao(db: StorefrontAppDatabase): UserDao = db.userDao()

        @Provides @SingleIn(AppScope::class)
        fun provideTokenDao(db: StorefrontAppDatabase): UserTokenDao = db.userTokenDao()

        @Provides @SingleIn(AppScope::class)
        fun provideSessionDao(db: StorefrontAppDatabase): UserSessionDao = db.userSessionDao()
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
                .build()
                .also { closableRegistry.register { it.close() } }
        }

        // ecom
        @Provides
        fun provideStorefrontDao(db: StorefrontWorkspaceDatabase): StorefrontDao = db.storefrontDao()

        @Provides
        fun provideTaxonomyImageDao(db: StorefrontWorkspaceDatabase): TaxonomyImageDao = db.taxonomyImageDao()

        @Provides
        fun provideListedProductDao(db: StorefrontWorkspaceDatabase): ListedProductDao = db.listedProductDao()

        @Provides
        fun provideSyncCursorDao(db: StorefrontWorkspaceDatabase): SyncCursorDao = db.syncCursorDao()

        @Provides
        fun provideCartDao(db: StorefrontWorkspaceDatabase): CartDao = db.cartDao()

        @Provides
        fun provideAddressDao(db: StorefrontWorkspaceDatabase): AddressDao = db.addressDao()

        @Provides
        fun provideEcomOrderDao(db: StorefrontWorkspaceDatabase): EcomOrderDao = db.ecomOrderDao()

        // store settings
        @Provides
        fun provideStoreSettingDao(db: StorefrontWorkspaceDatabase): StoreSettingDao = db.storeSettingDao()

        @Provides
        fun provideStoreSettingDefinitionDao(db: StorefrontWorkspaceDatabase): StoreSettingDefinitionDao =
            db.storeSettingDefinitionDao()

        // file
        @Provides
        fun provideFileDao(db: StorefrontWorkspaceDatabase): FileDao = db.fileDao()

        // sync state
        @Provides
        fun provideSyncStateDao(db: StorefrontWorkspaceDatabase): SyncStateDao = db.syncStateDao()
    }
}
