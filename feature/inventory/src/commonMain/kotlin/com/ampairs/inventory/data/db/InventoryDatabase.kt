package com.ampairs.inventory.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.inventory.agent.InventoryAgentDao

@Database(
    entities = [InventoryItemEntity::class, InventoryTransactionEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(InventoryDatabaseConstructor::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun inventoryTransactionDao(): InventoryTransactionDao

    /** DAO for assistant report queries. */
    abstract fun inventoryAgentDao(): InventoryAgentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object InventoryDatabaseConstructor : RoomDatabaseConstructor<InventoryDatabase> {
    override fun initialize(): InventoryDatabase
}
