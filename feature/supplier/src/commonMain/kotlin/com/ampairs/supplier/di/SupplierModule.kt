package com.ampairs.supplier.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.supplier.data.SupplierDataService
import com.ampairs.supplier.data.db.SupplierDatabase
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.supplier.data.repository.SupplierRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface SupplierDaoModule {
    companion object {
        @Provides
        fun provideSupplierDao(db: SupplierDatabase): SupplierDao = db.supplierDao()

        @Provides
        fun provideSupplierDataService(repo: SupplierRepository): SupplierDataService = repo
    }
}
