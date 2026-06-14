package com.ampairs.tax.data.repository

import com.ampairs.tax.util.TaxLogger

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxRuleDao
import dev.zacsweers.metro.Inject
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.data.db.entity.toDomain
import com.ampairs.tax.domain.model.TaxRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

/**
 * Tax Rule Repository - Offline-first repository for tax rules
 */
@Inject
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
        TaxLogger.d("TaxRuleRepo", "🔍 [TaxRuleRepo] getEffectiveRule: taxCode=$taxCode, jurisdiction=$jurisdiction")
        val entity = taxRuleDao.getEffectiveRule(
            taxCode = taxCode,
            jurisdiction = jurisdiction
        )
        if (entity != null) {
            TaxLogger.d("TaxRuleRepo", "✅ [TaxRuleRepo] Found rule: ID=${entity.id}, jurisdiction=${entity.jurisdiction}")
            val domainRule = entity.toDomain()
            TaxLogger.d("TaxRuleRepo", "   Component composition keys: ${domainRule.componentComposition.keys}")
            domainRule.componentComposition.forEach { (key, comp) ->
                TaxLogger.d("TaxRuleRepo", "   - Scenario: $key, components: ${comp.components.size}, total_rate: ${comp.totalRate}")
            }
            return domainRule
        } else {
            TaxLogger.d("TaxRuleRepo", "❌ [TaxRuleRepo] No rule found for taxCode=$taxCode, jurisdiction=$jurisdiction")
            return null
        }
    }

    /**
     * Observe tax rules for a specific tax code by code string (reactive, offline)
     * Use this when backend returns empty tax_code_id
     */
    fun observeTaxRulesByCode(taxCode: String): Flow<List<TaxRule>> {
        TaxLogger.d("TaxRuleRepo", "👀 [TaxRuleRepo] Setting up observer for tax_code string: $taxCode")
        return taxRuleDao.observeRulesByCodeString(taxCode)
            .map { entities ->
                TaxLogger.d("TaxRuleRepo", "👀 [TaxRuleRepo] Flow emitted ${entities.size} entities for tax_code: $taxCode")
                entities.map { it.toDomain() }
            }
    }

    /**
     * Observe tax rules for a specific tax code by ID (reactive, offline)
     * Use this when backend returns proper tax_code_id
     */
    fun observeTaxRulesForTaxCode(workspaceTaxCodeId: String): Flow<List<TaxRule>> {
        TaxLogger.d("TaxRuleRepo", "👀 [TaxRuleRepo] Setting up observer for tax_code_id: $workspaceTaxCodeId")
        return taxRuleDao.observeRulesByTaxCode(workspaceTaxCodeId)
            .map { entities ->
                TaxLogger.d("TaxRuleRepo", "👀 [TaxRuleRepo] Flow emitted ${entities.size} entities for tax_code_id: $workspaceTaxCodeId")
                entities.map { it.toDomain() }
            }
    }

    /**
     * Get tax rules by workspace tax code (offline)
     */
    suspend fun getRulesByTaxCode(workspaceTaxCodeId: String): List<TaxRule> {
        TaxLogger.d("TaxRuleRepo", "🔍 [TaxRuleRepo] getRulesByTaxCode for ID: $workspaceTaxCodeId")
        val rules = taxRuleDao.getRulesByTaxCode(workspaceTaxCodeId)
            .map { it.toDomain() }
        TaxLogger.d("TaxRuleRepo", "🔍 [TaxRuleRepo] Found ${rules.size} rules in local DB")
        return rules
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
            val now = kotlin.time.Clock.System.now()
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
            val now = kotlin.time.Clock.System.now()
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
            val now = kotlin.time.Clock.System.now()
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
            TaxLogger.d("TaxRuleRepo", "🔄 [TaxRuleRepo] Starting sync with lastSync: $lastSync")

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

                    TaxLogger.d("TaxRuleRepo", "📥 [TaxRuleRepo] Page $page: Received ${rules.size} rules")

                    // Log first few rules for debugging
                    rules.take(3).forEach { rule ->
                        TaxLogger.d("TaxRuleRepo", "  - Rule: ID=${rule.id}, tax_code_id=${rule.taxCodeId}, tax_code=${rule.taxCode}, jurisdiction=${rule.jurisdiction}")
                    }

                    // Upsert to local database
                    try {
                        val entities = rules.map { it.toEntity().copy(syncStatus = "SYNCED") }
                        TaxLogger.d("TaxRuleRepo", "📥 [TaxRuleRepo] Converting ${entities.size} rules to entities...")

                        // Log first entity details
                        entities.firstOrNull()?.let { entity ->
                            TaxLogger.d("TaxRuleRepo", "  - First entity: ID=${entity.id}, taxCodeId=${entity.taxCodeId}, taxCode=${entity.taxCode}")
                        }

                        taxRuleDao.insertAll(entities)
                        TaxLogger.d("TaxRuleRepo", "✅ [TaxRuleRepo] Successfully inserted ${entities.size} entities to DB")
                    } catch (e: Exception) {
                        TaxLogger.d("TaxRuleRepo", "❌ [TaxRuleRepo] Failed to insert entities: ${e.message}")
                        e.printStackTrace()
                    }

                    totalSynced += rules.size

                    // Check if more pages available
                    if (!response.hasNext || rules.isEmpty()) {
                        break
                    }

                    page++
                } else {
                    TaxLogger.d("TaxRuleRepo", "❌ [TaxRuleRepo] Sync failed: ${result.exceptionOrNull()?.message}")
                    break
                }
            } while (totalSynced < 10000) // Safety limit

            TaxLogger.d("TaxRuleRepo", "✅ [TaxRuleRepo] Sync complete: $totalSynced rules synced")
            Result.success(totalSynced)
        } catch (e: Exception) {
            TaxLogger.d("TaxRuleRepo", "❌ [TaxRuleRepo] Sync exception: ${e.message}")
            ErrorTracking.captureException(e, "TaxRuleRepository.syncTaxRules")
            Result.failure(e)
        }
    }

    /**
     * Get last sync time for incremental sync
     */
    private suspend fun getLastSyncTime(): kotlin.time.Instant? {
        val rules = taxRuleDao.getModifiedAfter(kotlin.time.Instant.fromEpochMilliseconds(0))
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
