package com.ampairs.unit.data.repository

import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.repository.UnitLookup
import dev.zacsweers.metro.Inject
import com.ampairs.unit.data.db.entity.toEntity
import com.ampairs.unit.data.db.entity.toUnit
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.util.UnitLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for units. The [UnitApi] is owned by the sync layer
 * ([com.ampairs.unit.sync.UnitSyncDelegate]); writes here persist to Room as unsynced and mark
 * UNIT as PENDING_PUSH so CentralSyncService runs the automatic bulk push.
 */
@OptIn(ExperimentalTime::class)
@Inject
class UnitRepository(
    private val unitDao: UnitDao,
    private val syncStateDao: SyncStateDao,
) : UnitLookup {

    fun observeUnits(): Flow<List<Unit>> =
        unitDao.getAllUnits().map { entities -> entities.map { it.toUnit() } }

    override suspend fun getActiveUnits(): List<Unit> =
        unitDao.getAllUnits().first().map { it.toUnit() }

    fun searchUnits(query: String): Flow<List<Unit>> =
        if (query.isBlank()) {
            unitDao.getAllUnits().map { entities -> entities.map { it.toUnit() } }
        } else {
            unitDao.searchUnits(query).map { entities -> entities.map { it.toUnit() } }
        }

    override suspend fun getUnitById(id: String): Unit? = unitDao.getUnitById(id)?.toUnit()

    suspend fun getUnitByName(name: String): Unit? = unitDao.getUnitByName(name)?.toUnit()

    /** Offline-first create: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun createUnit(unit: Unit): Result<Unit> {
        return try {
            unitDao.insertUnit(unit.toEntity().copy(synced = false))
            markPending()
            Result.success(unit)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to create unit", e)
            Result.failure(e)
        }
    }

    /** Offline-first update: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun updateUnit(unit: Unit): Result<Unit> {
        return try {
            unitDao.insertUnit(unit.toEntity().copy(synced = false))
            markPending()
            Result.success(unit)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to update unit", e)
            Result.failure(e)
        }
    }

    /** Offline-first delete: mark inactive + unsynced locally and flag for automatic bulk push. */
    suspend fun deleteUnit(id: String): Result<kotlin.Unit> {
        return try {
            val existing = unitDao.getUnitById(id)
            if (existing != null) {
                unitDao.insertUnit(existing.copy(active = false, synced = false))
                markPending()
            } else {
                unitDao.deleteUnit(id)
            }
            Result.success(kotlin.Unit)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to delete unit", e)
            Result.failure(e)
        }
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.UNIT, Clock.System.now().toEpochMilliseconds())
    }
}
