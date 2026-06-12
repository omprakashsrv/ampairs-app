package com.ampairs.order.db

import androidx.paging.PagingSource
import androidx.room.Transaction
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.order.api.OrderApi
import com.ampairs.order.api.model.toApiModel
import com.ampairs.order.api.model.toOrderDatabaseModel
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.asDatabaseModel as orderAsEntity
import com.ampairs.order.domain.asDomainModel
import com.ampairs.product.data.ProductDataService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

/**
 * Local-only order repository (offline-first, spec 010). Writes go to Room and flag the entity
 * PENDING_PUSH; the [com.ampairs.order.sync.OrderSyncDelegate] owns all order ↔ server traffic.
 * The list pull is now driven by CentralSyncService (TriggerPull) via [OrderSyncDelegate].
 *
 * NOTE: [createInvoice] (order→invoice conversion) still references [orderApi]; that is the only
 * remaining network call in this repository. The create/edit path no longer touches the network.
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
        val numbered = if (orderEntity.order_number.isBlank()) assignOrderNumber(orderEntity) else orderEntity
        orderDao.insert(numbered.copy(synced = 0))
        orderItemDao.insertAll(orderItems)
        markPending()
    }

    /** Client-assigned sequential order number at save (design v2 flow): ORD-0001, ORD-0002, … */
    private suspend fun assignOrderNumber(entity: OrderEntity): OrderEntity {
        val seq = (orderDao.maxOrderSequence() ?: 0L) + 1L
        return entity.copy(order_number = "ORD-" + seq.toString().padStart(4, '0'))
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

        // Map the full aggregate (order + items) — the OrderEntity-only mapper sets items = empty.
        val orderDomain = orderWithItems.asDomainModel()
        orderDomain.fromCustomer =
            orderDomain.fromCustomer?.uid?.let { customerDataService.getById(it) }
                ?: orderDomain.fromCustomer
        orderDomain.toCustomer =
            orderDomain.toCustomer?.uid?.let { customerDataService.getById(it) }
                ?: orderDomain.toCustomer

        val products =
            productDataService.getByIds(orderWithItems.orderItems.map { it.product_id })
        orderDomain.items.forEach { item ->
            // Loaded items carry only productId — re-attach the catalog product (name/HSN/variants).
            item.product = products.find { product -> product.id == item.productId } ?: item.product
        }
        // The customer assignments above fire the legacy toCustomer setter, which re-derives
        // taxSpec incorrectly. The persisted tax data is authoritative — restore it last.
        (orderDomain.taxInfos?.firstOrNull() ?: orderDomain.items.firstOrNull()?.taxInfos?.firstOrNull())
            ?.let { orderDomain.taxSpec = it.taxSpec }
        return orderDomain
    }

    fun getOrders(searchText: String): PagingSource<Int, OrderEntity> {
        return orderDao.getOrdersBySearchPagingSource(searchText)
    }

    /** Whether the row has reached the server (drives the order view's sync chip). */
    suspend fun isOrderSynced(id: String): Boolean = orderDao.selectById(id)?.synced == 1L

    /** Order list v2: search (number/buyer/seller) + status / invoiced / offline filters. */
    fun getOrdersFiltered(
        searchText: String,
        status: String,
        invoicedOnly: Boolean,
        unsyncedOnly: Boolean,
    ): PagingSource<Int, OrderEntity> = orderDao.getOrdersFilteredPagingSource(
        searchText = searchText.trim(),
        status = status,
        invoicedOnly = if (invoicedOnly) 1 else 0,
        unsyncedOnly = if (unsyncedOnly) 1 else 0,
    )
}
