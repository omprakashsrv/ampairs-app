package com.ampairs.order

import com.ampairs.common.di.AppScope
import com.ampairs.order.db.OrderRoomDatabase
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface OrderDaoModule {
    companion object {
        @Provides
        fun provideOrderDao(db: OrderRoomDatabase): OrderDao = db.orderDao()

        @Provides
        fun provideOrderItemDao(db: OrderRoomDatabase): OrderItemDao = db.orderItemDao()
    }
}
