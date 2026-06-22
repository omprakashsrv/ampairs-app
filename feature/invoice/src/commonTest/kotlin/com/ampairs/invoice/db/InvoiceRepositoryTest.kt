package com.ampairs.invoice.db

import androidx.paging.PagingSource
import com.ampairs.common.DeviceService
import com.ampairs.auth.domain.DeviceInfo
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.invoice.domain.InvoiceStatus
import com.ampairs.invoice.spi.FinalizedInvoice
import com.ampairs.invoice.spi.InvoiceLifecycleListener
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
import com.ampairs.common.model.PageResponse
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Offline-first repository tests for [InvoiceRepository], modelled on `UnitRepositoryTest`:
 * hand-written fakes for the DAOs / data services (MockK is JVM-only and won't compile for the
 * iOS/native test targets), `runTest` for the suspend calls. No real Room or network involved.
 *
 * [SequenceNumberProvider] is a concrete (final) class, so we drive a REAL provider built from real
 * [SequenceAllocationRepository] / [SequenceRepository] whose leaf interfaces (DAOs, [SequenceApi],
 * [DeviceService], [SyncStateDao]) are faked. This exercises the genuine numbering logic:
 *   - a usable block present → consume it and format via SequenceFormatter (non-provisional);
 *   - no block + API request fails (offline) → provider returns a provisional number, and the
 *     repository falls back to its local "{series}/{seq}" counter.
 *
 * The invariants pinned here mirror `/offline-sync`:
 *   1. Every local write persists to Room as `synced = 0`.
 *   2. Every successful save flags the entity PENDING_PUSH via SyncStateDao.
 *   3. Client-assigned numbering (spec 010 C4/C5).
 *   4. Local billed/un-billed transitions bridge to the lifecycle listeners (spec 013 R9).
 */
class InvoiceRepositoryTest {

    // ── helpers to build a provider in two modes ──

    /** Provider that hands out an exclusive server block → non-provisional formatted numbers. */
    private fun providerWithBlock(
        entityType: String = "invoice",
        prefix: String? = "INV",
        padding: Int = 4,
        nextAvailable: Long = 7,
        rangeEnd: Long = 50,
    ): SequenceNumberProvider {
        val allocation = SequenceAllocationEntity(
            uid = "ALLOC1",
            definitionUid = "DEF1",
            entityType = entityType,
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
        val allocationDao = FakeSequenceAllocationDao(usable = allocation)
        return buildProvider(allocationDao, FakeSequenceDefinitionDao(), FailingSequenceApi())
    }

    /** Provider with no block and a failing (offline) API → provisional numbers. */
    private fun providerProvisional(workspacePrefix: String? = null): SequenceNumberProvider {
        val defDao = FakeSequenceDefinitionDao(
            workspaceDefinition = workspacePrefix?.let {
                SequenceDefinitionEntity(
                    uid = "DEF1",
                    entityType = "invoice",
                    scope = "WORKSPACE",
                    prefix = it,
                )
            }
        )
        return buildProvider(FakeSequenceAllocationDao(usable = null), defDao, FailingSequenceApi())
    }

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
        invoiceDao: FakeInvoiceDao = FakeInvoiceDao(),
        invoiceItemDao: FakeInvoiceItemDao = FakeInvoiceItemDao(),
        sync: RecordingSyncStateDao = RecordingSyncStateDao(),
        sequence: SequenceNumberProvider = providerWithBlock(),
        listener: RecordingLifecycleListener? = null,
        products: ProductDataService = FakeProductDataService(),
        customers: CustomerDataService = FakeCustomerDataService(),
    ) = InvoiceRepository(
        invoiceDao = invoiceDao,
        invoiceItemDao = invoiceItemDao,
        productDataService = products,
        customerDataService = customers,
        syncStateDao = sync,
        sequenceNumberProvider = sequence,
        lifecycleListeners = if (listener == null) emptySet() else setOf(listener),
    )

    private fun invoice(
        id: String = "INV1",
        number: String = "",
        status: String = InvoiceStatus.NEW.name,
        total: Double = 100.0,
        customerId: String = "CUS1",
        series: String = "",
    ) = InvoiceEntity(
        id = id,
        invoice_number = number,
        invoice_date = "2026-01-01 10:00:00",
        status = status,
        customer_id = customerId,
        customer_name = "Acme",
        customer_gst = "",
        total_cost = total,
        total_tax = 0.0,
        total_items = 1,
        total_quantity = 1.0,
        base_price = total,
        series = series,
    )

    private fun item(id: String = "IIT1", invoiceId: String = "INV1") = InvoiceItemEntity(
        id = id,
        description = "Widget",
        product_id = "PRD1",
        total_cost = 100.0,
        base_price = 100.0,
        product_price = 100.0,
        total_tax = 0.0,
        invoice_id = invoiceId,
        tax_code = "1234",
        quantity = 1.0,
        item_no = 1,
        selling_price = 100.0,
        mrp = 0.0,
        dp = 0.0,
    )

    // ── save: persistence + sync flag ──

    @Test
    fun `saveInvoice persists invoice and items as unsynced and flags pending push`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        val itemDao = FakeInvoiceItemDao()
        val sync = RecordingSyncStateDao()
        val repository = repo(invoiceDao, itemDao, sync)

        repository.saveInvoice(invoice(number = "INV/0001"), listOf(item()))

        val stored = invoiceDao.selectById("INV1")
        assertEquals("INV/0001", stored?.invoice_number)
        assertEquals(0L, stored?.synced, "a freshly written invoice must be unsynced so the push picks it up")
        assertEquals(listOf(item()), itemDao.getInvoiceItems("INV1"))
        assertEquals(listOf(SyncEntity.INVOICE), sync.pendingPushes)
    }

    // ── numbering: sequence provider (block) path ──

    @Test
    fun `saveInvoice assigns a sequence-module number when the invoice number is blank`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        val repository = repo(
            invoiceDao = invoiceDao,
            sequence = providerWithBlock(prefix = "INV", padding = 4, nextAvailable = 7),
        )

        repository.saveInvoice(invoice(number = ""), listOf())

        val stored = invoiceDao.selectById("INV1")!!
        assertEquals("INV-0007", stored.invoice_number, "SequenceFormatter: prefix-paddedValue")
        assertEquals(7L, stored.sequence_number)
        assertEquals("INV", stored.series)
    }

    @Test
    fun `saveInvoice keeps an already-assigned number untouched`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        // A block exists, but it must not be consulted when a number is already present.
        val repository = repo(invoiceDao = invoiceDao, sequence = providerWithBlock(nextAvailable = 99))

        repository.saveInvoice(invoice(number = "MANUAL/5"), listOf())

        assertEquals("MANUAL/5", invoiceDao.selectById("INV1")!!.invoice_number)
        assertEquals(0L, invoiceDao.selectById("INV1")!!.sequence_number, "sequence_number stays untouched")
    }

    // ── numbering: provisional fallback (offline, local counter) ──

    @Test
    fun `saveInvoice falls back to the local series counter when the block is provisional`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        invoiceDao.maxSequence["ACME"] = 41L
        val repository = repo(invoiceDao = invoiceDao, sequence = providerProvisional())

        repository.saveInvoice(invoice(number = "", series = "ACME"), listOf())

        val stored = invoiceDao.selectById("INV1")!!
        assertEquals(42L, stored.sequence_number, "next = max(41) + 1")
        assertEquals("ACME/0042", stored.invoice_number, "series/seq padded to 4")
        assertEquals("ACME", stored.series)
    }

    @Test
    fun `saveInvoice provisional fallback uses DEFAULT_SERIES and seq 1 for an empty db`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        val repository = repo(invoiceDao = invoiceDao, sequence = providerProvisional())

        repository.saveInvoice(invoice(number = "", series = ""), listOf())

        val stored = invoiceDao.selectById("INV1")!!
        assertEquals(1L, stored.sequence_number)
        assertEquals("INV/0001", stored.invoice_number)
        assertEquals("INV", stored.series)
    }

    // ── nextNumberPreview ──

    @Test
    fun `nextNumberPreview prefers the sequence provider peek`() = runTest {
        // peek returns the block's nextAvailable without consuming it.
        val repository = repo(sequence = providerWithBlock(prefix = "INV", padding = 4, nextAvailable = 12))

        assertEquals("INV-0012", repository.nextNumberPreview("INV"))
    }

    @Test
    fun `nextNumberPreview falls back to the local counter when peek is null`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        invoiceDao.maxSequence["INV"] = 9L
        // No block + offline API → peek() returns null → local counter path.
        val repository = repo(invoiceDao = invoiceDao, sequence = providerProvisional())

        assertEquals("INV/0010", repository.nextNumberPreview("INV"))
    }

    @Test
    fun `nextNumberPreview uses DEFAULT_SERIES for a null or blank series`() = runTest {
        val repository = repo(sequence = providerProvisional())

        assertEquals("INV/0001", repository.nextNumberPreview(null))
        assertEquals("INV/0001", repository.nextNumberPreview("   "))
    }

    // ── lifecycle listener bridge (spec 013 R9) ──

    @Test
    fun `saving a billed invoice posts the receivable to listeners`() = runTest {
        val listener = RecordingLifecycleListener()
        val repository = repo(listener = listener)

        repository.saveInvoice(invoice(status = InvoiceStatus.NEW.name, number = "INV/1", total = 250.0), listOf())

        assertEquals(1, listener.finalized.size)
        val posted: FinalizedInvoice = listener.finalized.single()
        assertEquals("INV1", posted.uid)
        assertEquals("CUS1", posted.partyUid)
        assertEquals(250.0, posted.totalCost)
        assertEquals("INV/1", posted.invoiceNumber)
        assertTrue(listener.reverted.isEmpty())
    }

    @Test
    fun `saving a DRAFT invoice does not post the receivable`() = runTest {
        val listener = RecordingLifecycleListener()
        val repository = repo(listener = listener)

        repository.saveInvoice(invoice(status = InvoiceStatus.DRAFT.name, number = "INV/1"), listOf())

        assertTrue(listener.finalized.isEmpty())
        assertTrue(listener.reverted.isEmpty())
    }

    @Test
    fun `a billed invoice with no customer is not posted`() = runTest {
        val listener = RecordingLifecycleListener()
        val repository = repo(listener = listener)

        repository.saveInvoice(invoice(status = InvoiceStatus.NEW.name, number = "INV/1", customerId = ""), listOf())

        assertTrue(listener.finalized.isEmpty())
    }

    @Test
    fun `re-saving the same billed total does not re-post`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        val listener = RecordingLifecycleListener()
        val repository = repo(invoiceDao = invoiceDao, listener = listener)

        repository.saveInvoice(invoice(status = InvoiceStatus.NEW.name, number = "INV/1", total = 100.0), listOf())
        repository.saveInvoice(invoice(status = InvoiceStatus.NEW.name, number = "INV/1", total = 100.0), listOf())

        assertEquals(1, listener.finalized.size, "billed → billed with unchanged total must not re-post")
    }

    @Test
    fun `editing a billed amount re-posts the receivable (idempotent refresh)`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        val listener = RecordingLifecycleListener()
        val repository = repo(invoiceDao = invoiceDao, listener = listener)

        repository.saveInvoice(invoice(status = InvoiceStatus.NEW.name, number = "INV/1", total = 100.0), listOf())
        repository.saveInvoice(invoice(status = InvoiceStatus.NEW.name, number = "INV/1", total = 175.0), listOf())

        assertEquals(2, listener.finalized.size)
        assertEquals(175.0, listener.finalized.last().totalCost)
    }

    @Test
    fun `reverting a billed invoice to DRAFT reverses the receivable`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        val listener = RecordingLifecycleListener()
        val repository = repo(invoiceDao = invoiceDao, listener = listener)

        repository.saveInvoice(invoice(status = InvoiceStatus.NEW.name, number = "INV/1"), listOf())
        repository.saveInvoice(invoice(status = InvoiceStatus.DRAFT.name, number = "INV/1"), listOf())

        assertEquals(listOf("INV1"), listener.reverted)
    }

    @Test
    fun `no listeners means save still succeeds and flags pending push`() = runTest {
        val sync = RecordingSyncStateDao()
        val repository = repo(sync = sync, listener = null)

        repository.saveInvoice(invoice(number = "INV/1"), listOf())

        assertEquals(listOf(SyncEntity.INVOICE), sync.pendingPushes)
    }

    // ── nullable Invoice overload ──

    @Test
    fun `saveInvoice(null) is a no-op`() = runTest {
        val sync = RecordingSyncStateDao()
        val repository = repo(sync = sync)

        repository.saveInvoice(null)

        assertTrue(sync.pendingPushes.isEmpty())
    }

    // ── simple getters ──

    @Test
    fun `getInvoice returns an empty Invoice when the id is unknown`() = runTest {
        val repository = repo()
        val result = repository.getInvoice("missing")
        assertTrue(result.items.isEmpty())
        assertNull(result.invoiceNumber)
    }

    @Test
    fun `isInvoiceSynced reflects the stored synced flag`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        invoiceDao.rows["A"] = invoice(id = "A", number = "INV/1").copy(synced = 1)
        invoiceDao.rows["B"] = invoice(id = "B", number = "INV/2").copy(synced = 0)
        val repository = repo(invoiceDao = invoiceDao)

        assertTrue(repository.isInvoiceSynced("A"))
        assertFalse(repository.isInvoiceSynced("B"))
        assertFalse(repository.isInvoiceSynced("missing"))
    }

    @Test
    fun `getInvoiceNumber returns null for blank or missing rows`() = runTest {
        val invoiceDao = FakeInvoiceDao()
        invoiceDao.rows["A"] = invoice(id = "A", number = "INV/9")
        invoiceDao.rows["B"] = invoice(id = "B", number = "")
        val repository = repo(invoiceDao = invoiceDao)

        assertEquals("INV/9", repository.getInvoiceNumber("A"))
        assertNull(repository.getInvoiceNumber("B"))
        assertNull(repository.getInvoiceNumber("missing"))
    }
}

// ─────────────────────────────── Fakes ───────────────────────────────

/** In-memory [InvoiceDao]; only the methods the repository touches are functional. */
private class FakeInvoiceDao : InvoiceDao {
    val rows = mutableMapOf<String, InvoiceEntity>()
    val maxSequence = mutableMapOf<String, Long>()

    override suspend fun selectById(id: String): InvoiceEntity? = rows[id]
    override suspend fun insert(invoice: InvoiceEntity) { rows[invoice.id] = invoice }
    override suspend fun insertAll(invoices: List<InvoiceEntity>) { invoices.forEach { rows[it.id] = it } }
    override suspend fun update(invoice: InvoiceEntity) { rows[invoice.id] = invoice }
    override suspend fun maxSequenceForSeries(series: String): Long? = maxSequence[series]

    override suspend fun selectAll(): List<InvoiceEntity> = rows.values.filter { it.active == 1L }
    override suspend fun getInvoicesByCustomer(customerId: String): List<InvoiceEntity> = emptyList()
    override suspend fun getInvoicesByToCustomer(customerId: String): List<InvoiceEntity> = emptyList()
    override suspend fun getInvoicesByStatus(status: String): List<InvoiceEntity> = emptyList()
    override suspend fun getInvoicesByNumber(searchText: String): List<InvoiceEntity> = emptyList()
    override suspend fun getInvoicesByCustomerName(searchText: String): List<InvoiceEntity> = emptyList()
    override suspend fun getInvoicesByDateRange(startDate: String, endDate: String): List<InvoiceEntity> = emptyList()
    override suspend fun getInvoicesByOrderRef(orderRefId: String): List<InvoiceEntity> = emptyList()
    override suspend fun getMaxLastUpdated(): Long? = null
    override suspend fun countInvoices(): Int = rows.size
    override suspend fun countInvoicesByCustomer(customerId: String): Int = 0
    override suspend fun countInvoicesByStatus(status: String): Int = 0
    override suspend fun getInvoicesPaginated(limit: Long, offset: Long): List<InvoiceEntity> = emptyList()
    override suspend fun getUnsyncedInvoices(): List<InvoiceEntity> = rows.values.filter { it.synced == 0L && it.active == 1L }
    override suspend fun getTotalInvoiceValue(): Double? = null
    override suspend fun getTotalInvoiceValueByCustomer(customerId: String): Double? = null
    override suspend fun getTotalInvoiceValueByDateRange(startDate: String, endDate: String): Double? = null
    override suspend fun getTotalTaxValue(): Double? = null
    override suspend fun getTotalTaxValueByCustomer(customerId: String): Double? = null
    override suspend fun getTotalTaxValueByDateRange(startDate: String, endDate: String): Double? = null
    override suspend fun markAsSynced(id: String) {}
    override suspend fun updateStatus(id: String, status: String) {}
    override suspend fun softDelete(id: String) {}
    override suspend fun deleteById(id: String) { rows.remove(id) }
    override suspend fun deleteAll() { rows.clear() }
    override fun getAllInvoicesPagingSource(): PagingSource<Int, InvoiceEntity> = throw NotImplementedError()
    override fun getInvoicesByCustomerPagingSource(customerId: String): PagingSource<Int, InvoiceEntity> = throw NotImplementedError()
    override fun getInvoicesBySearchPagingSource(searchText: String): PagingSource<Int, InvoiceEntity> = throw NotImplementedError()
    override fun getInvoicesFilteredPagingSource(
        searchText: String,
        status: String,
        fromOrderOnly: Int,
        unsyncedOnly: Int,
    ): PagingSource<Int, InvoiceEntity> = throw NotImplementedError()
    override fun getInvoicesByStatusPagingSource(status: String): PagingSource<Int, InvoiceEntity> = throw NotImplementedError()
}

/** In-memory [InvoiceItemDao]; the repository writes via insertAll and reads via getInvoiceItems. */
private class FakeInvoiceItemDao : InvoiceItemDao {
    private val rows = mutableMapOf<String, InvoiceItemEntity>()

    override suspend fun insertAll(invoiceItems: List<InvoiceItemEntity>) { invoiceItems.forEach { rows[it.id] = it } }
    override suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItemEntity> =
        rows.values.filter { it.invoice_id == invoiceId && it.active == 1L }.sortedBy { it.item_no }

    override suspend fun selectById(id: String): InvoiceItemEntity? = rows[id]
    override suspend fun selectAll(): List<InvoiceItemEntity> = rows.values.toList()
    override suspend fun getItemsByProduct(productId: String): List<InvoiceItemEntity> = emptyList()
    override suspend fun getItemsByInvoiceIds(invoiceIds: List<String>): List<InvoiceItemEntity> = emptyList()
    override suspend fun getItemsByDescription(searchText: String): List<InvoiceItemEntity> = emptyList()
    override suspend fun countItemsByInvoice(invoiceId: String): Int = 0
    override suspend fun countItemsByProduct(productId: String): Int = 0
    override suspend fun countAllItems(): Int = rows.size
    override suspend fun getTotalQuantityByInvoice(invoiceId: String): Double? = null
    override suspend fun getTotalCostByInvoice(invoiceId: String): Double? = null
    override suspend fun getTotalTaxByInvoice(invoiceId: String): Double? = null
    override suspend fun getTotalBasePriceByInvoice(invoiceId: String): Double? = null
    override suspend fun getTotalQuantityByProduct(productId: String): Double? = null
    override suspend fun getTotalRevenueByProduct(productId: String): Double? = null
    override suspend fun getMaxItemNumberByInvoice(invoiceId: String): Long? = null
    override suspend fun getDistinctTaxCodes(): List<String> = emptyList()
    override suspend fun getDistinctProducts(): List<String> = emptyList()
    override suspend fun getInvoiceItemsPaginated(invoiceId: String, limit: Long, offset: Long): List<InvoiceItemEntity> = emptyList()
    override suspend fun getItemsByTaxCode(taxCode: String): List<InvoiceItemEntity> = emptyList()
    override suspend fun getTotalCostByTaxCode(taxCode: String): Double? = null
    override suspend fun getTotalTaxByTaxCode(taxCode: String): Double? = null
    override suspend fun insert(invoiceItem: InvoiceItemEntity) { rows[invoiceItem.id] = invoiceItem }
    override suspend fun update(invoiceItem: InvoiceItemEntity) { rows[invoiceItem.id] = invoiceItem }
    override suspend fun updateQuantityAndCost(id: String, quantity: Double, totalCost: Double) {}
    override suspend fun updateSellingPriceAndCost(id: String, sellingPrice: Double, totalCost: Double) {}
    override suspend fun softDelete(id: String) {}
    override suspend fun softDeleteByInvoiceId(invoiceId: String) {}
    override suspend fun deleteById(id: String) { rows.remove(id) }
    override suspend fun deleteByInvoiceId(invoiceId: String) {
        rows.values.filter { it.invoice_id == invoiceId }.forEach { rows.remove(it.id) }
    }
    override suspend fun deleteAll() { rows.clear() }
    override fun getInvoiceItemsPagingSource(invoiceId: String): PagingSource<Int, InvoiceItemEntity> = throw NotImplementedError()
    override fun getItemsByProductPagingSource(productId: String): PagingSource<Int, InvoiceItemEntity> = throw NotImplementedError()
    override fun getAllItemsPagingSource(): PagingSource<Int, InvoiceItemEntity> = throw NotImplementedError()
    override fun getItemsByTaxCodePagingSource(taxCode: String): PagingSource<Int, InvoiceItemEntity> = throw NotImplementedError()
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

/** Records lifecycle bridge calls (spec 013 R9). */
private class RecordingLifecycleListener : InvoiceLifecycleListener {
    val finalized = mutableListOf<FinalizedInvoice>()
    val reverted = mutableListOf<String>()
    override suspend fun onFinalized(invoice: FinalizedInvoice) { finalized += invoice }
    override suspend fun onReverted(invoiceUid: String) { reverted += invoiceUid }
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

private class FakeProductDataService : ProductDataService {
    override suspend fun getById(uid: String): ProductSummary? = null
    override suspend fun getByIds(ids: List<String>): List<ProductSummary> = emptyList()
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

private class FakeCustomerDataService : CustomerDataService {
    override suspend fun getById(uid: String): Customer? = null
    override suspend fun listCustomers(query: String): List<CustomerListItem> = emptyList()
}
