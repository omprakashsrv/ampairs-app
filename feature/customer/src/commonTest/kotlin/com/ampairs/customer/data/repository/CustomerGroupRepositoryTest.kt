package com.ampairs.customer.data.repository

import app.cash.turbine.test
import com.ampairs.common.model.Error
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.CustomerGroupEntity
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Offline-first invariants for [CustomerGroupRepository]. Same pattern as [CustomerRepositoryTest]:
 * hand-written fakes (no MockK), `runTest`, Turbine. The group repo also injects a [CustomerGroupApi]
 * for the non-sync "available for import" path — that path is faked and tested separately.
 */
class CustomerGroupRepositoryTest {

    private val dao = FakeCustomerGroupDao()
    private val syncStateDao = RecordingGroupSyncStateDao()
    private val api = FakeCustomerGroupApi()
    private val repository = CustomerGroupRepository(api, dao, syncStateDao)

    private fun group(uid: String = "G1", name: String = "Retail", code: String? = "RET") =
        CustomerGroup(uid = uid, name = name, groupCode = code)

    // -- create -------------------------------------------------------------

    @Test
    fun `createCustomerGroup persists locally as unsynced and flags pending push`() = runTest {
        val result = repository.createCustomerGroup(group())

        assertTrue(result.isSuccess)
        val stored = dao.getCustomerGroupById("G1")
        assertEquals("Retail", stored?.name)
        assertFalse(stored!!.synced)
        assertEquals(listOf(SyncEntity.CUSTOMER_GROUP), syncStateDao.pendingPushes)
    }

    @Test
    fun `createCustomerGroup generates a uid when blank`() = runTest {
        val result = repository.createCustomerGroup(group(uid = "", name = "New Group"))

        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertTrue(created.uid.isNotBlank(), "a blank uid must be replaced by a generated one")
        assertTrue(created.uid.startsWith("CG"), "uid uses the CUSTOMER_GROUP prefix")
        assertNull(dao.getCustomerGroupById(""))
        assertEquals("New Group", dao.getCustomerGroupById(created.uid)?.name)
    }

    @Test
    fun `createCustomerGroup derives a group code from the name when none is given`() = runTest {
        val result = repository.createCustomerGroup(group(uid = "G2", name = "Wholesale Buyers", code = null))

        assertTrue(result.isSuccess)
        // name -> letters/digits/space kept, trimmed, spaces -> '_', uppercased, take(20)
        assertEquals("WHOLESALE_BUYERS", result.getOrThrow().groupCode)
    }

    @Test
    fun `createCustomerGroup keeps an explicit non-blank group code`() = runTest {
        val result = repository.createCustomerGroup(group(uid = "G3", name = "Retail", code = "MYCODE"))

        assertEquals("MYCODE", result.getOrThrow().groupCode)
    }

    // -- update -------------------------------------------------------------

    @Test
    fun `updateCustomerGroup rewrites the row as unsynced and re-flags pending push`() = runTest {
        repository.createCustomerGroup(group(name = "Retail"))
        syncStateDao.pendingPushes.clear()

        val result = repository.updateCustomerGroup(group(name = "Retailers"))

        assertTrue(result.isSuccess)
        assertEquals("Retailers", dao.getCustomerGroupById("G1")?.name)
        assertFalse(dao.getCustomerGroupById("G1")!!.synced)
        assertEquals(listOf(SyncEntity.CUSTOMER_GROUP), syncStateDao.pendingPushes)
    }

    // -- delete -------------------------------------------------------------

    @Test
    fun `deleteCustomerGroup soft-deletes an existing row and flags pending push`() = runTest {
        repository.createCustomerGroup(group())
        syncStateDao.pendingPushes.clear()

        val result = repository.deleteCustomerGroup("G1")

        assertTrue(result.isSuccess)
        val row = dao.getCustomerGroupById("G1")
        assertFalse(row!!.active, "delete must set active = false (soft-delete)")
        assertFalse(row.synced, "delete must set synced = false or the push never sends it")
        assertEquals(listOf(SyncEntity.CUSTOMER_GROUP), syncStateDao.pendingPushes)
    }

    @Test
    fun `deleteCustomerGroup on a missing row falls back to hard delete without flagging a push`() = runTest {
        // The repo path for a missing row calls dao.deleteCustomerGroup(id) (a no-op here) and
        // does NOT call markPending — assert no push was flagged.
        val result = repository.deleteCustomerGroup("missing")

        assertTrue(result.isSuccess)
        assertTrue(syncStateDao.pendingPushes.isEmpty())
    }

    // -- import (uses the create path, so it flags a push) -------------------

    @Test
    fun `importCustomerGroup persists locally and flags pending push`() = runTest {
        val result = repository.importCustomerGroup(group(uid = "I1", name = "Imported"))

        assertTrue(result.isSuccess)
        assertEquals("Imported", dao.getCustomerGroupById("I1")?.name)
        assertEquals(listOf(SyncEntity.CUSTOMER_GROUP), syncStateDao.pendingPushes)
    }

    @Test
    fun `bulkImportCustomerGroups imports every group`() = runTest {
        val result = repository.bulkImportCustomerGroups(
            listOf(group(uid = "B1", name = "One"), group(uid = "B2", name = "Two")),
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals("One", dao.getCustomerGroupById("B1")?.name)
        assertEquals("Two", dao.getCustomerGroupById("B2")?.name)
    }

    // -- reactive queries / getters -----------------------------------------

    @Test
    fun `observeCustomerGroups emits active groups mapped to domain`() = runTest {
        repository.createCustomerGroup(group(uid = "G1", name = "Retail"))
        repository.createCustomerGroup(group(uid = "G2", name = "Wholesale"))

        repository.observeCustomerGroups().test {
            assertEquals(listOf("Retail", "Wholesale"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchCustomerGroups returns full list on blank query and filters otherwise`() = runTest {
        repository.createCustomerGroup(group(uid = "G1", name = "Retail"))
        repository.createCustomerGroup(group(uid = "G2", name = "Wholesale"))

        repository.searchCustomerGroups("").test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
        repository.searchCustomerGroups("Whole").test {
            assertEquals(listOf("Wholesale"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCustomerGroupById and ByName map to domain and return null when absent`() = runTest {
        assertNull(repository.getCustomerGroupById("G1"))
        repository.createCustomerGroup(group(uid = "G1", name = "Retail"))

        assertEquals("Retail", repository.getCustomerGroupById("G1")?.name)
        assertEquals("G1", repository.getCustomerGroupByName("Retail")?.uid)
        assertNull(repository.getCustomerGroupByName("Nope"))
    }

    // -- available-for-import (the API path) --------------------------------

    @Test
    fun `getAvailableCustomerGroupsForImport filters out groups already present locally`() = runTest {
        repository.createCustomerGroup(group(uid = "G1", name = "Retail"))
        api.availableResponse = Response(
            data = listOf(group(uid = "M1", name = "Retail"), group(uid = "M2", name = "Distributor")),
        )

        val result = repository.getAvailableCustomerGroupsForImport()

        assertTrue(result.isSuccess)
        // "Retail" already exists locally -> excluded; only "Distributor" remains.
        assertEquals(listOf("Distributor"), result.getOrThrow().map { it.name })
    }

    @Test
    fun `getAvailableCustomerGroupsForImport surfaces an API error as failure`() = runTest {
        api.availableResponse = Response(error = Error(message = "boom"))

        val result = repository.getAvailableCustomerGroupsForImport()

        assertTrue(result.isFailure)
    }
}

/** In-memory [CustomerGroupDao]. Note `deleteCustomerGroup` only flips active (matches the @Query). */
private class FakeCustomerGroupDao : CustomerGroupDao {
    private val rows = MutableStateFlow<Map<String, CustomerGroupEntity>>(emptyMap())

    override fun getAllCustomerGroups(): Flow<List<CustomerGroupEntity>> =
        rows.map { m -> m.values.filter { it.active }.sortedBy { it.name } }

    override fun searchCustomerGroups(query: String): Flow<List<CustomerGroupEntity>> =
        rows.map { m -> m.values.filter { it.active && it.name.contains(query) }.sortedBy { it.name } }

    override suspend fun getCustomerGroupById(id: String): CustomerGroupEntity? = rows.value[id]

    override suspend fun getCustomerGroupByName(name: String): CustomerGroupEntity? =
        rows.value.values.firstOrNull { it.name == name && it.active }

    override suspend fun getUnsyncedCustomerGroups(): List<CustomerGroupEntity> =
        rows.value.values.filter { !it.synced }

    override suspend fun getCustomerGroupsByTallyRefIds(refs: List<String>): List<CustomerGroupEntity> =
        rows.value.values.filter { it.ref_id in refs }

    override suspend fun insertCustomerGroup(customerGroup: CustomerGroupEntity) {
        rows.value = rows.value + (customerGroup.id to customerGroup)
    }

    override suspend fun insertCustomerGroups(customerGroups: List<CustomerGroupEntity>) {
        rows.value = rows.value + customerGroups.associateBy { it.id }
    }

    override suspend fun updateCustomerGroup(customerGroup: CustomerGroupEntity) {
        rows.value = rows.value + (customerGroup.id to customerGroup)
    }

    override suspend fun deleteCustomerGroup(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = false)) }
    }

    override suspend fun hardDeleteCustomerGroup(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun clearAll() {
        rows.value = emptyMap()
    }

    override suspend fun getActiveCustomerGroupsCount(): Int = rows.value.values.count { it.active }

    override fun observeUnsyncedCount(): Flow<Int> = rows.map { m -> m.values.count { !it.synced } }
}

/** Configurable [CustomerGroupApi] — only the available-for-import path is exercised. */
private class FakeCustomerGroupApi : CustomerGroupApi {
    var availableResponse: Response<List<CustomerGroup>> = Response(data = emptyList())

    override suspend fun getCustomerGroups(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String,
    ): Response<PageResponse<CustomerGroup>> = Response()

    override suspend fun getAvailableCustomerGroupsForImport(): Response<List<CustomerGroup>> =
        availableResponse

    override suspend fun getCustomerGroupById(id: String): Response<CustomerGroup> = Response()

    override suspend fun bulkUpsertGroups(groups: List<CustomerGroup>): Result<List<CustomerGroup>> =
        Result.success(groups)

    override suspend fun searchCustomerGroups(
        query: String,
        page: Int,
        size: Int,
    ): Response<PageResponse<CustomerGroup>> = Response()
}

private class RecordingGroupSyncStateDao : SyncStateDao {
    val pendingPushes = mutableListOf<SyncEntity>()

    override fun observeAll(): Flow<List<SyncStateEntity>> = flowOf(emptyList())
    override fun observe(entity: SyncEntity): Flow<SyncStateEntity?> = flowOf(null)
    override suspend fun getAll(): List<SyncStateEntity> = emptyList()
    override suspend fun getPending(): List<SyncStateEntity> = emptyList()
    override suspend fun upsert(state: SyncStateEntity) {}
    override suspend fun getLastSyncedAtIso(entity: SyncEntity): String? = null
    override suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String) {}
    override suspend fun markPendingPush(entity: SyncEntity, now: Long) {
        pendingPushes += entity
    }
    override suspend fun deleteAll() {}
}
