package com.ampairs.customer.di

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_6_7
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_7_8
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_8_9
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface CustomerAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideCustomerDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): CustomerDatabase = factory.createAndroidDatabase<CustomerDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "customer",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(CUSTOMER_MIGRATION_6_7, CUSTOMER_MIGRATION_7_8, CUSTOMER_MIGRATION_8_9),
        ).also { closableRegistry.register { it.close() } }
    }
}
