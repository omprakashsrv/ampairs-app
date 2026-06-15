package com.ampairs.product.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductVariantDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.dao.TaxCodeDao
import com.ampairs.product.db.dao.TaxInfoDao
import com.ampairs.product.db.dao.VariantAttributeDao
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.ProductVariantEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import com.ampairs.product.db.entity.TaxCodeEntity
import com.ampairs.product.db.entity.TaxInfoEntity
import com.ampairs.product.db.entity.VariantAttributeEntity

@Database(
    entities = [
        ProductEntity::class,
        TaxCodeEntity::class,
        TaxInfoEntity::class,
        GroupEntity::class,
        CategoryEntity::class,
        SubCategoryEntity::class,
        BrandEntity::class,
        ProductVariantEntity::class,
        VariantAttributeEntity::class
    ],
    version = 10,
    exportSchema = true
)
@ConstructedBy(ProductRoomDatabaseConstructor::class)
abstract class ProductRoomDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun taxCodeDao(): TaxCodeDao
    abstract fun taxInfoDao(): TaxInfoDao
    abstract fun groupDao(): GroupDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun brandDao(): BrandDao
    abstract fun productVariantDao(): ProductVariantDao
    abstract fun variantAttributeDao(): VariantAttributeDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ProductRoomDatabaseConstructor : RoomDatabaseConstructor<ProductRoomDatabase> {
    override fun initialize(): ProductRoomDatabase
}
