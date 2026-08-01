package com.ampairs.customer.data.repository

import app.cash.turbine.test
import com.ampairs.common.model.Error
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.CustomerTypeEntity
import com.ampairs.customer.domain.CustomerType
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Offline-first invariants for [CustomerTypeRepository]. Mirrors [CustomerGroupRepositoryTest];
 * the type repo does NOT derive a code from the name (unlike the group repo).
 */
class CustomerTypeRepositoryTest {

    private val dao = FakeCustomerTypeDao()
    private val syncStateDao = RecordingTypeSyncStateDao()
    private val api = FakeCustomerTypeApi()
    private val repository = CustomerTypeRepository(api, dao, syncStateDao)

    private fun type(uid: String = "T1", name: String = "Regular") =
        CustomerType(uid = uid, name = name)

    // -- create -------------------------------------------------------------

    @Test
    fun `createCustomerType persists locally as unsynced and flags pending push`() = runTest {
        val result = repository.createCustomerType(type())

        assertTrue(result.isSuccess)
        val stored = dao.getCustomerTypeById("T1")
        assertEquals("Regular", stored?.name)
        assertFalse(stored!!.synced)
        assertEquals(listOf(SyncEntity.CUSTOMER_TYPE), syncStateDao.pendingPushes)
    }

    @Test
    fun `createCustomerType generates a uid when blank`() = runTest {
        val result = repository.createCustomerType(type(uid = "", name = "VIP"))

        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertTrue(created.uid.isNotBlank())
        assertTrue(created.uid.startsWith("CT"), "uid uses the CUSTOMER_TYPE prefix")
        assertEquals("VIP", dao.getCustomerTypeById(created.uid)?.name)
    }

    // -- update -------------------------------------------------------------

    @Test
    fun `updateCustomerType rewrites the row as unsynced and re-flags pending push`() = runTest {
        repository.createCustomerType(type(name = "Regular"))
        syncStateDao.pendingPushes.clear()

        val result = repository.updateCustomerType(type(name = "Premium"))

        assertTrue(result.isSuccess)
        assertEquals("Premium", dao.getCustomerTypeById("T1")?.name)
        assertFalse(dao.getCustomerTypeById("T1")!!.synced)
        assertEquals(listOf(SyncEntity.CUSTOMER_TYPE), syncStateDao.pendingPushes)
    }

    // -- delete -------------------------------------------------------------

    @Test
    fun `deleteCustomerType soft-deletes an existing row and flags pending push`() = runTest {
        repository.createCustomerType(type())
        syncStateDao.pendingPushes.clear()

        val result = repository.deleteCustomerType("T1")

        assertTrue(result.isSuccess)
        val row = dao.getCustomerTypeById("T1")
        assertFalse(row!!.active, "delete must set active = false (soft-delete)")
        assertFalse(row.synced, "delete must set synced = false or the push never sends it")
        assertEquals(listOf(SyncEntity.CUSTOMER_TYPE), syncStateDao.pendingPushes)
    }

    @Test
    fun `deleteCustomerType on a missing row falls back to hard delete without flagging a push`() = runTest {
        val result = repository.deleteCustomerType("missing")

        assertTrue(result.isSuccess)
        assertTrue(syncStateDao.pendingPushes.isEmpty())
    }

    // -- import -------------------------------------------------------------

    @Test
    fun `importCustomerType persists locally and flags pending push`() = runTest {
        val result = repository.importCustomerType(type(uid = "I1", name = "Imported"))

        assertTrue(result.isSuccess)
        assertEquals("Imported", dao.getCustomerTypeById("I1")?.name)
        assertEquals(listOf(SyncEntity.CUSTOMER_TYPE), syncStateDao.pendingPushes)
    }

    @Test
    fun `bulkImportCustomerTypes imports every type`() = runTest {
        val result = repository.bulkImportCustomerTypes(
            listOf(type(uid = "B1", name = "One"), type(uid = "B2", name = "Two")),
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals("One", dao.getCustomerTypeById("B1")?.name)
        assertEquals("Two", dao.getCustomerTypeById("B2")?.name)
    }

    // -- reactive queries / getters -----------------------------------------

    @Test
    fun `observeCustomerTypes emits active types mapped to domain`() = runTest {
        repository.createCustomerType(type(uid = "T1", name = "Regular"))
        repository.createCustomerType(type(uid = "T2", name = "Premium"))

        repository.observeCustomerTypes().test {
            assertEquals(listOf("Premium", "Regular"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchCustomerTypes returns full list on blank query and filters otherwise`() = runTest {
        repository.createCustomerType(type(uid = "T1", name = "Regular"))
        repository.createCustomerType(type(uid = "T2", name = "Premium"))

        repository.searchCustomerTypes("").test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
        repository.searchCustomerTypes("Prem").test {
            assertEquals(listOf("Premium"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCustomerTypeById and ByName map to domain and return null when absent`() = runTest {
        assertNull(repository.getCustomerTypeById("T1"))
        repository.createCustomerType(type(uid = "T1", name = "Regular"))

        assertEquals("Regular", repository.getCustomerTypeById("T1")?.name)
        assertEquals("T1", repository.getCustomerTypeByName("Regular")?.uid)
        assertNull(repository.getCustomerTypeByName("Nope"))
    }

    // -- available-for-import (the API path) --------------------------------

    @Test
    fun `getAvailableCustomerTypesForImport filters out types already present locally`() = runTest {
        repository.createCustomerType(type(uid = "T1", name = "Regular"))
        api.availableResponse = Response(
            data = listOf(type(uid = "M1", name = "Regular"), type(uid = "M2", name = "Dealer")),
        )

        val result = repository.getAvailableCustomerTypesForImport()

        assertTrue(result.isSuccess)
        assertEquals(listOf("Dealer"), result.getOrThrow().map { it.name })
    }

    @Test
    fun `getAvailableCustomerTypesForImport surfaces an API error as failure`() = runTest {
        api.availableResponse = Response(error = Error(message = "boom"))

        val result = repository.getAvailableCustomerTypesForImport()

        assertTrue(result.isFailure)
    }
}

/** In-memory [CustomerTypeDao]. `deleteCustomerType` only flips active (matches the @Query). */
private class FakeCustomerTypeDao : CustomerTypeDao {
    private val rows = MutableStateFlow<Map<String, CustomerTypeEntity>>(emptyMap())

    override fun getAllCustomerTypes(): Flow<List<CustomerTypeEntity>> =
        rows.map { m -> m.values.filter { it.active }.sortedBy { it.name } }

    override fun searchCustomerTypes(query: String): Flow<List<CustomerTypeEntity>> =
        rows.map { m -> m.values.filter { it.active && it.name.contains(query) }.sortedBy { it.name } }

    override suspend fun getCustomerTypeById(id: String): CustomerTypeEntity? = rows.value[id]

    override suspend fun getCustomerTypeByName(name: String): CustomerTypeEntity? =
        rows.value.values.firstOrNull { it.name == name && it.active }

    override suspend fun getUnsyncedCustomerTypes(): List<CustomerTypeEntity> =
        rows.value.values.filter { !it.synced }

    override suspend fun insertCustomerType(customerType: CustomerTypeEntity) {
        rows.value = rows.value + (customerType.id to customerType)
    }

    override suspend fun insertCustomerTypes(customerTypes: List<CustomerTypeEntity>) {
        rows.value = rows.value + customerTypes.associateBy { it.id }
    }

    override suspend fun updateCustomerType(customerType: CustomerTypeEntity) {
        rows.value = rows.value + (customerType.id to customerType)
    }

    override suspend fun deleteCustomerType(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = false)) }
    }

    override suspend fun hardDeleteCustomerType(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun clearAll() {
        rows.value = emptyMap()
    }

    override suspend fun getActiveCustomerTypesCount(): Int = rows.value.values.count { it.active }

    override fun observeUnsyncedCount(): Flow<Int> = rows.map { m -> m.values.count { !it.synced } }
}

/** Configurable [CustomerTypeApi] — only the available-for-import path is exercised. */
private class FakeCustomerTypeApi : CustomerTypeApi {
    var availableResponse: Response<List<CustomerType>> = Response(data = emptyList())

    override suspend fun getCustomerTypes(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String,
    ): Response<PageResponse<CustomerType>> = Response()

    override suspend fun getAvailableCustomerTypesForImport(): Response<List<CustomerType>> =
        availableResponse

    override suspend fun getCustomerTypeById(id: String): Response<CustomerType> = Response()

    override suspend fun bulkUpsertTypes(types: List<CustomerType>): Result<List<CustomerType>> =
        Result.success(types)

    override suspend fun searchCustomerTypes(
        query: String,
        page: Int,
        size: Int,
    ): Response<PageResponse<CustomerType>> = Response()
}

private class RecordingTypeSyncStateDao : SyncStateDao {
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
