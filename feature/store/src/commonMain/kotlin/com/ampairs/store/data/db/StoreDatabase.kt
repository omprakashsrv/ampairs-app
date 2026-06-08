package com.ampairs.store.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.entity.StoreSettingEntity

/**
 * Workspace-isolated Room database for store (module) settings.
 *
 * Version: 1
 * Entities: StoreSettingEntity
 */
@Database(
    entities = [StoreSettingEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(StoreDatabaseConstructor::class)
abstract class StoreDatabase : RoomDatabase() {
    abstract fun storeSettingDao(): StoreSettingDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object StoreDatabaseConstructor : RoomDatabaseConstructor<StoreDatabase> {
    override fun initialize(): StoreDatabase
}
