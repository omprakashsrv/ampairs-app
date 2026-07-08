package com.ampairs.ecom.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
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

@Database(
    entities = [
        StorefrontEntity::class,
        TaxonomyImageEntity::class,
        ListedProductEntity::class,
        SyncCursorEntity::class,
        CartEntity::class,
        CartItemEntity::class,
        CustomerAddressEntity::class,
        EcomOrderEntity::class,
        EcomOrderLineItemEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@ConstructedBy(EcomRoomDatabaseConstructor::class)
abstract class EcomRoomDatabase : RoomDatabase() {
    abstract fun storefrontDao(): StorefrontDao
    abstract fun taxonomyImageDao(): TaxonomyImageDao
    abstract fun listedProductDao(): ListedProductDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun cartDao(): CartDao
    abstract fun addressDao(): AddressDao
    abstract fun ecomOrderDao(): EcomOrderDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object EcomRoomDatabaseConstructor : RoomDatabaseConstructor<EcomRoomDatabase>
