package com.ampairs.inventory

import com.ampairs.common.di.AppScope
import com.ampairs.inventory.db.InventoryRoomDatabase
import com.ampairs.inventory.db.dao.InventoryDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface InventoryDaoModule {
    companion object {
        @Provides
        fun provideInventoryDao(db: InventoryRoomDatabase): InventoryDao = db.inventoryDao()
    }
}
