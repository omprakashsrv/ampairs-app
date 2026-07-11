package com.ampairs.supplier.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.supplier.data.SupplierDataService
import com.ampairs.supplier.data.repository.SupplierRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// The supplier DAO is provided by the consolidated workspace database module (:data:database for
// the main app). This module keeps only the feature's non-DAO service bindings.
@ContributesTo(WorkspaceScope::class)
interface SupplierServiceModule {
    companion object {
        @Provides
        fun provideSupplierDataService(repo: SupplierRepository): SupplierDataService = repo
    }
}
