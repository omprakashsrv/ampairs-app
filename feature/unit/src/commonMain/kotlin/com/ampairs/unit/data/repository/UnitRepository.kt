package com.ampairs.unit.data.repository

import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.unit.data.api.UnitApi
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.Inject
import com.ampairs.unit.data.db.entity.toEntity
import com.ampairs.unit.data.db.entity.toUnit
import com.ampairs.unit.domain.UnitStore
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.domain.model.UnitKey
import com.ampairs.unit.util.UnitConstants
import com.ampairs.unit.util.UnitLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Repository for Unit Management with offline-first architecture
 *
 * Implements the complete offline-first pattern:
 * 1. Database-first operations (save locally with synced = false)
 * 2. Background server synchronization
 * 3. Graceful fallback if server fails
 * 4. Batch sync with conflict resolution
 * 5. UID generation and correction
 *
 * Constructor dependencies injected via Metro (all as unscoped factory).
 */
@Inject
class UnitRepository(
    private val unitApi: UnitApi,
    private val unitDao: UnitDao,
    private val unitStore: UnitStore
) {

    // ======================================
    // READ OPERATIONS
    // ======================================

    /**
     * Get units with Store5 integration for offline-first data flow
     *
     * Store5 automatically handles:
     * - Reading from local database (SourceOfTruth)
     * - Fetching from API if cache expired or forceRefresh = true
     * - Writing API results to database
     *
     * @param page Page number (0-indexed)
     * @param size Page size (default: 100)
     * @param forceRefresh Force fetch from API even if cache is valid
     * @return Flow of StoreReadResponse with unit list
     */
    fun getUnitsFlow(
        page: Int = 0,
        size: Int = 100,
        forceRefresh: Boolean = false
    ): Flow<StoreReadResponse<List<Unit>>> {
        val key = UnitKey(page = page, size = size)
        return unitStore.unitStore.stream(
            StoreReadRequest.cached(key, refresh = forceRefresh)
        )
    }

    /**
     * Search units by name
     *
     * Direct DAO query bypasses Store5 for immediate search results
     *
     * @param query Search query string
     * @return Flow of matching units
     */
    fun searchUnits(query: String): Flow<List<Unit>> {
        return if (query.isBlank()) {
            unitDao.getAllUnits().map { entities ->
                entities.map { it.toUnit() }
            }
        } else {
            unitDao.searchUnits(query).map { entities ->
                entities.map { it.toUnit() }
            }
        }
    }

    /**
     * Get all units for dropdown/autocomplete
     *
     * @return Flow of all active units
     */
    fun getAllUnitsFlow(): Flow<List<Unit>> {
        return unitStore.getUnitsFlow()
    }

    /**
     * Get single unit by ID
     *
     * @param id Unit UID
     * @return Unit if found, null otherwise
     */
    suspend fun getUnitById(id: String): Unit? {
        return unitStore.getUnitById(id)
    }

    /**
     * Get unit by name (for validation)
     *
     * @param name Unit name
     * @return Unit if found, null otherwise
     */
    suspend fun getUnitByName(name: String): Unit? {
        return unitStore.getUnitByName(name)
    }

    // ======================================
    // CREATE OPERATION (OFFLINE-FIRST)
    // ======================================

    /**
     * Create a new unit using offline-first pattern
     *
     * Critical Pattern:
     * 1. Generate UID locally if not provided
     * 2. Save to database FIRST with synced = false
     * 3. Try server sync in background
     * 4. Correct server UID if mismatch (maintain local UID consistency)
     * 5. Return success even if server fails (graceful fallback)
     *
     * @param unit Unit to create
     * @return Result with created unit (always succeeds if DB save works)
     */
    suspend fun createUnit(unit: Unit): Result<Unit> {
        return try {
            // 1. Generate UID if not provided
            val uid = if (unit.uid.isBlank()) {
                UidGenerator.generateUid(UnitConstants.UNIT_UID_PREFIX)
            } else {
                unit.uid
            }

            val unitWithUid = unit.copy(uid = uid)
            UnitLogger.d("UnitRepository", "Creating unit: ${unitWithUid.name} (${unitWithUid.uid})")

            // 2. Save to database FIRST with unsynced status
            val unsyncedEntity = unitWithUid.toEntity().copy(synced = false)
            unitDao.insertUnit(unsyncedEntity)
            UnitLogger.i("UnitRepository", "Unit saved locally: ${unitWithUid.uid}")

            // 3. Try to sync with server (background operation)
            try {
                val response = unitApi.createUnit(unitWithUid)

                if (response.data != null && response.error == null) {
                    val serverUnit = response.data!!

                    // 4. Correct server UID if mismatch (maintain local UID consistency)
                    val finalUnit = if (serverUnit.uid != uid) {
                        UnitLogger.w(
                            "UnitRepository",
                            "Server returned different UID: ${serverUnit.uid} vs local: $uid. Using local UID."
                        )
                        serverUnit.copy(uid = uid)
                    } else {
                        serverUnit
                    }

                    // Mark as synced in database
                    unitDao.insertUnit(finalUnit.toEntity().copy(synced = true))
                    UnitLogger.i("UnitRepository", "Unit synced with server: ${finalUnit.uid}")

                    Result.success(finalUnit)
                } else {
                    // Server returned error but data is already saved locally
                    UnitLogger.w("UnitRepository", "Server error: ${response.error?.message}. Using local data.")
                    Result.success(unitWithUid)
                }
            } catch (e: Exception) {
                // 5. Graceful fallback - data already saved locally
                UnitLogger.w("UnitRepository", "Server sync failed, using local data", e)
                Result.success(unitWithUid)
            }
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to create unit", e)
            Result.failure(e)
        }
    }

    // ======================================
    // UPDATE OPERATION (OFFLINE-FIRST)
    // ======================================

    /**
     * Update an existing unit using offline-first pattern
     *
     * Pattern: Save locally first, then sync with server
     *
     * @param unit Unit to update
     * @return Result with updated unit
     */
    suspend fun updateUnit(unit: Unit): Result<Unit> {
        return try {
            UnitLogger.d("UnitRepository", "Updating unit: ${unit.name} (${unit.uid})")

            // 1. Save to database FIRST with unsynced status
            val unsyncedEntity = unit.toEntity().copy(synced = false)
            unitDao.updateUnit(unsyncedEntity)
            UnitLogger.i("UnitRepository", "Unit updated locally: ${unit.uid}")

            // 2. Try to sync with server
            try {
                val response = unitApi.updateUnit(unit.uid, unit)

                if (response.data != null && response.error == null) {
                    // Mark as synced in database
                    unitDao.insertUnit(response.data!!.toEntity().copy(synced = true))
                    UnitLogger.i("UnitRepository", "Unit synced with server: ${unit.uid}")
                    Result.success(response.data!!)
                } else {
                    UnitLogger.w("UnitRepository", "Server error: ${response.error?.message}. Using local data.")
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

    // ======================================
    // DELETE OPERATION
    // ======================================

    /**
     * Delete a unit (soft delete)
     *
     * Pattern: Mark inactive locally first, then try server delete
     *
     * @param id Unit UID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteUnit(id: String): Result<kotlin.Unit> {
        return try {
            UnitLogger.d("UnitRepository", "Deleting unit: $id")

            // 1. Mark as inactive locally first (soft delete)
            unitDao.deleteUnit(id)
            UnitLogger.i("UnitRepository", "Unit marked inactive locally: $id")

            // 2. Try to delete from server
            try {
                unitApi.deleteUnit(id)
                UnitLogger.i("UnitRepository", "Unit deleted from server: $id")
            } catch (e: Exception) {
                UnitLogger.w("UnitRepository", "Server delete failed, marked inactive locally", e)
                // Ignore server error - already marked locally
            }

            Result.success(kotlin.Unit)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to delete unit", e)
            Result.failure(e)
        }
    }

    // ======================================
    // SYNC OPERATIONS (BATCH SYNC WITH CONFLICT RESOLUTION)
    // ======================================

    /**
     * Synchronize units with server using offline-first batch sync
     *
     * Two-phase sync:
     * 1. Push unsynced local units to server (prevents data loss)
     * 2. Pull updates from server in batches (100 items per page)
     *
     * Conflict Resolution: Preserve unsynced local changes over server data
     *
     * @return Result with total count of synchronized units
     */
    suspend fun syncUnits(): Result<Int> {
        return try {
            UnitLogger.i("UnitRepository", "Starting unit synchronization")

            // PHASE 1: Sync unsynced local units to server (prevents data loss)
            val unsyncedUnits = unitDao.getUnsyncedUnits()
            var syncedCount = 0

            UnitLogger.d("UnitRepository", "Found ${unsyncedUnits.size} unsynced local units")

            for (entity in unsyncedUnits) {
                val unit = entity.toUnit()
                try {
                    if (!entity.active) {
                        // Handle deleted units
                        unitApi.deleteUnit(unit.uid)
                        unitDao.deleteUnit(unit.uid)
                        syncedCount++
                        UnitLogger.d("UnitRepository", "Synced deletion: ${unit.uid}")
                    } else {
                        // Handle created/updated units
                        val response = if (entity.synced) {
                            unitApi.updateUnit(unit.uid, unit)
                        } else {
                            unitApi.createUnit(unit)
                        }

                        if (response.data != null && response.error == null) {
                            unitDao.insertUnit(response.data!!.toEntity().copy(synced = true))
                            syncedCount++
                            UnitLogger.d("UnitRepository", "Synced unit: ${unit.uid}")
                        }
                    }
                } catch (e: Exception) {
                    UnitLogger.w("UnitRepository", "Failed to sync unit ${unit.uid}", e)
                    // Continue with next item
                }
            }

            // PHASE 2: Sync from server in batches (incremental sync)
            val serverSyncResult = syncUnitsFromServerInBatches()
            val serverSyncCount = serverSyncResult.getOrElse { 0 }

            val totalSynced = syncedCount + serverSyncCount
            UnitLogger.i(
                "UnitRepository",
                "Sync completed: $syncedCount local → server, $serverSyncCount server → local, $totalSynced total"
            )

            Result.success(totalSynced)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sync units from server in batches using timestamp-based incremental sync
     *
     * Features:
     * - Paginated fetch (100 items per page)
     * - Conflict resolution: Preserves unsynced local changes
     * - Safety limit: Maximum 10,000 records
     * - Infinite loop protection
     *
     * @param batchSize Number of records per page (default: 100)
     * @return Result with count of synced units
     */
    private suspend fun syncUnitsFromServerInBatches(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            UnitLogger.d("UnitRepository", "Starting batch sync from server (batch size: $batchSize)")

            do {
                // Fetch one page of units
                val pageResponse = unitApi.getUnits(currentPage, batchSize)
                val batchUnits = pageResponse.data?.content ?: emptyList()

                UnitLogger.d("UnitRepository", "Page $currentPage: ${batchUnits.size} units")

                // Process batch with conflict resolution
                val unitsToInsert = batchUnits.mapNotNull { serverUnit ->
                    val existing = unitDao.getUnitById(serverUnit.uid)
                    if (existing != null && !existing.synced) {
                        // Skip server entity to preserve local changes
                        UnitLogger.d("UnitRepository", "Skipping ${serverUnit.uid} - local changes exist")
                        null
                    } else {
                        serverUnit.toEntity().copy(synced = true)
                    }
                }

                if (unitsToInsert.isNotEmpty()) {
                    unitDao.insertUnits(unitsToInsert)
                    totalSynced += unitsToInsert.size
                    UnitLogger.d("UnitRepository", "Inserted ${unitsToInsert.size} units from page $currentPage")
                }

                currentPage++
            } while (batchUnits.size == batchSize && totalSynced < 10000) // Safety limit

            UnitLogger.i("UnitRepository", "Batch sync completed: $totalSynced units synced")
            Result.success(totalSynced)
        } catch (e: Exception) {
            UnitLogger.e("UnitRepository", "Failed to sync from server", e)
            Result.failure(e)
        }
    }
}
