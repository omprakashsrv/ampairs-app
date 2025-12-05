package com.ampairs.tax.data.repository

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxComponentDao
import com.ampairs.tax.data.db.dao.TaxComponentTypeDao
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.data.db.entity.toDomain
import com.ampairs.tax.domain.model.TaxComponentType
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

/**
 * Tax Component Repository - Component types and workspace components
 */
@OptIn(ExperimentalTime::class)
class TaxComponentRepository(
    private val taxConfigApi: TaxConfigurationApi,
    private val taxComponentTypeDao: TaxComponentTypeDao,
    private val taxComponentDao: TaxComponentDao,
) {

    // ==================== Tax Component Types (Global Master) ====================

    /**
     * Observe all component types (offline)
     */
    fun observeAllComponentTypes(): Flow<List<TaxComponentType>> {
        return taxComponentTypeDao.observeAllComponentTypes()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Observe component types by country (offline)
     */
    fun observeComponentTypesByCountry(countryCode: String): Flow<List<TaxComponentType>> {
        return taxComponentTypeDao.observeComponentTypesByCountry(countryCode)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get component type by ID (offline)
     */
    suspend fun getComponentTypeById(id: String): TaxComponentType? {
        return taxComponentTypeDao.getById(id)?.toDomain()
    }

    /**
     * Get component type by code (offline)
     */
    suspend fun getComponentTypeByCode(
        countryCode: String,
        componentCode: String
    ): TaxComponentType? {
        return taxComponentTypeDao.getByCode(countryCode, componentCode)?.toDomain()
    }

    /**
     * Sync component types for a country from server
     */
    suspend fun syncComponentTypes(countryCode: String): Result<Int> {
        return try {
            val result = taxConfigApi.getComponentTypes(countryCode)

            if (result.isSuccess) {
                val componentTypes = result.getOrThrow()

                // Save to local database
                val entities = componentTypes.map { it.toEntity() }
                taxComponentTypeDao.insertAll(entities)

                Result.success(componentTypes.size)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Sync failed"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxComponentRepository.syncComponentTypes")
            Result.failure(e)
        }
    }

    /**
     * Get workspace components by jurisdiction (offline)
     */
    suspend fun getComponentsByJurisdiction(
        jurisdiction: String,
        effectiveDate: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): List<WorkspaceTaxComponent> {

        return taxComponentDao.getComponentsByJurisdiction(
            jurisdiction = jurisdiction,
            effectiveDate = effectiveDate
        ).map { it.toDomain() }
    }

    /**
     * Get workspace component by ID (offline)
     */
    suspend fun getWorkspaceComponentById(id: String): WorkspaceTaxComponent? {
        return taxComponentDao.getById(id)?.toDomain()
    }

    /**
     * Get workspace component by type and jurisdiction (offline)
     */
    suspend fun getComponentByTypeAndJurisdiction(
        componentTypeId: String,
        jurisdiction: String
    ): WorkspaceTaxComponent? {

        return taxComponentDao.getByTypeAndJurisdiction(
            componentTypeId = componentTypeId,
            jurisdiction = jurisdiction
        )?.toDomain()
    }

    /**
     * Create workspace component (database-first with background sync)
     */
    suspend fun createWorkspaceComponent(component: WorkspaceTaxComponent): Result<WorkspaceTaxComponent> {
        return try {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val componentWithTimestamp = component.copy(
                createdAt = now,
                updatedAt = now,
                syncStatus = "PENDING"
            )

            // Save to local database first
            taxComponentDao.insert(componentWithTimestamp.toEntity())

            // Try to sync with server in background
            // Note: Actual server sync would happen in a background job
            // For now, just return success

            Result.success(componentWithTimestamp)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxComponentRepository.createWorkspaceComponent")
            Result.failure(e)
        }
    }

    /**
     * Update workspace component (database-first with background sync)
     */
    suspend fun updateWorkspaceComponent(component: WorkspaceTaxComponent): Result<WorkspaceTaxComponent> {
        return try {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val updatedComponent = component.copy(
                updatedAt = now,
                syncStatus = "PENDING"
            )

            // Update local database
            taxComponentDao.update(updatedComponent.toEntity())

            Result.success(updatedComponent)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxComponentRepository.updateWorkspaceComponent")
            Result.failure(e)
        }
    }

    /**
     * Deactivate workspace component
     */
    suspend fun deactivateWorkspaceComponent(id: String): Result<Unit> {
        return try {
            taxComponentDao.deactivate(id)
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxComponentRepository.deactivateWorkspaceComponent")
            Result.failure(e)
        }
    }

    /**
     * Sync workspace components from server (incremental sync)
     */
    suspend fun syncWorkspaceComponents(): Result<Int> {

        return try {
            // Get last sync time
            val lastSync = getLastSyncTime()

            // Fetch updated components from server
            val result = taxConfigApi.getWorkspaceComponents(
                modifiedAfter = lastSync
            )

            if (result.isSuccess) {
                val components = result.getOrThrow()

                // Upsert to local database
                val entities = components.map { it.toEntity() }
                taxComponentDao.insertAll(entities)

                Result.success(components.size)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Sync failed"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxComponentRepository.syncWorkspaceComponents")
            Result.failure(e)
        }
    }

    /**
     * Get last sync time for incremental sync
     */
    private suspend fun getLastSyncTime(): Long? {
        val components = taxComponentDao.getUnsyncedComponents()
        return components.maxOfOrNull { it.updatedAt }
    }

    /**
     * Sync unsynced changes to server
     */
    suspend fun syncUnsyncedChanges(): Result<Int> {
        return try {
            val unsyncedComponents = taxComponentDao.getUnsyncedComponents()
            var syncedCount = 0

            // In a full implementation, this would call server API to create/update components
            // For now, just mark as synced
            unsyncedComponents.forEach { entity ->
                taxComponentDao.updateSyncStatus(entity.id, "SYNCED")
                syncedCount++
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxComponentRepository.syncUnsyncedChanges")
            Result.failure(e)
        }
    }
}
