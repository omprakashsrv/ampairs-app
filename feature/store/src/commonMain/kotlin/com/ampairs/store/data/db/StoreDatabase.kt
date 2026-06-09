package com.ampairs.store.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import com.ampairs.store.data.db.entity.StoreSettingDefinitionEntity
import com.ampairs.store.data.db.entity.StoreSettingEntity

/**
 * Workspace-isolated Room database for store (module) settings.
 *
 * Version: 2 — adds the pull-only setting-definition cache.
 * Entities: StoreSettingEntity (overrides), StoreSettingDefinitionEntity (server catalog)
 */
@Database(
    entities = [StoreSettingEntity::class, StoreSettingDefinitionEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(StoreDatabaseConstructor::class)
abstract class StoreDatabase : RoomDatabase() {
    abstract fun storeSettingDao(): StoreSettingDao
    abstract fun storeSettingDefinitionDao(): StoreSettingDefinitionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object StoreDatabaseConstructor : RoomDatabaseConstructor<StoreDatabase> {
    override fun initialize(): StoreDatabase
}
