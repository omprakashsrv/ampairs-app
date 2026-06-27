package com.ampairs.business.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.business.agent.BusinessAgentDao

@Database(
    entities = [BusinessEntity::class],
    version = 4,
    exportSchema = true
)
@ConstructedBy(BusinessDatabaseConstructor::class)
abstract class BusinessDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao

    /** DAO for assistant report queries. */
    abstract fun businessAgentDao(): BusinessAgentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BusinessDatabaseConstructor : RoomDatabaseConstructor<BusinessDatabase> {
    override fun initialize(): BusinessDatabase
}
