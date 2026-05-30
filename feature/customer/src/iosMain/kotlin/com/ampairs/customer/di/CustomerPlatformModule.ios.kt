package com.ampairs.customer.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.IosFileManager
import com.ampairs.customer.data.repository.PlatformFileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface CustomerIosModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideCustomerDatabase(factory: WorkspaceAwareDatabaseFactory): CustomerDatabase =
            factory.createDatabase(klass = CustomerDatabase::class, moduleName = "customer")

        @Provides
        fun providePlatformFileManager(): PlatformFileManager = IosFileManager()
    }
}
