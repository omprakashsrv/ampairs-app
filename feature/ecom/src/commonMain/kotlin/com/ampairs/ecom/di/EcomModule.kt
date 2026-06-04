package com.ampairs.ecom.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.data.db.EcomRoomDatabase
import com.ampairs.ecom.data.db.dao.AddressDao
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.dao.EcomOrderDao
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.dao.StorefrontDao
import com.ampairs.ecom.data.db.dao.SyncCursorDao
import com.ampairs.ecom.data.db.dao.TaxonomyImageDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Common DI: DAOs derived from the EcomRoomDatabase. The database itself is provided per
 * platform (EcomModule.android/ios/desktop). Repositories, sync delegates, ViewModels are
 * auto-wired via their @Inject / @ContributesIntoMap annotations.
 */
@ContributesTo(WorkspaceScope::class)
interface EcomDaoModule {
    companion object {
        @Provides fun provideStorefrontDao(db: EcomRoomDatabase): StorefrontDao = db.storefrontDao()
        @Provides fun provideTaxonomyImageDao(db: EcomRoomDatabase): TaxonomyImageDao = db.taxonomyImageDao()
        @Provides fun provideListedProductDao(db: EcomRoomDatabase): ListedProductDao = db.listedProductDao()
        @Provides fun provideSyncCursorDao(db: EcomRoomDatabase): SyncCursorDao = db.syncCursorDao()
        @Provides fun provideCartDao(db: EcomRoomDatabase): CartDao = db.cartDao()
        @Provides fun provideAddressDao(db: EcomRoomDatabase): AddressDao = db.addressDao()
        @Provides fun provideEcomOrderDao(db: EcomRoomDatabase): EcomOrderDao = db.ecomOrderDao()
    }
}
