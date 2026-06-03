package com.ampairs.customer.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_6_7
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_7_8
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface CustomerIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideCustomerDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): CustomerDatabase = factory.createDatabase<CustomerDatabase>(
            moduleName = "customer",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(CUSTOMER_MIGRATION_6_7, CUSTOMER_MIGRATION_7_8),
        ).also { closableRegistry.register { it.close() } }
    }
}
