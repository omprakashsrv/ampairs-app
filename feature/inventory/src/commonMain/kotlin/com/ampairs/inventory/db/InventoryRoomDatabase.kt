package com.ampairs.inventory.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.inventory.db.dao.InventoryDao
import com.ampairs.inventory.db.entity.InventoryEntity

@Database(
    entities = [InventoryEntity::class],
    version = 1,
    exportSchema = true
)
@ConstructedBy(InventoryRoomDatabaseConstructor::class)
abstract class InventoryRoomDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object InventoryRoomDatabaseConstructor : RoomDatabaseConstructor<InventoryRoomDatabase>