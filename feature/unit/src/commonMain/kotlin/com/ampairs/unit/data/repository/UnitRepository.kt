package com.ampairs.unit.data.repository

import com.ampairs.unit.data.api.UnitApi
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.Inject
import com.ampairs.unit.data.db.entity.toEntity
import com.ampairs.unit.data.db.entity.toUnit
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.util.UnitLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class UnitRepository(
    private val unitApi: UnitApi,
    private val unitDao: UnitDao
) {

    fun observeUnits(): Flow<List<Unit>> =
        unitDao.getAllUnits().map { entities -> entities.map { it.toUnit() } }

    fun searchUnits(query: String): Flow<List<Unit>> =
        if (query.isBlank()) {
            unitDao.getAllUnits().map { entities -> entities.map { it.toUnit() } }
        } else {
            unitDao.searchUnits(query).map { entities -> entities.map { it.toUnit() } }
        }

    suspend fun getUnitById(id: String): Unit? = unitDao.getUnitById(id)?.toUnit()

    suspend fun getUnitByName(name: String): Unit? = unitDao.getUnitByName(name)?.toUnit()

    suspend fun createUnit(unit: Unit): Result<Unit> {
        return try {
            unitDao.insertUnit(unit.toEntity().copy(synced = false))

            try {
                val response = unitApi.createUnit(unit)
                if (response.data != null && response.error == null) {
                    val serverUnit = response.data!!
                    val finalUnit = if (serverUnit.uid != unit.uid) serverUnit.copy(uid = unit.uid) else serverUnit
                    unitDao.insertUnit(finalUnit.toEntity().copy(synced = true))
                    Result.success(finalUnit)
                } else {
                    Result.success(unit)
                }
            } catch (e: Exception) {
                UnitLogger.w("UnitRepository", "Server sync failed, using local data", e)
                Result.success(unit)
            }
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to create unit", e)
            Result.failure(e)
        }
    }

    suspend fun updateUnit(unit: Unit): Result<Unit> {
        return try {
            unitDao.updateUnit(unit.toEntity().copy(synced = false))

            try {
                val response = unitApi.updateUnit(unit.uid, unit)
                if (response.data != null && response.error == null) {
                    unitDao.insertUnit(response.data!!.toEntity().copy(synced = true))
                    Result.success(response.data!!)
                } else {
                    Result.success(unit)
                }
            } catch (e: Exception) {
                UnitLogger.w("UnitRepository", "Server update failed, using local data", e)
                Result.success(unit)
            }
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to update unit", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUnit(id: String): Result<kotlin.Unit> {
        return try {
            unitDao.deleteUnit(id)
            try {
                unitApi.deleteUnit(id)
            } catch (e: Exception) {
                UnitLogger.w("UnitRepository", "Server delete failed, marked inactive locally", e)
            }
            Result.success(kotlin.Unit)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to delete unit", e)
            Result.failure(e)
        }
    }

    suspend fun syncUnits(): Result<Int> {
        return try {
            val unsyncedUnits = unitDao.getUnsyncedUnits()
            var syncedCount = 0

            for (entity in unsyncedUnits) {
                val unit = entity.toUnit()
                try {
                    if (!entity.active) {
                        unitApi.deleteUnit(unit.uid)
                        unitDao.deleteUnit(unit.uid)
                        syncedCount++
                    } else {
                        val response = unitApi.createUnit(unit)
                        if (response.data != null && response.error == null) {
                            unitDao.insertUnit(response.data!!.toEntity().copy(synced = true))
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    UnitLogger.w("UnitRepository", "Failed to sync unit ${unit.uid}", e)
                }
            }

            val serverSyncCount = syncUnitsFromServer().getOrElse { 0 }
            Result.success(syncedCount + serverSyncCount)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun pullFromServer(): Result<Int> = syncUnitsFromServer()

    suspend fun pushPendingToServer(): Result<Int> {
        return try {
            val unsyncedUnits = unitDao.getUnsyncedUnits()
            var syncedCount = 0
            for (entity in unsyncedUnits) {
                val unit = entity.toUnit()
                try {
                    val response = unitApi.createUnit(unit)
                    if (response.data != null && response.error == null) {
                        unitDao.insertUnit(response.data!!.toEntity().copy(synced = true))
                        syncedCount++
                    }
                } catch (_: Exception) {
                    // continue with remaining items
                }
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Push failed", e)
            Result.failure(e)
        }
    }

    suspend fun handleExternalEvent(unitId: String, eventType: String) {
        try {
            val response = unitApi.getUnitById(unitId)
            if (response.data != null && response.error == null) {
                val existing = unitDao.getUnitById(unitId)
                if (existing == null || existing.synced) {
                    unitDao.insertUnit(response.data!!.toEntity().copy(synced = true)) // don't overwrite unsynced local edits
                }
            }
        } catch (e: Exception) {
            UnitLogger.w("UnitRepository", "Failed to handle event for unit $unitId", e)
        }
    }

    private suspend fun syncUnitsFromServer(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            do {
                val pageResponse = unitApi.getUnits(currentPage, batchSize)
                val batchUnits = pageResponse.data?.content ?: emptyList()

                val unitsToInsert = batchUnits.mapNotNull { serverUnit ->
                    val existing = unitDao.getUnitById(serverUnit.uid)
                    if (existing != null && !existing.synced) null
                    else serverUnit.toEntity().copy(synced = true)
                }

                if (unitsToInsert.isNotEmpty()) {
                    unitDao.insertUnits(unitsToInsert)
                    totalSynced += unitsToInsert.size
                }

                currentPage++
            } while (batchUnits.size == batchSize && totalSynced < 10000)

            Result.success(totalSynced)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to sync from server", e)
            Result.failure(e)
        }
    }
}
