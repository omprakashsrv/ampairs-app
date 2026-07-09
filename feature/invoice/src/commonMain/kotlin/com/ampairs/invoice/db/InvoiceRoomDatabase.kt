package com.ampairs.invoice.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.ampairs.invoice.agent.InvoiceAgentDao
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity

@Database(
    entities = [
        InvoiceEntity::class,
        InvoiceItemEntity::class
    ],
    version = 6,
    exportSchema = true
)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
@ConstructedBy(InvoiceRoomDatabaseConstructor::class)
abstract class InvoiceRoomDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun invoiceAgentDao(): InvoiceAgentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object InvoiceRoomDatabaseConstructor : RoomDatabaseConstructor<InvoiceRoomDatabase> {
    override fun initialize(): InvoiceRoomDatabase
}