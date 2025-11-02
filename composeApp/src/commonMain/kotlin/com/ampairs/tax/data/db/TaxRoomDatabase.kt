package com.ampairs.tax.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        HsnCodeEntity::class,
        TaxRateEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(TaxRoomDatabaseConstructor::class)
abstract class TaxRoomDatabase : RoomDatabase() {
    abstract fun hsnCodeDao(): HsnCodeDao
    abstract fun taxRateDao(): TaxRateDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TaxRoomDatabaseConstructor : RoomDatabaseConstructor<TaxRoomDatabase>