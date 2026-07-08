package com.ampairs.sync.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [SyncStateEntity::class], version = 2)
@ConstructedBy(SyncStateDatabaseConstructor::class)
abstract class SyncStateDatabase : RoomDatabase() {
    abstract fun syncStateDao(): SyncStateDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SyncStateDatabaseConstructor : RoomDatabaseConstructor<SyncStateDatabase> {
    override fun initialize(): SyncStateDatabase
}
