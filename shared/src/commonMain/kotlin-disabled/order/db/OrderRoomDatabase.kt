package com.ampairs.order.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(OrderRoomDatabaseConstructor::class)
abstract class OrderRoomDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object OrderRoomDatabaseConstructor : RoomDatabaseConstructor<OrderRoomDatabase>