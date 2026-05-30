package com.ampairs.product

import com.ampairs.common.di.AppScope
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductVariantDao
import com.ampairs.product.db.dao.VariantAttributeDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface ProductDaoModule {
    companion object {
        @Provides
        fun provideProductDao(db: ProductRoomDatabase): ProductDao = db.productDao()

        @Provides
        fun provideProductVariantDao(db: ProductRoomDatabase): ProductVariantDao = db.productVariantDao()

        @Provides
        fun provideVariantAttributeDao(db: ProductRoomDatabase): VariantAttributeDao = db.variantAttributeDao()

        @Provides
        fun provideGroupDao(db: ProductRoomDatabase): GroupDao = db.groupDao()

        @Provides
        fun provideCategoryDao(db: ProductRoomDatabase): CategoryDao = db.categoryDao()

        @Provides
        fun provideProductDataService(repo: ProductRepository): ProductDataService = repo
    }
}
