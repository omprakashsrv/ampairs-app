package com.ampairs.unit.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.db.entity.UnitConversionEntity
import com.ampairs.unit.data.db.entity.UnitEntity

/**
 * Unit Management Room Database
 *
 * Workspace-isolated database for unit and unit conversion management.
 * Uses WorkspaceAwareDatabaseFactory for proper workspace segregation.
 *
 * Version: 1 (Initial version)
 * Entities: UnitEntity, UnitConversionEntity
 */
@Database(
    entities = [
        UnitEntity::class,
        UnitConversionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(UnitDatabaseConstructor::class)
abstract class UnitDatabase : RoomDatabase() {
    /**
     * DAO for unit operations
     */
    abstract fun unitDao(): UnitDao

    /**
     * DAO for unit conversion operations
     */
    abstract fun unitConversionDao(): UnitConversionDao
}

/**
 * Room database constructor - platform-specific implementation
 *
 * Expect/actual pattern allows different database builders per platform:
 * - Android: Room with Android context
 * - iOS: Room with Core Data bridge
 * - Desktop: Room with JDBC drivers
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object UnitDatabaseConstructor : RoomDatabaseConstructor<UnitDatabase>
