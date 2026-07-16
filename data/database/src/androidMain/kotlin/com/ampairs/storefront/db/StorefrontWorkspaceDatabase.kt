package com.ampairs.storefront.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.ampairs.ecom.data.db.dao.AddressDao
import com.ampairs.ecom.data.db.dao.CartDao
import com.ampairs.ecom.data.db.dao.EcomOrderDao
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.dao.StorefrontDao
import com.ampairs.ecom.data.db.dao.SyncCursorDao
import com.ampairs.ecom.data.db.dao.TaxonomyImageDao
import com.ampairs.ecom.data.db.entity.CartEntity
import com.ampairs.ecom.data.db.entity.CartItemEntity
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.db.entity.EcomOrderLineItemEntity
import com.ampairs.ecom.data.db.entity.ListedProductEntity
import com.ampairs.ecom.data.db.entity.StorefrontEntity
import com.ampairs.ecom.data.db.entity.SyncCursorEntity
import com.ampairs.ecom.data.db.entity.TaxonomyImageEntity
import com.ampairs.file.db.dao.FileDao
import com.ampairs.file.db.entity.FileEntity
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import com.ampairs.store.data.db.entity.StoreSettingDefinitionEntity
import com.ampairs.store.data.db.entity.StoreSettingEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity

/**
 * Consolidated per-workspace database for the storefront apps — one file
 * (`workspace_{slug}_main.db`) holding the ecom, store-settings, file and sync-state schemas that
 * previously lived in four per-module files
 * ([com.ampairs.storefront.di.StorefrontDatabaseModule] imports the legacy files once on upgrade).
 *
 * Android-only module, so no `@ConstructedBy` — Room resolves the generated impl reflectively.
 */
@Database(
    entities = [
        // ecom (was workspace_{slug}_ecom.db v2)
        StorefrontEntity::class,
        TaxonomyImageEntity::class,
        ListedProductEntity::class,
        SyncCursorEntity::class,
        CartEntity::class,
        CartItemEntity::class,
        CustomerAddressEntity::class,
        EcomOrderEntity::class,
        EcomOrderLineItemEntity::class,
        // store settings (was store.db v2)
        StoreSettingEntity::class,
        StoreSettingDefinitionEntity::class,
        // file (was file.db v1)
        FileEntity::class,
        // sync state (was sync.db v2)
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StorefrontWorkspaceDatabase : RoomDatabase() {
    abstract fun storefrontDao(): StorefrontDao
    abstract fun taxonomyImageDao(): TaxonomyImageDao
    abstract fun listedProductDao(): ListedProductDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun cartDao(): CartDao
    abstract fun addressDao(): AddressDao
    abstract fun ecomOrderDao(): EcomOrderDao
    abstract fun storeSettingDao(): StoreSettingDao
    abstract fun storeSettingDefinitionDao(): StoreSettingDefinitionDao
    abstract fun fileDao(): FileDao
    abstract fun syncStateDao(): SyncStateDao
}
