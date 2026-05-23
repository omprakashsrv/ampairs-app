package com.ampairs.di

import com.ampairs.common.cache.CacheCleanable
import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.product.data.repository.ProductRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface CacheModule {
    companion object {
        @Provides
        fun provideCacheCleanables(
            customerRepository: CustomerRepository,
            productRepository: ProductRepository
        ): Set<CacheCleanable> = setOf(customerRepository, productRepository)
    }
}
