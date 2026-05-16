package com.ampairs.inventory

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.inventory.db.InventoryRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val inventoryPlatformModule: Module = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<InventoryRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = InventoryRoomDatabase::class,
            moduleName = "inventory",
            migrations = emptyList()
        )
    }
}
