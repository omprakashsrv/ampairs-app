package com.ampairs.tax.data.repository

import app.cash.turbine.test
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncPersistStatus
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import com.ampairs.tax.data.api.BulkSubscribeResult
import com.ampairs.tax.testutil.FakeTaxCodeDao
import com.ampairs.tax.testutil.FakeTaxConfigurationApi
import com.ampairs.tax.testutil.taxCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [TaxCodeRepository] — the one tax repository on the offline-sync contract. The local
 * write paths (favorite toggle, notes edit, unsubscribe) must flag PENDING_PUSH via [SyncStateDao]
 * and mark the row's local syncStatus so [TaxSyncDelegate] later picks it up; they must not depend on
 * the network. Subscribe / bulk-subscribe are the allowed online operations (server mints the code).
 */
class TaxCodeRepositoryTest {

    private val dao = FakeTaxCodeDao()
    private val syncStateDao = RecordingSyncStateDao()
    private val api = FakeTaxConfigurationApi()
    private val repository = TaxCodeRepository(api, dao, syncStateDao)

    @Test
    fun setFavorite_flagsPendingPush_andMarksRowPending() = runTest {
        dao.seed(taxCode(id = "T1", code = "1001"))

        val result = repository.setFavorite("T1", true)

        assertTrue(result.isSuccess)
        val row = dao.getById("T1")!!
        assertTrue(row.isFavorite)
        assertEquals("PENDING", row.syncStatus)
        assertEquals(listOf(SyncEntity.TAX), syncStateDao.pendingPushes)
    }

    @Test
    fun updateNotes_writesLocallyAndFlagsPending() = runTest {
        dao.seed(taxCode(id = "T1", code = "1001"))

        val result = repository.updateNotes("T1", "remember GST split")

        assertTrue(result.isSuccess)
        val row = dao.getById("T1")!!
        assertEquals("remember GST split", row.notes)
        assertEquals("PENDING", row.syncStatus)
        assertEquals(listOf(SyncEntity.TAX), syncStateDao.pendingPushes)
    }

    @Test
    fun updateNotes_onMissingRow_isFailure_andDoesNotFlag() = runTest {
        val result = repository.updateNotes("missing", "x")

        assertTrue(result.isFailure)
        assertTrue(syncStateDao.pendingPushes.isEmpty())
    }

    @Test
    fun unsubscribe_softDeletesLocally_marksDeletePending_andFlags() = runTest {
        dao.seed(taxCode(id = "T1", code = "1001"))

        val result = repository.unsubscribeFromTaxCode("T1")

        assertTrue(result.isSuccess)
        val row = dao.getById("T1")!!
        assertFalse(row.isActive, "unsubscribe is a soft-delete (offline-safe), not a hard delete")
        assertEquals("DELETE_PENDING", row.syncStatus)
        assertEquals(listOf(SyncEntity.TAX), syncStateDao.pendingPushes)
        // Offline-safe: no network unsubscribe in the write path.
        assertTrue(api.unsubscribedCodes.isEmpty())
    }

    @Test
    fun subscribe_persistsServerCodeLocally() = runTest {
        api.subscribeResult = { masterId -> Result.success(taxCode(id = "srv-$masterId", code = "9999")) }

        val result = repository.subscribeToTaxCode(masterTaxCodeId = "M1")

        assertTrue(result.isSuccess)
        assertEquals("9999", result.getOrThrow().code)
        assertEquals("9999", dao.getById("srv-M1")?.code)
        assertEquals(listOf("M1"), api.subscribedMasterCodes)
    }

    @Test
    fun subscribe_serverFailure_isFailure() = runTest {
        api.subscribeResult = { Result.failure(Exception("server down")) }

        val result = repository.subscribeToTaxCode(masterTaxCodeId = "M1")

        assertTrue(result.isFailure)
        assertNull(dao.getById("srv-M1"))
    }

    @Test
    fun bulkSubscribe_chunksAtHundredAndSumsSuccessCounts() = runTest {
        // 150 ids → two batches (100 + 50). Each batch reports its own success count.
        val ids = (1..150).map { "M$it" }
        api.bulkSubscribeResult = { batch ->
            Result.success(
                BulkSubscribeResult(
                    successCount = batch.size,
                    failureCount = 0,
                    subscribedCodes = batch.map { taxCode(id = "srv-$it", code = it) },
                ),
            )
        }

        val result = repository.bulkSubscribeTaxCodes(ids)

        assertTrue(result.isSuccess)
        assertEquals(150, result.getOrThrow())
        assertEquals(listOf(100, 50), api.bulkSubscribeBatches.map { it.size })
        assertEquals(150, dao.rows.value.size)
    }

    @Test
    fun bulkSubscribe_abortsRemainingBatchesOnFailure() = runTest {
        val ids = (1..150).map { "M$it" }
        api.bulkSubscribeResult = { batch ->
            if (batch.size == 100) Result.failure(Exception("first batch failed"))
            else Result.success(BulkSubscribeResult(0, 0, emptyList()))
        }

        val result = repository.bulkSubscribeTaxCodes(ids)

        assertTrue(result.isFailure)
        // Only the first (failing) batch was attempted; the second never ran.
        assertEquals(listOf(100), api.bulkSubscribeBatches.map { it.size })
    }

    @Test
    fun incrementUsageCount_updatesRow() = runTest {
        dao.seed(taxCode(id = "T1", usageCount = 0))

        repository.incrementUsageCount("T1")

        assertEquals(1, dao.getById("T1")?.usageCount)
    }

    @Test
    fun observeWorkspaceTaxCodes_emitsActiveOnly() = runTest {
        dao.seed(taxCode(id = "T1", code = "active", isActive = true, usageCount = 5))
        dao.seed(taxCode(id = "T2", code = "inactive", isActive = false))

        repository.observeWorkspaceTaxCodes().test {
            val emitted = awaitItem()
            assertEquals(listOf("active"), emitted.map { it.code })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFavorites_emitsOnlyFavorites() = runTest {
        dao.seed(taxCode(id = "T1", code = "fav", isFavorite = true))
        dao.seed(taxCode(id = "T2", code = "plain", isFavorite = false))

        repository.observeFavorites().test {
            assertEquals(listOf("fav"), awaitItem().map { it.code })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByCode_mapsAndReturnsNullWhenAbsent() = runTest {
        assertNull(repository.getByCode("nope"))
        dao.seed(taxCode(id = "T1", code = "1001"))
        assertEquals("T1", repository.getByCode("1001")?.id)
    }

    @Test
    fun searchWorkspaceTaxCodes_matchesByCode() = runTest {
        dao.seed(taxCode(id = "T1", code = "GROCERY"))
        dao.seed(taxCode(id = "T2", code = "CEMENT"))

        val matches = repository.searchWorkspaceTaxCodes("GRO", limit = 10)
        assertEquals(listOf("T1"), matches.map { it.id })
    }
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
