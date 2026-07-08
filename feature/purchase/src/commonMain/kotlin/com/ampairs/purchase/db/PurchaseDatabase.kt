package com.ampairs.purchase.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.purchase.db.dao.PurchaseDao
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity

@Database(
    entities = [
        PurchaseEntity::class,
        PurchaseItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(PurchaseDatabaseConstructor::class)
abstract class PurchaseDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PurchaseDatabaseConstructor : RoomDatabaseConstructor<PurchaseDatabase> {
    override fun initialize(): PurchaseDatabase
}
