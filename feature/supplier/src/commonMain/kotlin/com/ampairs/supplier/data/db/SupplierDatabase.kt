package com.ampairs.supplier.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [SupplierEntity::class],
    version = 1,
    exportSchema = true
)
@ConstructedBy(SupplierDatabaseConstructor::class)
abstract class SupplierDatabase : RoomDatabase() {
    abstract fun supplierDao(): SupplierDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SupplierDatabaseConstructor : RoomDatabaseConstructor<SupplierDatabase> {
    override fun initialize(): SupplierDatabase
}
