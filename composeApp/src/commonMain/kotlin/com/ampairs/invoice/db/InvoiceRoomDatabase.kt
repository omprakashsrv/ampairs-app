package com.ampairs.invoice.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity

@Database(
    entities = [
        InvoiceEntity::class,
        InvoiceItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(InvoiceRoomDatabaseConstructor::class)
abstract class InvoiceRoomDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object InvoiceRoomDatabaseConstructor : RoomDatabaseConstructor<InvoiceRoomDatabase>