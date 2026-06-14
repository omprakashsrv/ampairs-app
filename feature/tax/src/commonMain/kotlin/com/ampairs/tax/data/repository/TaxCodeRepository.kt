package com.ampairs.tax.data.repository

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.tax.util.TaxLogger
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxCodeDao
import dev.zacsweers.metro.Inject
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.data.db.entity.toDomain
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

/**
 * Tax Code Repository - Offline-first repository for tax codes
 */
@Inject
@OptIn(ExperimentalTime::class)
class TaxCodeRepository(
    private val taxConfigApi: TaxConfigurationApi,
    private val taxCodeDao: TaxCodeDao,
    private val syncStateDao: SyncStateDao,
) : TaxCodeLookup {

    private companion object {
        // Backend caps master_tax_code_ids at 100 per bulk-subscribe request.
        const val BULK_SUBSCRIBE_BATCH_SIZE = 100
    }

    /** Flag tax changes for the central push (handled by TaxSyncDelegate). */
    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.TAX, kotlin.time.Clock.System.now().toEpochMilliseconds())

    // ==================== Workspace Tax Codes (Offline Available) ====================

    /**
     * Observe workspace tax codes (offline available)
     */
    fun observeWorkspaceTaxCodes(): Flow<List<TaxCode>> {
        return taxCodeDao.observeTaxCodes()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Observe favorite tax codes
     */
    fun observeFavorites(): Flow<List<TaxCode>> {
        return taxCodeDao.observeFavorites()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get workspace tax code by ID (offline)
     */
    suspend fun getById(id: String): TaxCode? {
        return taxCodeDao.getById(id)?.toDomain()
    }

    /**
     * Get workspace tax code by code (offline)
     */
    suspend fun getByCode(code: String): TaxCode? {
        return taxCodeDao.getByCode(code)?.toDomain()
    }

    /**
     * Search workspace tax codes (offline)
     */
    override suspend fun searchWorkspaceTaxCodes(query: String, limit: Int): List<TaxCode> {
        return taxCodeDao.searchTaxCodes(query, limit)
            .map { it.toDomain() }
    }

    /**
     * Increment usage count when tax code is used
     */
    override suspend fun incrementUsageCount(id: String) {
        val timestamp = kotlin.time.Clock.System.now()
        taxCodeDao.incrementUsageCount(id, timestamp)
    }

    /**
     * Toggle favorite status
     */
    suspend fun setFavorite(id: String, isFavorite: Boolean): Result<Unit> {
        return try {
            taxCodeDao.setFavorite(id, isFavorite)
            taxCodeDao.updateSyncStatus(id, "PENDING")
            markPending()
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.setFavorite")
            Result.failure(e)
        }
    }

    /**
     * Update the workspace-specific notes on a subscribed tax code.
     *
     * Writes locally first (so the edit is immediately reflected offline) then best-effort persists
     * to the server via PATCH; the local write is the source of truth and is marked SYNCED only once
     * the server confirms.
     */
    suspend fun updateNotes(id: String, notes: String?): Result<Unit> {
        return try {
            val existing = taxCodeDao.getById(id)
                ?: return Result.failure(IllegalStateException("Tax code not found: $id"))
            // Local write is the source of truth; TaxSyncDelegate persists it via PATCH on push.
            taxCodeDao.update(existing.copy(notes = notes, syncStatus = "PENDING"))
            markPending()
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.updateNotes")
            Result.failure(e)
        }
    }

    // ==================== Master Tax Codes (Server-Side Search) ====================

    /**
     * Search master tax codes (server-side, requires network)
     */
    suspend fun searchMasterTaxCodes(
        query: String,
        countryCode: String,
        codeType: String? = null,
        category: String? = null,
        page: Int = 0,
        size: Int = 50
    ): Result<List<MasterTaxCode>> {
        return try {
            val response = taxConfigApi.searchMasterTaxCodes(
                query = query,
                countryCode = countryCode,
                codeType = codeType,
                category = category,
                page = page,
                size = size
            )

            if (response.isSuccess) {
                Result.success(response.getOrThrow().content)
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("Search failed"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.searchMasterTaxCodes")
            Result.failure(e)
        }
    }

    /**
     * Get popular tax codes for industry (server-side)
     */
    suspend fun getPopularTaxCodes(
        countryCode: String,
        industry: String? = null,
        limit: Int = 20
    ): Result<List<MasterTaxCode>> {
        return try {
            taxConfigApi.getPopularTaxCodes(countryCode, industry, limit)
                .map { it.content }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.getPopularTaxCodes")
            Result.failure(e)
        }
    }

    // ==================== Subscribe/Unsubscribe Operations ====================

    /**
     * Subscribe workspace to tax code (database-first with background sync)
     */
    suspend fun subscribeToTaxCode(
        masterTaxCodeId: String,
        customTaxRuleId: String? = null,
        isFavorite: Boolean = false,
        notes: String? = null
    ): Result<TaxCode> {
        return try {
            // Call server to subscribe
            val result = taxConfigApi.subscribeToTaxCode(
                masterTaxCodeId = masterTaxCodeId,
                customTaxRuleId = customTaxRuleId,
                isFavorite = isFavorite,
                notes = notes
            )

            if (result.isSuccess) {
                val workspaceTaxCode = result.getOrThrow()

                // Save to local database
                taxCodeDao.insert(workspaceTaxCode.toEntity())

                Result.success(workspaceTaxCode)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Subscription failed"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.subscribeToTaxCode")
            Result.failure(e)
        }
    }

    /**
     * Unsubscribe from a tax code — offline-safe.
     *
     * Soft-deletes locally (inactive + queued for deletion) and flags the entity for the central
     * push; TaxSyncDelegate confirms the unsubscribe on the server then hard-deletes the row. Works
     * offline (the deletion propagates on reconnect) and is crash-safe (no out-of-band hard delete).
     */
    suspend fun unsubscribeFromTaxCode(workspaceTaxCodeId: String): Result<Unit> {
        return try {
            taxCodeDao.deactivate(workspaceTaxCodeId)
            taxCodeDao.updateSyncStatus(workspaceTaxCodeId, "DELETE_PENDING")
            markPending()
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.unsubscribeFromTaxCode")
            Result.failure(e)
        }
    }

    /**
     * Bulk subscribe to multiple tax codes.
     *
     * The backend caps `master_tax_code_ids` at 100 per request (`@field:Size(max = 100)`), so the
     * ids are chunked into batches of [BULK_SUBSCRIBE_BATCH_SIZE]. Each batch's subscribed codes are
     * persisted as they arrive; a failed batch aborts the remaining chunks and surfaces the error.
     */
    suspend fun bulkSubscribeTaxCodes(
        masterTaxCodeIds: List<String>,
        applyDefaultRules: Boolean = true
    ): Result<Int> {
        return try {
            var totalSuccess = 0

            for (chunk in masterTaxCodeIds.chunked(BULK_SUBSCRIBE_BATCH_SIZE)) {
                val result = taxConfigApi.bulkSubscribeTaxCodes(
                    masterTaxCodeIds = chunk,
                    applyDefaultRules = applyDefaultRules
                )

                if (result.isSuccess) {
                    val bulkResult = result.getOrThrow()

                    // Save this batch's subscribed codes to local database
                    val entities = bulkResult.subscribedCodes.map { it.toEntity() }
                    taxCodeDao.insertAll(entities)

                    totalSuccess += bulkResult.successCount
                } else {
                    return Result.failure(
                        result.exceptionOrNull() ?: Exception("Bulk subscription failed")
                    )
                }
            }

            Result.success(totalSuccess)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.bulkSubscribeTaxCodes")
            Result.failure(e)
        }
    }

    // ==================== Sync Operations ====================

    /**
     * Sync workspace tax codes from server (incremental sync)
     */
    suspend fun syncWorkspaceTaxCodes(): Result<Int> {
        return try {
            // Get last sync time
            val lastSync = getLastSyncTime()

            // Fetch updated codes from server
            val result = taxConfigApi.getTaxCodes(
                modifiedAfter = lastSync,
                page = 0,
                size = 1000 // Large batch for sync
            )

            if (result.isSuccess) {
                val response = result.getOrThrow()
                val codes = response.content

                // Upsert to local database
                val entities = codes.map { it.toEntity() }
                taxCodeDao.insertAll(entities)

                Result.success(codes.size)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Sync failed"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.syncWorkspaceTaxCodes")
            Result.failure(e)
        }
    }

    /**
     * Get last sync time for incremental sync
     */
    private suspend fun getLastSyncTime(): kotlin.time.Instant? {
        val codes = taxCodeDao.getModifiedAfter(kotlin.time.Instant.fromEpochMilliseconds(0))
        return codes.maxOfOrNull { it.updatedAt }   // newest updatedAt seen locally
    }

    /**
     * Sync unsynced changes to server
     */
    suspend fun syncUnsyncedChanges(): Result<Int> {
        return try {
            val unsyncedCodes = taxCodeDao.getUnsyncedCodes()
            var syncedCount = 0

            unsyncedCodes.forEach { entity ->
                val code = entity.toDomain()

                when (entity.syncStatus) {
                    "DELETE_PENDING" -> {
                        val result = taxConfigApi.unsubscribeFromTaxCode( code.id)
                        if (result.isSuccess) {
                            taxCodeDao.delete(code.id)
                            syncedCount++
                        }
                    }

                    "PENDING" -> {
                        // Re-subscribe with current settings
                        val result = taxConfigApi.subscribeToTaxCode(
                            masterTaxCodeId = code.masterTaxCodeId,
                            customTaxRuleId = code.customTaxRuleId,
                            isFavorite = code.isFavorite,
                            notes = code.notes
                        )
                        if (result.isSuccess) {
                            taxCodeDao.updateSyncStatus(code.id, "SYNCED")
                            syncedCount++
                        }
                    }
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCodeRepository.syncUnsyncedChanges")
            Result.failure(e)
        }
    }
}
