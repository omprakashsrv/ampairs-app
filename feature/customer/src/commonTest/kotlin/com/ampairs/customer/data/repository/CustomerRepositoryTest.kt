package com.ampairs.customer.data.repository

import app.cash.turbine.test
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerEntity
import com.ampairs.customer.domain.Customer
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncPersistStatus
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Offline-first invariants for [CustomerRepository] (see `/offline-sync`):
 *   1. Every local write persists to Room as `synced = false`.
 *   2. Every successful write flags CUSTOMER as PENDING_PUSH via SyncStateDao.
 *   3. Delete is a soft-delete (`active = false, synced = false`) — never a hard delete.
 *
 * Hand-written fakes for the DAOs (MockK is JVM-only and won't compile for the iOS/native
 * test targets). `runTest` for suspend calls; Turbine for the reactive `Flow` queries.
 */
class CustomerRepositoryTest {

    private val customerDao = FakeCustomerDao()
    private val syncStateDao = RecordingSyncStateDao()
    private val repository = CustomerRepository(customerDao, syncStateDao)

    private fun customer(
        uid: String = "C1",
        name: String = "Acme",
        city: String? = "Pune",
        active: Boolean = true,
    ) = Customer(uid = uid, name = name, city = city, active = active)

    // -- create -------------------------------------------------------------

    @Test
    fun `createCustomer persists locally as unsynced and flags pending push`() = runTest {
        val result = repository.createCustomer(customer())

        assertTrue(result.isSuccess)
        val stored = customerDao.getCustomerById("C1")
        assertEquals("Acme", stored?.name)
        assertFalse(stored!!.synced, "a freshly written row must be unsynced so the push picks it up")
        assertTrue(stored.active)
        assertEquals(listOf(SyncEntity.CUSTOMER), syncStateDao.pendingPushes)
    }

    @Test
    fun `createCustomer requires a non-blank uid`() = runTest {
        // require(...) failure surfaces as IllegalArgumentException — the row must not be written.
        assertFails { repository.createCustomer(customer(uid = "")) }
        assertNull(customerDao.getCustomerById(""))
        assertTrue(syncStateDao.pendingPushes.isEmpty(), "a rejected create must not flag a push")
    }

    // -- update -------------------------------------------------------------

    @Test
    fun `updateCustomer rewrites the row as unsynced and re-flags pending push`() = runTest {
        repository.createCustomer(customer(name = "Acme"))
        syncStateDao.pendingPushes.clear()

        val result = repository.updateCustomer(customer(name = "Acme Corp"))

        assertTrue(result.isSuccess)
        assertEquals("Acme Corp", customerDao.getCustomerById("C1")?.name)
        assertFalse(customerDao.getCustomerById("C1")!!.synced)
        assertEquals(listOf(SyncEntity.CUSTOMER), syncStateDao.pendingPushes)
    }

    // -- delete -------------------------------------------------------------

    @Test
    fun `deleteCustomer soft-deletes an existing row and flags pending push`() = runTest {
        repository.createCustomer(customer())
        syncStateDao.pendingPushes.clear()

        val result = repository.deleteCustomer("C1")

        assertTrue(result.isSuccess)
        val row = customerDao.getCustomerById("C1")
        assertFalse(row!!.active, "delete must set active = false (soft-delete), not remove the row")
        assertFalse(row.synced, "delete must set synced = false or the push never sends it")
        assertEquals(listOf(SyncEntity.CUSTOMER), syncStateDao.pendingPushes)
    }

    @Test
    fun `deleteCustomer on a missing row is a no-op that does not flag a push`() = runTest {
        val result = repository.deleteCustomer("does-not-exist")

        assertTrue(result.isSuccess)
        assertTrue(syncStateDao.pendingPushes.isEmpty())
    }

    // -- reactive queries ---------------------------------------------------

    @Test
    fun `observeCustomers emits only active customers mapped to list items`() = runTest {
        repository.createCustomer(customer(uid = "C1", name = "Acme"))
        repository.createCustomer(customer(uid = "C2", name = "Beta"))

        repository.observeCustomers().test {
            val emitted = awaitItem()
            assertEquals(listOf("Acme", "Beta"), emitted.map { it.name })
            // toListItem maps gst -> gstin and id -> uid
            assertEquals(listOf("C1", "C2"), emitted.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCustomers hides soft-deleted customers`() = runTest {
        repository.createCustomer(customer(uid = "C1", name = "Acme"))
        repository.deleteCustomer("C1")

        repository.observeCustomers().test {
            assertTrue(awaitItem().isEmpty(), "soft-deleted (active = false) rows must not be observed")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchCustomers filters by name substring`() = runTest {
        repository.createCustomer(customer(uid = "C1", name = "Acme"))
        repository.createCustomer(customer(uid = "C2", name = "Beta"))

        repository.searchCustomers("Bet").test {
            assertEquals(listOf("Beta"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCustomer emits the mapped domain model and null when absent`() = runTest {
        repository.createCustomer(customer(uid = "C1", name = "Acme"))

        repository.observeCustomer("C1").test {
            assertEquals("Acme", awaitItem()?.name)
            cancelAndIgnoreRemainingEvents()
        }
        repository.observeCustomer("missing").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -- getters / mappers --------------------------------------------------

    @Test
    fun `getCustomer maps entity to domain and returns null when absent`() = runTest {
        assertNull(repository.getCustomer("C1"))
        repository.createCustomer(customer(uid = "C1", name = "Acme", city = "Mumbai"))

        val found = repository.getCustomer("C1")
        assertEquals("Acme", found?.name)
        assertEquals("Mumbai", found?.city)
        // getById delegates to getCustomer
        assertEquals("Acme", repository.getById("C1")?.name)
    }

    @Test
    fun `getCustomerCount counts only active rows`() = runTest {
        repository.createCustomer(customer(uid = "C1"))
        repository.createCustomer(customer(uid = "C2"))
        repository.deleteCustomer("C2")

        assertEquals(1, repository.getCustomerCount())
    }

    @Test
    fun `listCustomers uses search when a query is given and full list otherwise`() = runTest {
        repository.createCustomer(customer(uid = "C1", name = "Acme"))
        repository.createCustomer(customer(uid = "C2", name = "Beta"))

        assertEquals(listOf("Acme", "Beta"), repository.listCustomers("").map { it.name })
        assertEquals(listOf("Acme"), repository.listCustomers("Acm").map { it.name })
    }

    @Test
    fun `observeUnsyncedCount reflects pending writes`() = runTest {
        repository.observeUnsyncedCount().test {
            assertEquals(0, awaitItem())
            repository.createCustomer(customer(uid = "C1"))
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearCache wipes all locally stored customers`() = runTest {
        repository.createCustomer(customer(uid = "C1"))
        repository.createCustomer(customer(uid = "C2"))

        repository.clearCache()

        assertEquals(0, repository.getCustomerCount())
        assertNull(customerDao.getCustomerById("C1"))
    }

    @Test
    fun `roundtrip preserves city and attributes through entity mapping`() = runTest {
        val withAttrs = Customer(
            uid = "C9",
            name = "Gamma",
            city = "Delhi",
            attributes = mapOf("tier" to "gold"),
        )
        repository.createCustomer(withAttrs)

        val loaded = repository.getCustomer("C9")
        assertEquals("Delhi", loaded?.city)
        assertEquals(mapOf("tier" to "gold"), loaded?.attributes)
    }
}

/** In-memory [CustomerDao] backed by a map; reactive queries derive from a single state flow. */
private class FakeCustomerDao : CustomerDao {
    private val rows = MutableStateFlow<Map<String, CustomerEntity>>(emptyMap())

    override fun getAllCustomers(): Flow<List<CustomerEntity>> =
        rows.map { m -> m.values.filter { it.active }.sortedBy { it.name } }

    override suspend fun getCustomerById(customerId: String): CustomerEntity? = rows.value[customerId]

    override fun observeCustomerById(customerId: String): Flow<CustomerEntity?> =
        rows.map { it[customerId] }

    override fun searchCustomers(searchQuery: String): Flow<List<CustomerEntity>> =
        rows.map { m -> m.values.filter { it.active && it.name.contains(searchQuery) }.sortedBy { it.name } }

    override fun filterCustomers(
        searchQuery: String,
        states: List<String>,
        hasStates: Int,
        types: List<String>,
        hasTypes: Int,
        groups: List<String>,
        hasGroups: Int,
    ): Flow<List<CustomerEntity>> = rows.map { m ->
        m.values.filter { e ->
            e.active &&
                (hasStates == 0 || e.state in states) &&
                (hasTypes == 0 || e.customer_type in types) &&
                (hasGroups == 0 || e.customer_group in groups) &&
                (searchQuery.isEmpty() || e.name.contains(searchQuery))
        }.sortedBy { it.name }
    }

    override suspend fun getDistinctStates(): List<String> =
        rows.value.values.filter { it.active }.mapNotNull { it.state }.filter { it.isNotEmpty() }.distinct().sorted()

    override suspend fun getDistinctCustomerTypes(): List<String> =
        rows.value.values.filter { it.active }.mapNotNull { it.customer_type }.filter { it.isNotEmpty() }.distinct().sorted()

    override suspend fun getDistinctCustomerGroups(): List<String> =
        rows.value.values.filter { it.active }.mapNotNull { it.customer_group }.filter { it.isNotEmpty() }.distinct().sorted()

    override suspend fun getCustomersByTallyRefIds(refs: List<String>): List<CustomerEntity> =
        rows.value.values.filter { it.ref_id in refs }

    override suspend fun insertCustomer(customer: CustomerEntity) {
        rows.value = rows.value + (customer.id to customer)
    }

    override suspend fun insertCustomers(customers: List<CustomerEntity>) {
        rows.value = rows.value + customers.associateBy { it.id }
    }

    override suspend fun updateCustomer(customer: CustomerEntity) {
        rows.value = rows.value + (customer.id to customer)
    }

    override suspend fun deleteCustomer(customerId: String) {
        rows.value = rows.value - customerId
    }

    override suspend fun getCustomerCount(): Int = rows.value.values.count { it.active }

    override suspend fun getUnsyncedCustomers(): List<CustomerEntity> = rows.value.values.filter { !it.synced }

    override fun observeUnsyncedCount(): Flow<Int> = rows.map { m -> m.values.count { !it.synced } }

    override suspend fun clearWorkspaceCustomers() {
        rows.value = emptyMap()
    }

    override suspend fun nullifyInvalidPhones() {}

    override suspend fun nullifyInvalidPincodes() {}

    override suspend fun getUniqueCities(): List<String> =
        rows.value.values.filter { it.active }.mapNotNull { it.city }.filter { it.isNotEmpty() }.distinct()

    override suspend fun getUniquePincodes(): List<String> =
        rows.value.values.filter { it.active }.mapNotNull { it.pincode }.filter { it.isNotEmpty() }.distinct()
}

/** [SyncStateDao] that records markPendingPush calls so tests can assert the sync flag. */
private class RecordingSyncStateDao : SyncStateDao {
    val pendingPushes = mutableListOf<SyncEntity>()

    override fun observeAll(): Flow<List<SyncStateEntity>> = flowOf(emptyList())
    override fun observe(entity: SyncEntity): Flow<SyncStateEntity?> = flowOf(null)
    override suspend fun getAll(): List<SyncStateEntity> = emptyList()
    override suspend fun getPending(): List<SyncStateEntity> = emptyList()
    override suspend fun upsert(state: SyncStateEntity) {}
    override suspend fun upsertStatus(
        entity: SyncEntity,
        status: SyncPersistStatus,
        lastSyncedAt: Long?,
        pendingCount: Int,
        errorMessage: String?,
        now: Long,
    ) {}
    override suspend fun getLastSyncedAtIso(entity: SyncEntity): String? = null
    override suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String) {}
    override suspend fun markPendingPush(entity: SyncEntity, now: Long) {
        pendingPushes += entity
    }
    override suspend fun deleteAll() {}
}
