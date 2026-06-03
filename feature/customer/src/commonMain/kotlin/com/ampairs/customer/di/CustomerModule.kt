package com.ampairs.customer.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.StateDao
import com.ampairs.customer.data.repository.CustomerRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface CustomerDaoModule {
    companion object {
        @Provides
        fun provideCustomerDao(db: CustomerDatabase): CustomerDao = db.customerDao()

        @Provides
        fun provideCustomerTypeDao(db: CustomerDatabase): CustomerTypeDao = db.customerTypeDao()

        @Provides
        fun provideCustomerGroupDao(db: CustomerDatabase): CustomerGroupDao = db.customerGroupDao()

        @Provides
        fun provideStateDao(db: CustomerDatabase): StateDao = db.stateDao()

        @Provides
        fun provideCustomerDataService(repo: CustomerRepository): CustomerDataService = repo
    }
}
