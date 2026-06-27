package com.ampairs.supplier.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.supplier.data.db.SupplierDatabase
import com.ampairs.supplier.data.db.SupplierDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface SupplierDaoModule {
    companion object {
        @Provides
        fun provideSupplierDao(db: SupplierDatabase): SupplierDao = db.supplierDao()
    }
}
