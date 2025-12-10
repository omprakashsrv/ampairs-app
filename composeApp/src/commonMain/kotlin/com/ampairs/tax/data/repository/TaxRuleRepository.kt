package com.ampairs.tax.data.repository

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxRuleDao
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.data.db.entity.toDomain
import com.ampairs.tax.domain.model.TaxRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

/**
 * Tax Rule Repository - Offline-first repository for tax rules
 */
@OptIn(ExperimentalTime::class)
class TaxRuleRepository(
    private val taxConfigApi: TaxConfigurationApi,
    private val taxRuleDao: TaxRuleDao,
) {

    // ==================== Query Operations (Offline) ====================

    /**
     * Observe all tax rules for workspace (offline)
     */
    fun observeTaxRules(): Flow<List<TaxRule>> {
        return taxRuleDao.observeTaxRules()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Get tax rule by ID (offline)
     */
    suspend fun getById(id: String): TaxRule? {
        return taxRuleDao.getById(id)?.toDomain()
    }

    /**
     * Get effective tax rule for calculation (offline)
     */
    suspend fun getEffectiveRule(
        taxCode: String,
        jurisdiction: String
    ): TaxRule? {
        return taxRuleDao.getEffectiveRule(
            taxCode = taxCode,
            jurisdiction = jurisdiction
        )?.toDomain()
    }

    /**
     * Get tax rules by workspace tax code (offline)
     */
    suspend fun getRulesByTaxCode(workspaceTaxCodeId: String): List<TaxRule> {
        return taxRuleDao.getRulesByTaxCode(workspaceTaxCodeId)
            .map { it.toDomain() }
    }

    /**
     * Get tax rules by workspace tax code with server fallback
     * Tries local first, then fetches from server if needed
     */
    suspend fun getRulesByTaxCodeWithSync(workspaceTaxCodeId: String): Result<List<TaxRule>> {
        return try {
            // Try local database first
            val localRules = taxRuleDao.getRulesByTaxCode(workspaceTaxCodeId)
                .map { it.toDomain() }

            if (localRules.isNotEmpty()) {
                return Result.success(localRules)
            }

            // If not found locally, fetch from server
            val result = taxConfigApi.getTaxRulesByTaxCode(workspaceTaxCodeId)

            if (result.isSuccess) {
                val serverRules = result.getOrThrow()
                // Save to local database for future offline access
                val entities = serverRules.map { it.toEntity().copy(syncStatus = "SYNCED") }
                taxRuleDao.insertAll(entities)
                Result.success(serverRules)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("No tax rules found"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRuleRepository.getRulesByTaxCodeWithSync")
            Result.failure(e)
        }
    }

    // ==================== CRUD Operations (Database-First) ====================

    /**
     * Create tax rule (database-first with background sync)
     */
    suspend fun createTaxRule(rule: TaxRule): Result<TaxRule> {
        return try {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val ruleWithTimestamp = rule.copy(
                createdAt = now,
                updatedAt = now,
                syncStatus = "PENDING"
            )

            // Save to local database first
            taxRuleDao.insert(ruleWithTimestamp.toEntity())

            // Try to sync with server in background
            try {
                val result = taxConfigApi.createTaxRule(ruleWithTimestamp)

                if (result.isSuccess) {
                    val serverRule = result.getOrThrow()
                    taxRuleDao.updateSyncStatus(serverRule.id, "SYNCED")
                    Result.success(serverRule)
                } else {
                    // Keep as PENDING, will sync later
                    Result.success(ruleWithTimestamp)
                }
            } catch (e: Exception) {
                // Network error - keep as PENDING
                ErrorTracking.captureException(e, "TaxRuleRepository.createTaxRule.sync")
                Result.success(ruleWithTimestamp)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRuleRepository.createTaxRule")
            Result.failure(e)
        }
    }

    /**
     * Update tax rule (database-first with background sync)
     */
    suspend fun updateTaxRule(rule: TaxRule): Result<TaxRule> {
        return try {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val updatedRule = rule.copy(
                updatedAt = now,
                syncStatus = "PENDING"
            )

            // Update local database
            taxRuleDao.update(updatedRule.toEntity())

            // Try to sync with server in background
            try {
                val result = taxConfigApi.updateTaxRule(rule.id, updatedRule)

                if (result.isSuccess) {
                    val serverRule = result.getOrThrow()
                    taxRuleDao.updateSyncStatus(serverRule.id, "SYNCED")
                    Result.success(serverRule)
                } else {
                    // Keep as PENDING
                    Result.success(updatedRule)
                }
            } catch (e: Exception) {
                // Network error - keep as PENDING
                ErrorTracking.captureException(e, "TaxRuleRepository.updateTaxRule.sync")
                Result.success(updatedRule)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRuleRepository.updateTaxRule")
            Result.failure(e)
        }
    }

    /**
     * Deactivate tax rule
     */
    suspend fun deactivateTaxRule(id: String): Result<Unit> {
        return try {
            taxRuleDao.deactivate(id)
            taxRuleDao.updateSyncStatus(id, "PENDING")
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRuleRepository.deactivateTaxRule")
            Result.failure(e)
        }
    }

    /**
     * Bulk import tax rules
     */
    suspend fun bulkImportTaxRules(rules: List<TaxRule>): Result<Int> {
        return try {
            // Save all rules to local database first
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val rulesWithTimestamp = rules.map { rule ->
                rule.copy(
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = "PENDING"
                )
            }

            val entities = rulesWithTimestamp.map { it.toEntity() }
            taxRuleDao.insertAll(entities)

            // Try to bulk import to server in background
            try {
                val result = taxConfigApi.bulkImportTaxRules(rulesWithTimestamp)

                if (result.isSuccess) {
                    val bulkResult = result.getOrThrow()

                    // Mark imported rules as synced
                    bulkResult.importedRules.forEach { rule ->
                        taxRuleDao.updateSyncStatus(rule.id, "SYNCED")
                    }

                    Result.success(bulkResult.successCount)
                } else {
                    // Keep as PENDING
                    Result.success(rules.size)
                }
            } catch (e: Exception) {
                // Network error - keep as PENDING
                ErrorTracking.captureException(e, "TaxRuleRepository.bulkImportTaxRules.sync")
                Result.success(rules.size)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRuleRepository.bulkImportTaxRules")
            Result.failure(e)
        }
    }

    // ==================== Sync Operations ====================

    /**
     * Sync tax rules from server (incremental sync)
     */
    suspend fun syncTaxRules(): Result<Int> {
        return try {
            // Get last sync time
            val lastSync = getLastSyncTime()

            // Fetch updated rules from server (paginated)
            var totalSynced = 0
            var page = 0
            val pageSize = 100

            do {
                val result = taxConfigApi.getTaxRules(
                    modifiedAfter = lastSync,
                    page = page,
                    size = pageSize
                )

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val rules = response.content

                    // Upsert to local database
                    val entities = rules.map { it.toEntity().copy(syncStatus = "SYNCED") }
                    taxRuleDao.insertAll(entities)

                    totalSynced += rules.size

                    // Check if more pages available
                    if (!response.hasNext || rules.isEmpty()) {
                        break
                    }

                    page++
                } else {
                    break
                }
            } while (totalSynced < 10000) // Safety limit

            Result.success(totalSynced)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRuleRepository.syncTaxRules")
            Result.failure(e)
        }
    }

    /**
     * Get last sync time for incremental sync
     */
    private suspend fun getLastSyncTime(): Long? {
        val rules = taxRuleDao.getModifiedAfter(0L)
        return rules.maxOfOrNull { it.updatedAt }
    }

    /**
     * Sync unsynced changes to server
     */
    suspend fun syncUnsyncedChanges(): Result<Int> {
        return try {
            val unsyncedRules = taxRuleDao.getUnsyncedRules()
            var syncedCount = 0

            unsyncedRules.forEach { entity ->
                val rule = entity.toDomain()

                try {
                    // Determine if create or update based on whether it exists on server
                    val result = if (entity.createdAt == entity.updatedAt) {
                        taxConfigApi.createTaxRule( rule)
                    } else {
                        taxConfigApi.updateTaxRule( rule.id, rule)
                    }

                    if (result.isSuccess) {
                        taxRuleDao.updateSyncStatus(rule.id, "SYNCED")
                        syncedCount++
                    }
                } catch (e: Exception) {
                    ErrorTracking.captureException(e, "TaxRuleRepository.syncUnsyncedChanges.item")
                    // Continue with next item
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRuleRepository.syncUnsyncedChanges")
            Result.failure(e)
        }
    }
}
