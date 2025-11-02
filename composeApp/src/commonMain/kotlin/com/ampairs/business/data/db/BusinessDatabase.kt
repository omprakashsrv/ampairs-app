package com.ampairs.business.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [BusinessEntity::class],
    version = 2,
    exportSchema = true
)
@ConstructedBy(BusinessDatabaseConstructor::class)
abstract class BusinessDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BusinessDatabaseConstructor : RoomDatabaseConstructor<BusinessDatabase>
