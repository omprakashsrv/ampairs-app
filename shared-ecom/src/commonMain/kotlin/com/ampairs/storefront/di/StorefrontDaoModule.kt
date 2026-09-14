package com.ampairs.storefront.di

import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.data.db.dao.AddressDao
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.dao.EcomOrderDao
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.dao.StorefrontDao
import com.ampairs.ecom.data.db.dao.StorefrontDirectoryDao
import com.ampairs.ecom.data.db.dao.SyncCursorDao
import com.ampairs.ecom.data.db.dao.TaxonomyImageDao
import com.ampairs.file.db.dao.FileDao
import com.ampairs.storefront.db.StorefrontAppDatabase
import com.ampairs.storefront.db.StorefrontDirectoryDatabase
import com.ampairs.storefront.db.StorefrontWorkspaceDatabase
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Platform-agnostic DAO providers for the storefront databases. The `@Database` classes now live in
 * `:data:database/commonMain` (Room KSP runs per-target), so the DAO accessors are identical on every
 * platform and belong here. Only the DB *builders* differ per platform:
 * [com.ampairs.storefront.di.StorefrontAppDatabaseModule] / `StorefrontWorkspaceDatabaseModule`
 * (androidMain, `Context`-based) and their iOS counterparts in `StorefrontDatabaseModule.ios.kt`
 * (path-based). Mirrors the `AppDatabaseDaoModule` / `WorkspaceDatabaseDaoModule` split in
 * `:data:database` for the slim storefront feature set.
 */
@ContributesTo(AppScope::class)
interface StorefrontAppDaoModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideUserDao(db: StorefrontAppDatabase): UserDao = db.userDao()

        @Provides @SingleIn(AppScope::class)
        fun provideTokenDao(db: StorefrontAppDatabase): UserTokenDao = db.userTokenDao()

        @Provides @SingleIn(AppScope::class)
        fun provideSessionDao(db: StorefrontAppDatabase): UserSessionDao = db.userSessionDao()

        @Provides @SingleIn(AppScope::class)
        fun provideStorefrontDirectoryDao(db: StorefrontDirectoryDatabase): StorefrontDirectoryDao =
            db.storefrontDirectoryDao()
    }
}

@ContributesTo(WorkspaceScope::class)
interface StorefrontWorkspaceDaoModule {
    companion object {
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
