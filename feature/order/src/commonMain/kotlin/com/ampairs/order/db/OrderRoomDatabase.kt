package com.ampairs.order.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.ampairs.order.agent.OrderAgentDao
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 6,
    exportSchema = true
)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
@ConstructedBy(OrderRoomDatabaseConstructor::class)
abstract class OrderRoomDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun orderAgentDao(): OrderAgentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object OrderRoomDatabaseConstructor : RoomDatabaseConstructor<OrderRoomDatabase> {
    override fun initialize(): OrderRoomDatabase
}