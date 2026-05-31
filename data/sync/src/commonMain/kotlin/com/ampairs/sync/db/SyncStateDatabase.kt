package com.ampairs.sync.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters

@Database(entities = [SyncStateEntity::class], version = 1)
@ConstructedBy(SyncStateDatabaseConstructor::class)
@TypeConverters(SyncStateConverters::class)
abstract class SyncStateDatabase : RoomDatabase() {
    abstract fun syncStateDao(): SyncStateDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SyncStateDatabaseConstructor : RoomDatabaseConstructor<SyncStateDatabase>