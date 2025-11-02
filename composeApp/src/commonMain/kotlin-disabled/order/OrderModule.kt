package com.ampairs.order

import com.ampairs.order.api.OrderApi
import com.ampairs.order.api.OrderApiImpl
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.db.OrderRoomDatabase
import com.ampairs.order.viewmodel.OrderViewModel
import com.ampairs.order.viewmodel.OrderViewViewModel
import com.ampairs.order.viewmodel.OrdersViewModel
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val orderModule: Module = module {
    single { OrderApiImpl(get(), get()) } bind (OrderApi::class)
    // Database is provided by platform-specific modules
    single { get<OrderRoomDatabase>().orderDao() }
    single { get<OrderRoomDatabase>().orderItemDao() }
    single { OrderRepository(get(), get(), get(), get(), get()) }

    // Direct ViewModel injection
    factory { OrdersViewModel(get()) }
    factory { (fromCustomerId: String?, toCustomerId: String?, id: String?) ->
        OrderViewModel(fromCustomerId, toCustomerId, id, get(), get(), get(), get(), get())
    }
    factory { (orderId: String) -> OrderViewViewModel(orderId, get()) }
}

fun orderModule() = orderModule