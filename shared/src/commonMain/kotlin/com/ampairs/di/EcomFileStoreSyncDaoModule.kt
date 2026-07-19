package com.ampairs.di

import com.ampairs.common.agent.WorkspaceDatabaseProvider
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.database.AmpairsWorkspaceDatabase
import com.ampairs.ecom.data.db.dao.AddressDao
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.dao.EcomOrderDao
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.dao.StorefrontDao
import com.ampairs.ecom.data.db.dao.SyncCursorDao
import com.ampairs.ecom.data.db.dao.TaxonomyImageDao
import com.ampairs.file.db.dao.FileDao
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Provides the ecom/store/file/sync-state DAOs (and the [WorkspaceDatabaseProvider] binding used
 * by the agent SAFE_QUERY executors) from the consolidated [AmpairsWorkspaceDatabase], for the
 * main app only. Kept out of `:data:database`'s `WorkspaceDatabaseDaoModule` deliberately: the
 * storefront apps (`:shared-ecom`) also depend on `:data:database` (for the storefront `@Database`
 * class definitions) but not on `:shared`, and provide this same DAO subset from their own
 * `StorefrontWorkspaceDatabase`. Since Metro merges every `@ContributesTo(WorkspaceScope::class)`
 * visible on the classpath, these bindings must live only where `:shared-ecom` can't see them.
 */
@ContributesTo(WorkspaceScope::class)
interface EcomFileStoreSyncDaoModule {
    companion object {
        // ecom
        @Provides
        fun provideStorefrontDao(db: AmpairsWorkspaceDatabase): StorefrontDao = db.storefrontDao()

        @Provides
        fun provideTaxonomyImageDao(db: AmpairsWorkspaceDatabase): TaxonomyImageDao = db.taxonomyImageDao()

        @Provides
        fun provideListedProductDao(db: AmpairsWorkspaceDatabase): ListedProductDao = db.listedProductDao()

        @Provides
        fun provideSyncCursorDao(db: AmpairsWorkspaceDatabase): SyncCursorDao = db.syncCursorDao()

        @Provides
        fun provideCartDao(db: AmpairsWorkspaceDatabase): CartDao = db.cartDao()

        @Provides
        fun provideAddressDao(db: AmpairsWorkspaceDatabase): AddressDao = db.addressDao()

        @Provides
        fun provideEcomOrderDao(db: AmpairsWorkspaceDatabase): EcomOrderDao = db.ecomOrderDao()

        // store
        @Provides
        fun provideStoreSettingDao(db: AmpairsWorkspaceDatabase): StoreSettingDao = db.storeSettingDao()

        @Provides
        fun provideStoreSettingDefinitionDao(db: AmpairsWorkspaceDatabase): StoreSettingDefinitionDao =
            db.storeSettingDefinitionDao()

        // file
        @Provides
        fun provideFileDao(db: AmpairsWorkspaceDatabase): FileDao = db.fileDao()

        // sync state
        @Provides
        fun provideSyncStateDao(db: AmpairsWorkspaceDatabase): SyncStateDao = db.syncStateDao()

        // reader-connection handle for the agent SAFE_QUERY executors
        @Provides
        fun provideWorkspaceDatabaseProvider(db: AmpairsWorkspaceDatabase): WorkspaceDatabaseProvider =
            WorkspaceDatabaseProvider { db }
    }
}
