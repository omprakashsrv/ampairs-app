package com.ampairs.sync.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SyncStateEntity::class], version = 1)
abstract class SyncStateDatabase : RoomDatabase() {
    abstract fun syncStateDao(): SyncStateDao
}
