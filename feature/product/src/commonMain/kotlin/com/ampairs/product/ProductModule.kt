package com.ampairs.product

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.data.repository.ProductRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Product DAOs are provided by the consolidated workspace database module (:data:database for the
// main app). This module keeps only the feature's non-DAO service bindings.
@ContributesTo(WorkspaceScope::class)
interface ProductServiceModule {
    companion object {
        @Provides
        fun provideProductDataService(repo: ProductRepository): ProductDataService = repo
    }
}
