package com.ampairs.customer.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_6_7
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_7_8
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface CustomerDesktopModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideCustomerDatabase(factory: WorkspaceAwareDatabaseFactory): CustomerDatabase =
            factory.createDatabase(klass = CustomerDatabase::class, moduleName = "customer", migrations = listOf(CUSTOMER_MIGRATION_6_7, CUSTOMER_MIGRATION_7_8))
    }
}
