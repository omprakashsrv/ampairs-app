package com.ampairs.order.db

import androidx.paging.PagingSource
import androidx.room.Transaction
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.flower_core.networkResource
import com.ampairs.common.model.Response
import com.ampairs.customer.data.CustomerDataService
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
import com.ampairs.order.domain.asDatabaseModel as orderAsEntity
import com.ampairs.order.domain.asDomainModel
import com.ampairs.product.data.ProductDataService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.Clock

/**
 * Local-only order repository (offline-first, spec 010). Writes go to Room and flag the entity
 * PENDING_PUSH; the [com.ampairs.order.sync.OrderSyncDelegate] owns all order ↔ server traffic.
 *
 * NOTE (intermediate state): [createInvoice] (order→invoice conversion, refactored in A3/T024) and
 * [getOrderResource] (legacy list pull, replaced by syncService TriggerPull in A3) still reference
 * [orderApi]. The create/edit path no longer touches the network.
 */
@Inject
class OrderRepository(
    val orderDao: OrderDao,
    val orderItemDao: OrderItemDao,
    val productDataService: ProductDataService,
    val customerDataService: CustomerDataService,
    val orderApi: OrderApi,
    val syncStateDao: SyncStateDao,
) {
    @Transaction
    suspend fun saveOrder(orderEntity: OrderEntity, orderItems: List<OrderItemEntity>) {
        orderDao.insert(orderEntity.copy(synced = 0))
        orderItemDao.insertAll(orderItems)
        markPending()
    }

    suspend fun saveOrder(order: Order?) {
        val o = order ?: return
        saveOrder(o.orderAsEntity(), o.items.orderAsEntity(o.id))
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.ORDER, Clock.System.now().toEpochMilliseconds())

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
            orderDomain.fromCustomer?.uid?.let { customerDataService.getById(it) }
                ?: orderDomain.fromCustomer
        orderDomain.toCustomer =
            orderDomain.toCustomer?.uid?.let { customerDataService.getById(it) }
                ?: orderDomain.toCustomer

        val products =
            productDataService.getByIds(orderWithItems.orderItems.map { it.product_id })
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
