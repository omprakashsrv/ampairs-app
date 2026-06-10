package com.ampairs.customer.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [CustomerEntity::class, StateEntity::class, CustomerTypeEntity::class, CustomerGroupEntity::class],
    version = 9,
    exportSchema = true
)
@ConstructedBy(CustomerDatabaseConstructor::class)
abstract class CustomerDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun stateDao(): StateDao
    abstract fun customerTypeDao(): CustomerTypeDao
    abstract fun customerGroupDao(): CustomerGroupDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CustomerDatabaseConstructor : RoomDatabaseConstructor<CustomerDatabase> {
    override fun initialize(): CustomerDatabase
}