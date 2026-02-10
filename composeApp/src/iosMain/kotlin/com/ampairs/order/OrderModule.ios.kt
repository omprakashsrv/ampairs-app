package com.ampairs.order

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.order.db.OrderRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val orderPlatformModule: Module = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<OrderRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = OrderRoomDatabase::class,
            moduleName = "order",
            migrations = emptyList()
        )
    }
}
