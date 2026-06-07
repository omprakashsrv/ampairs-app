package com.ampairs.unit.data.repository

import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.repository.UnitLookup
import com.ampairs.unit.data.repository.UnitOption
import com.ampairs.unit.data.repository.UnitOptionsLookup
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
    private val unitConversionDao: UnitConversionDao,
    private val syncStateDao: SyncStateDao,
) : UnitLookup, UnitOptionsLookup {

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

    override suspend fun unitsForProduct(productId: String, baseUnitId: String?): List<UnitOption> {
        val base = baseUnitId?.takeIf { it.isNotBlank() }?.let { unitDao.getUnitById(it)?.toUnit() }
        val options = mutableListOf<UnitOption>()
        if (base != null) {
            options.add(
                UnitOption(
                    unitId = base.uid,
                    name = base.name,
                    shortName = base.shortName,
                    multiplier = 1.0,
                    isBase = true,
                    decimalPlaces = base.decimalPlaces,
                )
            )
        }
        val conversions = unitConversionDao.getUnitConversionsByProductId(productId).first()
        for (conversion in conversions) {
            // Only surface conversions hanging off the product's base unit (if known).
            if (base != null && conversion.baseUnitId != base.uid) continue
            if (options.any { it.unitId == conversion.derivedUnitId }) continue
            val derived = unitDao.getUnitById(conversion.derivedUnitId)?.toUnit() ?: continue
            if (conversion.multiplier <= 0.0) continue
            options.add(
                UnitOption(
                    unitId = derived.uid,
                    name = derived.name,
                    shortName = derived.shortName,
                    multiplier = conversion.multiplier,
                    isBase = false,
                    decimalPlaces = derived.decimalPlaces,
                )
            )
        }
        return options
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.UNIT, Clock.System.now().toEpochMilliseconds())
    }
}
