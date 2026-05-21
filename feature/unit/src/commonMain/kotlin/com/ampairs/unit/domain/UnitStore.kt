package com.ampairs.unit.domain

import com.ampairs.unit.data.api.UnitApi
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.Inject
import com.ampairs.unit.data.db.entity.toEntity
import com.ampairs.unit.data.db.entity.toUnit
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.domain.model.UnitKey
import com.ampairs.unit.util.UnitLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

/**
 * Store5 integration for Unit Management
 *
 * Provides reactive offline-first data flow:
 * - Fetcher: Loads data from API
 * - SourceOfTruth: Reads/writes to Room database
 * - Store: Coordinates between network and database
 *
 * Benefits:
 * - Automatic caching
 * - Cache invalidation
 * - Concurrent request deduplication
 * - Error handling
 */
@Inject
class UnitStore(
    private val unitApi: UnitApi,
    private val unitDao: UnitDao
) {

    /**
     * Main Store5 instance for unit list queries
     *
     * Supports:
     * - Pagination via UnitKey
     * - Search via UnitKey.searchQuery
     * - Force refresh
     */
    val unitStore: Store<UnitKey, List<Unit>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key ->
                kotlinx.coroutines.flow.flow {
                    try {
                        UnitLogger.d("UnitStore", "Fetching units from API: page=${key.page}, query='${key.searchQuery}'")

                        val response = if (key.searchQuery.isNotBlank()) {
                            unitApi.searchUnits(key.searchQuery, key.page, key.size)
                        } else {
                            unitApi.getUnits(key.page, key.size)
                        }

                        if (response.data != null && response.error == null) {
                            val units = response.data!!.content
                            UnitLogger.i("UnitStore", "Fetched ${units.size} units from API")
                            emit(units)
                        } else {
                            val errorMessage = response.error?.message ?: "Failed to fetch units"
                            UnitLogger.e("UnitStore", "API error: $errorMessage")
                            throw Exception(errorMessage)
                        }
                    } catch (e: Exception) {
                        UnitLogger.e("UnitStore", "Error fetching units", e)
                        throw e
                    }
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: UnitKey ->
                    // Read from local database
                    if (key.searchQuery.isNotBlank()) {
                        unitDao.searchUnits(key.searchQuery).map { entities ->
                            val units = entities.map { it.toUnit() }
                            UnitLogger.d("UnitStore", "Read ${units.size} units from DB (search: '${key.searchQuery}')")
                            units
                        }
                    } else {
                        unitDao.getAllUnits().map { entities ->
                            val units = entities.map { it.toUnit() }
                            UnitLogger.d("UnitStore", "Read ${units.size} units from DB")
                            units
                        }
                    }
                },
                writer = { _: UnitKey, units: List<Unit> ->
                    // Write to local database (mark as synced since data came from server)
                    try {
                        val entities = units.map { it.toEntity().copy(synced = true) }
                        unitDao.insertUnits(entities)
                        UnitLogger.i("UnitStore", "Wrote ${entities.size} units to DB")
                    } catch (e: Exception) {
                        UnitLogger.e("UnitStore", "Failed to write units to DB", e)
                        throw e
                    }
                }
            )
        )
        .build()

    /**
     * Get all units for selection/autocomplete
     *
     * Bypasses Store5 for direct database access (no network calls)
     *
     * @return Flow of all active units
     */
    fun getUnitsFlow(): Flow<List<Unit>> {
        return unitDao.getAllUnits().map { entities ->
            entities.map { it.toUnit() }
        }
    }

    /**
     * Get unit by ID for form editing
     *
     * One-time database fetch (no network call)
     *
     * @param id Unit UID
     * @return Unit if found, null otherwise
     */
    suspend fun getUnitById(id: String): Unit? {
        return unitDao.getUnitById(id)?.toUnit()
    }

    /**
     * Get unit by name for form validation
     *
     * One-time database fetch (no network call)
     *
     * @param name Unit name
     * @return Unit if found, null otherwise
     */
    suspend fun getUnitByName(name: String): Unit? {
        return unitDao.getUnitByName(name)?.toUnit()
    }
}
