package com.ampairs.customer.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.DesktopFileManager
import com.ampairs.customer.data.repository.PlatformFileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface CustomerDesktopModule {
    companion object {
        @Provides
        fun provideCustomerDatabase(factory: WorkspaceAwareDatabaseFactory): CustomerDatabase =
            factory.createDatabase(klass = CustomerDatabase::class, moduleName = "customer")

        @Provides
        fun providePlatformFileManager(): PlatformFileManager = DesktopFileManager()
    }
}
