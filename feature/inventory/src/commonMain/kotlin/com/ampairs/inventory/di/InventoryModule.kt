package com.ampairs.inventory.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.db.InventoryDatabase
import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.InventoryTransactionDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface InventoryDaoModule {
    companion object {
        @Provides
        fun provideInventoryItemDao(db: InventoryDatabase): InventoryItemDao = db.inventoryItemDao()

        @Provides
        fun provideInventoryTransactionDao(db: InventoryDatabase): InventoryTransactionDao =
            db.inventoryTransactionDao()
    }
}
