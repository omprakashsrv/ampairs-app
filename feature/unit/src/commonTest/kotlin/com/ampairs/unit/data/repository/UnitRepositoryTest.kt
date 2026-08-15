package com.ampairs.unit.data.repository

import app.cash.turbine.test
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncPersistStatus
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.db.entity.UnitConversionEntity
import com.ampairs.unit.data.db.entity.UnitEntity
import com.ampairs.unit.domain.model.Unit as UnitModel
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
 * Exemplar test for the offline-first repository layer (the highest-risk, least-covered
 * tier of the app). It pins the central invariants from `/offline-sync`:
 *
 *   1. Every local write persists to Room as `synced = false`.
 *   2. Every successful write flags the entity PENDING_PUSH via SyncStateDao — this is what
 *      makes the bulk push automatic. The repository must NEVER touch the API.
 *   3. Delete is a soft-delete (`active = false, synced = false`), not a hard delete, or the
 *      push (which reads `synced = 0`) would never send the deletion.
 *
 * Pattern to copy for other repositories: hand-written fakes for the DAOs (MockK is JVM-only
 * and won't compile for the iOS/native test targets), `runTest` for suspend calls, and Turbine
 * for the reactive `Flow` queries. No real Room / network involved.
 */
class UnitRepositoryTest {

    private val unitDao = FakeUnitDao()
    private val syncStateDao = RecordingSyncStateDao()
    private val repository = UnitRepository(unitDao, FakeUnitConversionDao(), syncStateDao)

    private fun unit(uid: String = "U1", name: String = "Kilogram", short: String = "kg") =
        UnitModel(uid = uid, name = name, shortName = short, decimalPlaces = 3)

    @Test
    fun `createUnit persists locally as unsynced and flags pending push`() = runTest {
        val result = repository.createUnit(unit())

        assertTrue(result.isSuccess)
        val stored = unitDao.getUnitById("U1")
        assertEquals("Kilogram", stored?.name)
        assertFalse(stored!!.synced, "a freshly written row must be unsynced so the push picks it up")
        assertEquals(listOf(SyncEntity.UNIT), syncStateDao.pendingPushes)
    }

    @Test
    fun `updateUnit rewrites the row as unsynced and re-flags pending push`() = runTest {
        repository.createUnit(unit(name = "Kilogram"))
        syncStateDao.pendingPushes.clear()

        val result = repository.updateUnit(unit(name = "Kilo"))

        assertTrue(result.isSuccess)
        assertEquals("Kilo", unitDao.getUnitById("U1")?.name)
        assertFalse(unitDao.getUnitById("U1")!!.synced)
        assertEquals(listOf(SyncEntity.UNIT), syncStateDao.pendingPushes)
    }

    @Test
    fun `deleteUnit soft-deletes an existing row and flags pending push`() = runTest {
        repository.createUnit(unit())
        syncStateDao.pendingPushes.clear()

        val result = repository.deleteUnit("U1")

        assertTrue(result.isSuccess)
        val row = unitDao.getUnitById("U1")
        assertFalse(row!!.active, "delete must set active = false (soft-delete), not remove the row")
        assertFalse(row.synced, "delete must set synced = false or the push never sends it")
        assertEquals(listOf(SyncEntity.UNIT), syncStateDao.pendingPushes)
    }

    @Test
    fun `deleteUnit on a missing row is a no-op that does not flag a push`() = runTest {
        val result = repository.deleteUnit("does-not-exist")

        assertTrue(result.isSuccess)
        assertTrue(syncStateDao.pendingPushes.isEmpty())
    }

    @Test
    fun `observeUnits emits only active units, newest write reflected`() = runTest {
        repository.createUnit(unit(uid = "U1", name = "Kilogram"))

        repository.observeUnits().test {
            val emitted = awaitItem()
            assertEquals(listOf("Kilogram"), emitted.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUnitById maps entity to domain and returns null when absent`() = runTest {
        assertNull(repository.getUnitById("U1"))
        repository.createUnit(unit(uid = "U1", name = "Gram", short = "g"))

        val found = repository.getUnitById("U1")
        assertEquals("Gram", found?.name)
        assertEquals("g", found?.shortName)
    }
}

/** In-memory [UnitDao] backed by a map; reactive queries derive from a single state flow. */
private class FakeUnitDao : UnitDao {
    private val rows = MutableStateFlow<Map<String, UnitEntity>>(emptyMap())

    override fun getAllUnits(): Flow<List<UnitEntity>> =
        rows.map { m -> m.values.filter { it.active }.sortedBy { it.name } }

    override fun searchUnits(query: String): Flow<List<UnitEntity>> =
        rows.map { m -> m.values.filter { it.active && it.name.contains(query) }.sortedBy { it.name } }

    override suspend fun getUnitById(id: String): UnitEntity? = rows.value[id]

    override suspend fun getUnitByName(name: String): UnitEntity? =
        rows.value.values.firstOrNull { it.name == name && it.active }

    override suspend fun getUnsyncedUnits(): List<UnitEntity> = rows.value.values.filter { !it.synced }

    override suspend fun getUnitsByTallyRefIds(refs: List<String>): List<UnitEntity> =
        rows.value.values.filter { it.refId in refs }

    override suspend fun insertUnit(unit: UnitEntity) {
        rows.value = rows.value + (unit.id to unit)
    }

    override suspend fun insertUnits(units: List<UnitEntity>) {
        rows.value = rows.value + units.associateBy { it.id }
    }

    override suspend fun updateUnit(unit: UnitEntity) {
        rows.value = rows.value + (unit.id to unit)
    }

    override suspend fun setTallyRef(id: String, refId: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(refId = refId, synced = false)) }
    }

    override suspend fun deleteUnit(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = false)) }
    }

    override suspend fun hardDeleteUnit(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun clearAll() {
        rows.value = emptyMap()
    }

    override suspend fun getActiveUnitsCount(): Int = rows.value.values.count { it.active }
}

/** Minimal [UnitConversionDao] — UnitRepository only reads conversions for product-scoped lookups. */
private class FakeUnitConversionDao : UnitConversionDao {
    override fun getAllUnitConversions(): Flow<List<UnitConversionEntity>> = flowOf(emptyList())
    override fun getUnitConversionsByProductId(productId: String): Flow<List<UnitConversionEntity>> =
        flowOf(emptyList())
    override suspend fun getUnitConversion(
        productId: String,
        baseUnitId: String,
        derivedUnitId: String,
    ): UnitConversionEntity? = null
    override suspend fun getUnitConversionById(id: String): UnitConversionEntity? = null
    override suspend fun getUnsyncedUnitConversions(): List<UnitConversionEntity> = emptyList()
    override suspend fun getActiveByProductIds(productIds: List<String>): List<UnitConversionEntity> = emptyList()
    override suspend fun productIdsWithUnsyncedConversions(): List<String> = emptyList()
    override suspend fun unsyncedCountForProduct(productId: String): Int = 0
    override suspend fun markSyncedByProduct(productId: String) {}
    override suspend fun getUnitConversionsByBaseUnit(baseUnitId: String): List<UnitConversionEntity> = emptyList()
    override suspend fun getUnitConversionsByDerivedUnit(derivedUnitId: String): List<UnitConversionEntity> = emptyList()
    override suspend fun insertUnitConversion(unitConversion: UnitConversionEntity) {}
    override suspend fun insertUnitConversions(unitConversions: List<UnitConversionEntity>) {}
    override suspend fun updateUnitConversion(unitConversion: UnitConversionEntity) {}
    override suspend fun deleteUnitConversion(id: String) {}
    override suspend fun deleteByProductId(productId: String) {}
    override suspend fun clearAll() {}
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
