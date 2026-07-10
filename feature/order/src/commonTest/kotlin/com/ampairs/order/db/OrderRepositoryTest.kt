package com.ampairs.order.db

import androidx.paging.PagingSource
import com.ampairs.auth.domain.DeviceInfo
import com.ampairs.common.DeviceService
import com.ampairs.common.model.PageResponse
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.order.api.OrderApi
import com.ampairs.order.api.model.OrderApiModel
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity
import com.ampairs.order.db.model.OrderModel
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderStatus
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.domain.ProductSummary
import com.ampairs.product.domain.VariantOption
import com.ampairs.sequence.data.api.SequenceApi
import com.ampairs.sequence.data.db.dao.SequenceAllocationDao
import com.ampairs.sequence.data.db.dao.SequenceDefinitionDao
import com.ampairs.sequence.data.db.entity.SequenceAllocationEntity
import com.ampairs.sequence.data.db.entity.SequenceDefinitionEntity
import com.ampairs.sequence.data.repository.SequenceAllocationRepository
import com.ampairs.sequence.data.repository.SequenceRepository
import com.ampairs.sequence.domain.SequenceNumberProvider
import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.domain.model.SequenceAllocation
import com.ampairs.sequence.domain.model.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.model.SequenceAllocationRequest
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.common.model.Response
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Offline-first repository tests for [OrderRepository], modelled on `UnitRepositoryTest` /
 * `InvoiceRepositoryTest`: hand-written fakes for the DAOs / data services (MockK is JVM-only and
 * won't compile for the iOS/native test targets), `runTest` for the suspend calls. No real Room or
 * network involved.
 *
 * [SequenceNumberProvider] is a concrete (final) class, so we drive a REAL provider built from real
 * [SequenceAllocationRepository] / [SequenceRepository] whose leaf interfaces (DAOs, [SequenceApi],
 * [DeviceService], [SyncStateDao]) are faked. This exercises the genuine numbering logic:
 *   - a usable block present → consume it and format via SequenceFormatter (non-provisional);
 *   - no block + API request fails (offline) → provider returns a provisional number, and the
 *     repository falls back to its local "ORD-####" counter.
 *
 * The invariants pinned here mirror `/offline-sync`:
 *   1. Every local write persists to Room as `synced = 0`.
 *   2. Every successful save flags the entity PENDING_PUSH via SyncStateDao.
 *   3. The create/edit path never touches [OrderApi].
 *   4. Client-assigned order numbering (design v2): sequence-block first, local ORD-#### fallback.
 */
class OrderRepositoryTest {

    // ── helpers to build a numbering provider in two modes ──

    /** Provider that hands out an exclusive server block → non-provisional formatted numbers. */
    private fun providerWithBlock(
        prefix: String? = "ORD",
        padding: Int = 4,
        nextAvailable: Long = 7,
        rangeEnd: Long = 50,
    ): SequenceNumberProvider {
        val allocation = SequenceAllocationEntity(
            uid = "ALLOC1",
            definitionUid = "DEF1",
            entityType = "order",
            deviceId = "DEV1",
            rangeStart = 1,
            rangeEnd = rangeEnd,
            nextAvailable = nextAvailable,
            status = AllocationStatus.ACTIVE,
            prefix = prefix,
            suffix = null,
            paddingLength = padding,
            incrementStep = 1,
            synced = true,
        )
        return buildProvider(FakeSequenceAllocationDao(usable = allocation), FakeSequenceDefinitionDao(), FailingSequenceApi())
    }

    /** Provider with no block and a failing (offline) API → provisional numbers. */
    private fun providerProvisional(): SequenceNumberProvider =
        buildProvider(FakeSequenceAllocationDao(usable = null), FakeSequenceDefinitionDao(), FailingSequenceApi())

    private fun buildProvider(
        allocationDao: SequenceAllocationDao,
        definitionDao: SequenceDefinitionDao,
        api: SequenceApi,
    ): SequenceNumberProvider {
        val sync = RecordingSyncStateDao()
        val allocationRepo = SequenceAllocationRepository(allocationDao, api, sync)
        val definitionRepo = SequenceRepository(definitionDao, sync)
        return SequenceNumberProvider(allocationRepo, definitionRepo, FakeDeviceService())
    }

    private fun repo(
        orderDao: FakeOrderDao = FakeOrderDao(),
        orderItemDao: FakeOrderItemDao = FakeOrderItemDao(),
        sync: RecordingSyncStateDao = RecordingSyncStateDao(),
        sequence: SequenceNumberProvider = providerWithBlock(),
        api: OrderApi = ThrowingOrderApi(),
        products: ProductDataService = FakeProductDataService(),
        customers: CustomerDataService = FakeCustomerDataService(),
    ) = OrderRepository(
        orderDao = orderDao,
        orderItemDao = orderItemDao,
        productDataService = products,
        customerDataService = customers,
        orderApi = api,
        syncStateDao = sync,
        sequenceNumberProvider = sequence,
    )

    private fun orderEntity(
        id: String = "ORD1",
        number: String = "",
        status: String = OrderStatus.NEW.name,
        total: Double = 100.0,
        customerId: String = "CUS1",
    ) = OrderEntity(
        id = id,
        order_number = number,
        order_date = "2026-01-01 10:00:00",
        status = status,
        customer_id = customerId,
        customer_name = "Acme",
        customer_gst = "",
        total_cost = total,
        total_tax = 0.0,
        total_items = 1,
        total_quantity = 1.0,
        base_price = total,
    )

    private fun item(id: String = "OIT1", orderId: String = "ORD1") = OrderItemEntity(
        id = id,
        description = "Widget",
        product_id = "PRD1",
        total_cost = 100.0,
        base_price = 100.0,
        product_price = 100.0,
        total_tax = 0.0,
        order_id = orderId,
        tax_code = "1234",
        quantity = 1.0,
        item_no = 1,
        selling_price = 100.0,
        mrp = 0.0,
        dp = 0.0,
    )

    // ── save: persistence + sync flag ──

    @Test
    fun `saveOrder persists order and items as unsynced and flags pending push`() = runTest {
        val orderDao = FakeOrderDao()
        val itemDao = FakeOrderItemDao()
        val sync = RecordingSyncStateDao()
        val repository = repo(orderDao, itemDao, sync)

        repository.saveOrder(orderEntity(number = "ORD-0001"), listOf(item()))

        val stored = orderDao.selectById("ORD1")
        assertEquals("ORD-0001", stored?.order_number)
        assertEquals(0L, stored?.synced, "a freshly written order must be unsynced so the push picks it up")
        assertEquals(listOf(item()), itemDao.getOrderItems("ORD1"))
        assertEquals(listOf(SyncEntity.ORDER), sync.pendingPushes)
    }

    @Test
    fun `saveOrder never touches the order api`() = runTest {
        // ThrowingOrderApi makes any network call fail the test — the create path must be local-only.
        val repository = repo()
        repository.saveOrder(orderEntity(number = "ORD-0001"), listOf(item()))
        // Reaching here without an exception proves the API was not called.
    }

    // ── numbering: sequence provider (block) path ──

    @Test
    fun `saveOrder assigns a sequence-module number when the order number is blank`() = runTest {
        val orderDao = FakeOrderDao()
        val repository = repo(
            orderDao = orderDao,
            sequence = providerWithBlock(prefix = "ORD", padding = 4, nextAvailable = 7),
        )

        repository.saveOrder(orderEntity(number = ""), listOf())

        // SequenceFormatter: "{prefix}-{paddedValue}" → "ORD-0007".
        assertEquals("ORD-0007", orderDao.selectById("ORD1")!!.order_number)
    }

    @Test
    fun `saveOrder keeps an already-assigned number untouched`() = runTest {
        val orderDao = FakeOrderDao()
        // A block exists, but it must not be consulted when a number is already present.
        val repository = repo(orderDao = orderDao, sequence = providerWithBlock(nextAvailable = 99))

        repository.saveOrder(orderEntity(number = "MANUAL/5"), listOf())

        assertEquals("MANUAL/5", orderDao.selectById("ORD1")!!.order_number)
    }

    // ── numbering: provisional fallback (offline, local ORD-#### counter) ──

    @Test
    fun `saveOrder falls back to the local ORD counter when the block is provisional`() = runTest {
        val orderDao = FakeOrderDao()
        orderDao.maxOrderSeq = 41L
        val repository = repo(orderDao = orderDao, sequence = providerProvisional())

        repository.saveOrder(orderEntity(number = ""), listOf())

        // next = max(41) + 1, padded to 4 → "ORD-0042".
        assertEquals("ORD-0042", orderDao.selectById("ORD1")!!.order_number)
    }

    @Test
    fun `saveOrder provisional fallback starts at ORD-0001 for an empty db`() = runTest {
        val orderDao = FakeOrderDao()
        val repository = repo(orderDao = orderDao, sequence = providerProvisional())

        repository.saveOrder(orderEntity(number = ""), listOf())

        assertEquals("ORD-0001", orderDao.selectById("ORD1")!!.order_number)
    }

    // ── nextNumberPreview ──

    @Test
    fun `nextNumberPreview prefers the sequence provider peek`() = runTest {
        // peek returns the block's nextAvailable without consuming it.
        val repository = repo(sequence = providerWithBlock(prefix = "ORD", padding = 4, nextAvailable = 12))

        assertEquals("ORD-0012", repository.nextNumberPreview())
    }

    @Test
    fun `nextNumberPreview falls back to the local counter when peek is null`() = runTest {
        val orderDao = FakeOrderDao()
        orderDao.maxOrderSeq = 9L
        // No block + offline API → peek() returns null → local ORD-#### path.
        val repository = repo(orderDao = orderDao, sequence = providerProvisional())

        assertEquals("ORD-0010", repository.nextNumberPreview())
    }

    @Test
    fun `nextNumberPreview uses ORD-0001 for an empty db on the fallback path`() = runTest {
        val repository = repo(sequence = providerProvisional())

        assertEquals("ORD-0001", repository.nextNumberPreview())
    }

    // ── nullable Order overload ──

    @Test
    fun `saveOrder(null) is a no-op`() = runTest {
        val sync = RecordingSyncStateDao()
        val repository = repo(sync = sync)

        repository.saveOrder(null as Order?)

        assertTrue(sync.pendingPushes.isEmpty())
    }

    @Test
    fun `saveOrder(Order) maps and persists the aggregate as unsynced`() = runTest {
        val orderDao = FakeOrderDao()
        val sync = RecordingSyncStateDao()
        val repository = repo(orderDao = orderDao, sync = sync)

        val order = Order().apply {
            id = "ORD9"
            orderNumber = "ORD-0009"
            status = OrderStatus.NEW
            customer = Customer(uid = "CUS1", name = "Acme")
        }
        repository.saveOrder(order)

        val stored = orderDao.selectById("ORD9")!!
        assertEquals("ORD-0009", stored.order_number)
        assertEquals("CUS1", stored.customer_id)
        assertEquals(0L, stored.synced)
        assertEquals(listOf(SyncEntity.ORDER), sync.pendingPushes)
    }

    // ── getOrder ──

    @Test
    fun `getOrder throws when the id is unknown`() = runTest {
        val repository = repo()
        assertFailsWith<Throwable> { repository.getOrder("missing") }
    }

    @Test
    fun `getOrder maps the aggregate and re-attaches the catalog customer`() = runTest {
        val orderDao = FakeOrderDao()
        orderDao.rows["ORD1"] = orderEntity(id = "ORD1", number = "ORD-0001", customerId = "CUS1")
        orderDao.items["ORD1"] = listOf(item())
        val customers = FakeCustomerDataService(
            byId = mapOf("CUS1" to Customer(uid = "CUS1", name = "Acme From Catalog", gstNumber = "GST123")),
        )
        val products = FakeProductDataService(
            byId = mapOf("PRD1" to ProductSummary(id = "PRD1", name = "Catalog Widget")),
        )
        val repository = repo(orderDao = orderDao, products = products, customers = customers)

        val order = repository.getOrder("ORD1")

        assertEquals("ORD-0001", order.orderNumber)
        // Buyer re-attached from the catalog (snapshot name replaced by the live record).
        assertEquals("Acme From Catalog", order.customer?.name)
        assertEquals(1, order.items.size)
        // Loaded item carries only productId → the catalog product is re-attached.
        assertEquals("Catalog Widget", order.items.first().product?.name)
    }

    // ── isOrderSynced ──

    @Test
    fun `isOrderSynced reflects the stored synced flag`() = runTest {
        val orderDao = FakeOrderDao()
        orderDao.rows["A"] = orderEntity(id = "A", number = "ORD-1").copy(synced = 1)
        orderDao.rows["B"] = orderEntity(id = "B", number = "ORD-2").copy(synced = 0)
        val repository = repo(orderDao = orderDao)

        assertTrue(repository.isOrderSynced("A"))
        assertFalse(repository.isOrderSynced("B"))
        assertFalse(repository.isOrderSynced("missing"))
    }
}

// ─────────────────────────────── Fakes ───────────────────────────────

/** In-memory [OrderDao]; only the methods the repository touches are functional. */
private class FakeOrderDao : OrderDao {
    val rows = mutableMapOf<String, OrderEntity>()
    val items = mutableMapOf<String, List<OrderItemEntity>>()
    var maxOrderSeq: Long? = null

    override suspend fun selectById(id: String): OrderEntity? = rows[id]
    override suspend fun getOrderById(id: String): OrderModel? =
        rows[id]?.let { OrderModel(order = it, orderItems = items[id] ?: emptyList()) }
    override suspend fun insert(order: OrderEntity) { rows[order.id] = order }
    override suspend fun insertAll(orders: List<OrderEntity>) { orders.forEach { rows[it.id] = it } }
    override suspend fun insertOrderItems(orders: List<OrderItemEntity>) {}
    override suspend fun maxOrderSequence(): Long? = maxOrderSeq

    override suspend fun selectAll(): List<OrderEntity> = rows.values.filter { it.active == 1L }
    override suspend fun getOrdersByCustomer(customerId: String): List<OrderEntity> = emptyList()
    override suspend fun getOrdersByToCustomer(customerId: String): List<OrderEntity> = emptyList()
    override suspend fun getOrdersByStatus(status: String): List<OrderEntity> = emptyList()
    override suspend fun getOrdersByNumber(searchText: String): List<OrderEntity> = emptyList()
    override suspend fun getOrdersByCustomerName(searchText: String): List<OrderEntity> = emptyList()
    override suspend fun getOrdersByDateRange(startDate: String, endDate: String): List<OrderEntity> = emptyList()
    override suspend fun getMaxLastUpdated(): Long? = null
    override suspend fun countOrders(): Int = rows.size
    override suspend fun countOrdersByCustomer(customerId: String): Int = 0
    override suspend fun countOrdersByStatus(status: String): Int = 0
    override suspend fun getOrdersPaginated(limit: Long, offset: Long): List<OrderEntity> = emptyList()
    override suspend fun getUnsyncedOrders(): List<OrderEntity> = rows.values.filter { it.synced == 0L && it.active == 1L }
    override suspend fun getTotalOrderValue(): Double? = null
    override suspend fun getTotalOrderValueByCustomer(customerId: String): Double? = null
    override suspend fun getTotalOrderValueByDateRange(startDate: String, endDate: String): Double? = null
    override suspend fun update(order: OrderEntity) { rows[order.id] = order }
    override suspend fun markAsSynced(id: String) {}
    override suspend fun updateStatus(id: String, status: String) {}
    override suspend fun softDelete(id: String) {}
    override suspend fun deleteById(id: String) { rows.remove(id) }
    override suspend fun deleteOrderItemsByIds(ids: List<String>) {}
    override suspend fun deleteInactiveOrderItems(orderId: String) {}
    override suspend fun deleteAll() { rows.clear() }
    override fun getAllOrdersPagingSource(): PagingSource<Int, OrderEntity> = throw NotImplementedError()
    override fun getOrdersByCustomerPagingSource(customerId: String): PagingSource<Int, OrderEntity> = throw NotImplementedError()
    override fun getOrdersBySearchPagingSource(searchText: String): PagingSource<Int, OrderEntity> = throw NotImplementedError()
    override fun getOrdersFilteredPagingSource(
        searchText: String,
        status: String,
        invoicedOnly: Int,
        unsyncedOnly: Int,
    ): PagingSource<Int, OrderEntity> = throw NotImplementedError()
}

/** In-memory [OrderItemDao]; the repository writes via insertAll and reads via getOrderItems. */
private class FakeOrderItemDao : OrderItemDao {
    private val rows = mutableMapOf<String, OrderItemEntity>()

    override suspend fun insertAll(orderItems: List<OrderItemEntity>) { orderItems.forEach { rows[it.id] = it } }
    override suspend fun getOrderItems(orderId: String): List<OrderItemEntity> =
        rows.values.filter { it.order_id == orderId && it.active == 1 }.sortedBy { it.item_no }
    override suspend fun getAllOrderItemsRaw(orderId: String): List<OrderItemEntity> =
        rows.values.filter { it.order_id == orderId }.sortedBy { it.item_no }

    override suspend fun selectById(id: String): OrderItemEntity? = rows[id]
    override suspend fun selectAll(): List<OrderItemEntity> = rows.values.toList()
    override suspend fun getItemsByProduct(productId: String): List<OrderItemEntity> = emptyList()
    override suspend fun getItemsByOrderIds(orderIds: List<String>): List<OrderItemEntity> = emptyList()
    override suspend fun getItemsByDescription(searchText: String): List<OrderItemEntity> = emptyList()
    override suspend fun countItemsByOrder(orderId: String): Int = 0
    override suspend fun countItemsByProduct(productId: String): Int = 0
    override suspend fun countAllItems(): Int = rows.size
    override suspend fun getTotalQuantityByOrder(orderId: String): Double? = null
    override suspend fun getTotalCostByOrder(orderId: String): Double? = null
    override suspend fun getTotalTaxByOrder(orderId: String): Double? = null
    override suspend fun getTotalQuantityByProduct(productId: String): Double? = null
    override suspend fun getMaxItemNumberByOrder(orderId: String): Long? = null
    override suspend fun getDistinctTaxCodes(): List<String> = emptyList()
    override suspend fun getOrderItemsPaginated(orderId: String, limit: Long, offset: Long): List<OrderItemEntity> = emptyList()
    override suspend fun insert(orderItem: OrderItemEntity) { rows[orderItem.id] = orderItem }
    override suspend fun update(orderItem: OrderItemEntity) { rows[orderItem.id] = orderItem }
    override suspend fun updateQuantityAndCost(id: String, quantity: Double, totalCost: Double) {}
    override suspend fun softDelete(id: String) {}
    override suspend fun softDeleteByOrderId(orderId: String) {}
    override suspend fun deleteById(id: String) { rows.remove(id) }
    override suspend fun deleteByOrderId(orderId: String) {
        rows.values.filter { it.order_id == orderId }.forEach { rows.remove(it.id) }
    }
    override suspend fun deleteAll() { rows.clear() }
    override fun getOrderItemsPagingSource(orderId: String): PagingSource<Int, OrderItemEntity> = throw NotImplementedError()
    override fun getItemsByProductPagingSource(productId: String): PagingSource<Int, OrderItemEntity> = throw NotImplementedError()
    override fun getAllItemsPagingSource(): PagingSource<Int, OrderItemEntity> = throw NotImplementedError()
}

/** [SyncStateDao] recording markPendingPush calls. */
private class RecordingSyncStateDao : SyncStateDao {
    val pendingPushes = mutableListOf<SyncEntity>()

    override fun observeAll(): Flow<List<SyncStateEntity>> = flowOf(emptyList())
    override fun observe(entity: SyncEntity): Flow<SyncStateEntity?> = flowOf(null)
    override suspend fun getAll(): List<SyncStateEntity> = emptyList()
    override suspend fun getPending(): List<SyncStateEntity> = emptyList()
    override suspend fun upsert(state: SyncStateEntity) {}
    override suspend fun getLastSyncedAtIso(entity: SyncEntity): String? = null
    override suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String) {}
    override suspend fun markPendingPush(entity: SyncEntity, now: Long) { pendingPushes += entity }
    override suspend fun deleteAll() {}
}

/** Allocation DAO that hands back one (optional) usable block; insertAllocation is captured but unused. */
private class FakeSequenceAllocationDao(
    private val usable: SequenceAllocationEntity?,
) : SequenceAllocationDao {
    var current: SequenceAllocationEntity? = usable
    override suspend fun getUsableAllocation(entityType: String, deviceId: String): SequenceAllocationEntity? = current
    override suspend fun getByUid(uid: String): SequenceAllocationEntity? = current?.takeIf { it.uid == uid }
    override suspend fun getUnsyncedAllocations(): List<SequenceAllocationEntity> = emptyList()
    override suspend fun insertAllocation(allocation: SequenceAllocationEntity) { current = allocation }
    override suspend fun insertAllocations(allocations: List<SequenceAllocationEntity>) {}
}

private class FakeSequenceDefinitionDao(
    private val workspaceDefinition: SequenceDefinitionEntity? = null,
) : SequenceDefinitionDao {
    override fun observeActiveDefinitions(): Flow<List<SequenceDefinitionEntity>> = flowOf(emptyList())
    override suspend fun getByUid(uid: String): SequenceDefinitionEntity? = null
    override suspend fun getActiveWorkspaceDefinition(entityType: String): SequenceDefinitionEntity? = workspaceDefinition
    override suspend fun getUnsyncedDefinitions(): List<SequenceDefinitionEntity> = emptyList()
    override suspend fun insertDefinition(definition: SequenceDefinitionEntity) {}
    override suspend fun insertDefinitions(definitions: List<SequenceDefinitionEntity>) {}
    override suspend fun hardDeleteDefinition(uid: String) {}
}

/** Always-throwing API: simulates the offline path so block requests fail → provisional numbers. */
private class FailingSequenceApi : SequenceApi {
    override suspend fun getDefinitionsSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<SequenceDefinition> =
        throw UnsupportedOperationException("offline")
    override suspend fun bulkUpdateDefinitions(definitions: List<SequenceDefinition>): List<SequenceDefinition> =
        throw UnsupportedOperationException("offline")
    override suspend fun requestAllocation(request: SequenceAllocationRequest): SequenceAllocation =
        throw UnsupportedOperationException("offline")
    override suspend fun reportAllocations(reports: List<SequenceAllocationReportRequest>): List<SequenceAllocation> =
        throw UnsupportedOperationException("offline")
    override suspend fun getDeviceAllocations(deviceId: String, status: String?): List<SequenceAllocation> =
        throw UnsupportedOperationException("offline")
}

private class FakeDeviceService : DeviceService {
    override fun getDeviceInfo(): DeviceInfo = throw NotImplementedError()
    override fun getDeviceId(): String = "DEV1"
    override fun generateDeviceId(): String = "DEV1"
    override fun clearDeviceId() {}
}

private class FakeProductDataService(
    private val byId: Map<String, ProductSummary> = emptyMap(),
) : ProductDataService {
    override suspend fun getById(uid: String): ProductSummary? = byId[uid]
    override suspend fun getByIds(ids: List<String>): List<ProductSummary> = ids.mapNotNull { byId[it] }
    override suspend fun searchSummaries(term: String, limit: Int): List<ProductSummary> = emptyList()
    override suspend fun variantsForProduct(productId: String): List<VariantOption> = emptyList()
    override suspend fun quickCreate(
        id: String,
        name: String,
        code: String,
        sellingPrice: Double,
        mrp: Double,
        taxCode: String,
        baseUnitId: String?,
    ): ProductSummary? = null
}

private class FakeCustomerDataService(
    private val byId: Map<String, Customer> = emptyMap(),
) : CustomerDataService {
    override suspend fun getById(uid: String): Customer? = byId[uid]
    override suspend fun listCustomers(query: String): List<CustomerListItem> = emptyList()
}

/** Order API that fails any call — the create/edit path must never reach the network. */
private class ThrowingOrderApi : OrderApi {
    override suspend fun createInvoice(order: OrderApiModel): Response<OrderApiModel> =
        throw AssertionError("OrderRepository must not call the API on the create/edit path")
    override suspend fun bulkUpdateOrders(orders: List<OrderApiModel>): List<OrderApiModel> =
        throw AssertionError("OrderRepository must not call the API on the create/edit path")
    override suspend fun getOrdersSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<OrderApiModel> =
        throw AssertionError("OrderRepository must not call the API on the create/edit path")
}
