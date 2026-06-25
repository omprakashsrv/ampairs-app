package com.ampairs.order

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.order.db.OrderRoomDatabase
import com.ampairs.order.db.migrations.ORDER_MIGRATION_1_2
import com.ampairs.order.db.migrations.ORDER_MIGRATION_2_3
import com.ampairs.order.db.migrations.ORDER_MIGRATION_3_4
import com.ampairs.order.db.migrations.ORDER_MIGRATION_4_5
import com.ampairs.order.db.migrations.ORDER_MIGRATION_5_6
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface OrderIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideOrderDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): OrderRoomDatabase = factory.createDatabase<OrderRoomDatabase>(
            moduleName = "order",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(ORDER_MIGRATION_1_2, ORDER_MIGRATION_2_3, ORDER_MIGRATION_3_4, ORDER_MIGRATION_4_5, ORDER_MIGRATION_5_6),
        ).also { closableRegistry.register { it.close() } }
    }
}
