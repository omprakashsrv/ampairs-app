package com.ampairs.product

import com.ampairs.common.di.AppScope
import com.ampairs.product.db.ProductRoomDatabase
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
    }
}
