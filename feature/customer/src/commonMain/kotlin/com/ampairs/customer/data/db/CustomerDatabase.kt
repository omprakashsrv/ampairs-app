package com.ampairs.customer.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.customer.agent.CustomerAgentDao

@Database(
    entities = [CustomerEntity::class, StateEntity::class, CustomerTypeEntity::class, CustomerGroupEntity::class],
    version = 10,
    exportSchema = true
)
@ConstructedBy(CustomerDatabaseConstructor::class)
abstract class CustomerDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun stateDao(): StateDao
    abstract fun customerTypeDao(): CustomerTypeDao
    abstract fun customerGroupDao(): CustomerGroupDao

    /** DAO for assistant report queries. */
    abstract fun customerAgentDao(): CustomerAgentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CustomerDatabaseConstructor : RoomDatabaseConstructor<CustomerDatabase> {
    override fun initialize(): CustomerDatabase
}