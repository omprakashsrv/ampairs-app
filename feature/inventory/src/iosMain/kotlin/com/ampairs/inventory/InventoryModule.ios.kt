package com.ampairs.inventory

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.inventory.db.InventoryRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface InventoryIosModule {
    companion object {
        @Provides
        fun provideInventoryDatabase(factory: WorkspaceAwareDatabaseFactory): InventoryRoomDatabase =
            factory.createDatabase(klass = InventoryRoomDatabase::class, moduleName = "inventory", migrations = emptyList())
    }
}
