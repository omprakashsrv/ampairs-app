package com.ampairs.customer.di

import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.CustomerImageDao
import com.ampairs.customer.data.db.StateDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface CustomerDaoModule {
    companion object {
        @Provides
        fun provideCustomerDao(db: CustomerDatabase): CustomerDao = db.customerDao()

        @Provides
        fun provideCustomerTypeDao(db: CustomerDatabase): CustomerTypeDao = db.customerTypeDao()

        @Provides
        fun provideCustomerGroupDao(db: CustomerDatabase): CustomerGroupDao = db.customerGroupDao()

        @Provides
        fun provideCustomerImageDao(db: CustomerDatabase): CustomerImageDao = db.customerImageDao()

        @Provides
        fun provideStateDao(db: CustomerDatabase): StateDao = db.stateDao()
    }
}
