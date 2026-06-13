package com.ampairs.order

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.order.db.OrderRoomDatabase
import com.ampairs.order.db.migrations.ORDER_MIGRATION_1_2
import com.ampairs.order.db.migrations.ORDER_MIGRATION_2_3
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface OrderAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideOrderDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): OrderRoomDatabase = factory.createAndroidDatabase<OrderRoomDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "order",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(ORDER_MIGRATION_1_2, ORDER_MIGRATION_2_3),
        ).also { closableRegistry.register { it.close() } }
    }
}
