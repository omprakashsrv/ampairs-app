package com.ampairs.order.db

import androidx.paging.PagingSource
import androidx.room.Transaction
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.flower_core.networkResource
import com.ampairs.common.model.Response
import com.ampairs.customer.db.dao.CustomerDao
import com.ampairs.customer.domain.asDomainModel
import com.ampairs.order.api.OrderApi
import com.ampairs.order.api.model.OrderApiModel
import com.ampairs.order.api.model.toApiModel
import com.ampairs.order.api.model.toOrderDatabaseModel
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.order.db.dto.asDatabaseModel
import com.ampairs.order.db.dto.asItemDatabaseModel
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.asDomainModel
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.domain.asDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn

class OrderRepository(
    val orderDao: OrderDao,
    val orderItemDao: OrderItemDao,
    val productDao: ProductDao,
    val customerDao: CustomerDao,
    val orderApi: OrderApi,
) {
    @Transaction
    suspend fun saveOrder(orderEntity: OrderEntity, orderItems: List<OrderItemEntity>) {
        orderDao.insert(orderEntity)
        orderItemDao.insertAll(orderItems)
        val order = getOrder(orderEntity.id)
        saveOrder(order)
    }

    suspend fun saveOrder(order: Order?) {
        order?.toApiModel()?.let {
            val orderResponse = orderApi.updateOrder(it)
            val updatedOrder = orderResponse.data
            updatedOrder?.toOrderDatabaseModel()?.let { it1 -> orderDao.insert(it1) }
            orderResponse
        }
    }

    suspend fun createInvoice(order: Order?) {
        order?.toApiModel()?.let {
            val orderResponse = orderApi.createInvoice(it)
            val updatedOrder = orderResponse.data
            updatedOrder?.toOrderDatabaseModel()?.let { it1 -> orderDao.insert(it1) }
            orderResponse
        }
    }

    suspend fun getOrder(id: String): Order {
        val orderWithItems = orderDao.getOrderById(id)
        if (orderWithItems == null) {
            throw Error("No order found with id $id")
        }

        val orderDomain = orderWithItems.order.asDomainModel()
        orderDomain.fromCustomer =
            orderDomain.fromCustomer?.id?.let { customerDao.selectById(it)?.asDomainModel() }
                ?: orderDomain.fromCustomer
        orderDomain.toCustomer =
            orderDomain.toCustomer?.id?.let { customerDao.selectById(it)?.asDomainModel() }
                ?: orderDomain.toCustomer

        val products =
            productDao.productsByIds(orderWithItems.orderItems.map { it.product_id })
                .map { it.asDomainModel() }
        orderDomain.items.forEach {
            val product = products.find { product -> product.id == it.product?.id }
            it.product = product ?: it.product
        }
        return orderDomain
    }

    fun getOrderResource(): Flow<Resource<List<OrderApiModel>>> {
        return networkResource(shouldMakeNetworkRequest = { true }, makeNetworkRequest = {
            val sharedFlow = MutableSharedFlow<Response<List<OrderApiModel>>>(replay = 10)
            var fetchSize = 1000
            while (fetchSize == 1000) {
                val lastUpdated = orderDao.getMaxLastUpdated() ?: 0
                val ordersResponse = orderApi.getOrders(lastUpdated)
                val orders = ordersResponse.data
                if (orders != null) {
                    orderDao.updateOrders(
                        orders.asDatabaseModel(), orders.asItemDatabaseModel()
                    )
                }
                fetchSize = ordersResponse.data?.size ?: 0
                sharedFlow.emit(ordersResponse)
            }
            sharedFlow
        }, processNetworkResponse = {

        }).flowOn(DispatcherProvider.io)
    }

    fun getOrders(searchText: String): PagingSource<Int, OrderEntity> {
        return orderDao.getOrdersBySearchPagingSource(searchText)
    }


}