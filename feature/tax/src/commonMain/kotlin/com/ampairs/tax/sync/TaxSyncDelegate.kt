package com.ampairs.tax.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.model.PageResponse
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxCodeDao
import com.ampairs.tax.data.db.dao.TaxComponentDao
import com.ampairs.tax.data.db.dao.TaxRuleDao
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.util.TaxLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Aggregate tax sync delegate (clean-hybrid model).
 *
 * Tax is a subscribe-model entity: the *server* mints workspace tax codes, so adds happen as online
 * subscribe/bulk-subscribe calls (kept in the repository as the allowed UI-invoked exception). This
 * delegate owns everything else under one [SyncEntity.TAX] checkpoint:
 *
 * - **pull** — incremental pull of codes, rules and components (server `updatedAt` is authoritative).
 *   Local unsynced edits win; rows the server reports inactive are hard-deleted locally.
 * - **push** — queued local changes flagged by the repository (`sync_status != 'SYNCED'`):
 *   inactive rows are unsubscribed on the server then hard-deleted locally; active rows (favorite /
 *   notes edits) are persisted via PATCH. Rules and components are pull-only.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.TAX)
@OptIn(ExperimentalTime::class)
class TaxSyncDelegate(
    private val api: TaxConfigurationApi,
    private val taxCodeDao: TaxCodeDao,
    private val taxRuleDao: TaxRuleDao,
    private val taxComponentDao: TaxComponentDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    private val tag = "TaxSyncDelegate"

    override val entity: SyncEntity = SyncEntity.TAX

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        push().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    // --- Pull -----------------------------------------------------------------------------------

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val checkpoint = syncStateDao.getLastSyncedAtIso(SyncEntity.TAX)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }

            var total = 0
            var maxSeen: Instant? = checkpoint

            // Codes
            maxSeen = pullPaged(
                batchSize = batchSize,
                maxSeen = maxSeen,
                fetch = { page -> api.getTaxCodes(checkpoint, page, batchSize).getOrThrow() },
                updatedAt = { it.updatedAt },
                reconcile = { code ->
                    val existing = taxCodeDao.getById(code.id)
                    when {
                        existing != null && existing.syncStatus != "SYNCED" -> Unit // local unsynced wins
                        !code.isActive -> taxCodeDao.delete(code.id)
                        else -> taxCodeDao.insert(code.toEntity().copy(syncStatus = "SYNCED"))
                    }
                    total++
                },
            )

            // Rules
            maxSeen = pullPaged(
                batchSize = batchSize,
                maxSeen = maxSeen,
                fetch = { page -> api.getTaxRules(checkpoint, page, batchSize).getOrThrow() },
                updatedAt = { it.updatedAt },
                reconcile = { rule ->
                    val existing = taxRuleDao.getById(rule.id)
                    when {
                        existing != null && existing.syncStatus != "SYNCED" -> Unit
                        !rule.isActive -> taxRuleDao.delete(rule.id)
                        else -> taxRuleDao.insert(rule.toEntity().copy(syncStatus = "SYNCED"))
                    }
                    total++
                },
            )

            // Components
            maxSeen = pullPaged(
                batchSize = batchSize,
                maxSeen = maxSeen,
                fetch = { page -> api.getWorkspaceComponents(checkpoint, page, batchSize).getOrThrow() },
                updatedAt = { it.updatedAt },
                reconcile = { comp ->
                    val existing = taxComponentDao.getById(comp.id)
                    when {
                        existing != null && existing.syncStatus != "SYNCED" -> Unit
                        !comp.isActive -> taxComponentDao.delete(comp.id)
                        else -> taxComponentDao.insert(comp.toEntity().copy(syncStatus = "SYNCED"))
                    }
                    total++
                },
            )

            if (maxSeen != null && maxSeen != checkpoint) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.TAX, maxSeen.toString())
            }
            Result.success(total)
        } catch (e: Exception) {
            TaxLogger.e(tag, "Pull failed", e)
            Result.failure(e)
        }
    }

    /** Page through one feed, reconciling each row and tracking the max updatedAt seen. */
    private suspend fun <T> pullPaged(
        batchSize: Int,
        maxSeen: Instant?,
        fetch: suspend (page: Int) -> PageResponse<T>,
        updatedAt: (T) -> Instant,
        reconcile: suspend (T) -> Unit,
    ): Instant? {
        var page = 0
        var localMax = maxSeen
        var pulled = 0
        while (pulled < MAX_PER_CYCLE) {
            val response = fetch(page)
            for (item in response.content) {
                reconcile(item)
                val ts = updatedAt(item)
                val current = localMax
                if (current == null || ts > current) localMax = ts
            }
            pulled += response.content.size
            if (!response.hasNext || response.content.isEmpty()) break
            page++
        }
        return localMax
    }

    // --- Push -----------------------------------------------------------------------------------

    private suspend fun push(): Result<Int> {
        return try {
            val pending = taxCodeDao.getUnsyncedCodes()
            if (pending.isEmpty()) return Result.success(0)

            var pushed = 0
            var failed = 0

            for (code in pending) {
                try {
                    if (!code.isActive) {
                        // Queued unsubscribe — confirm on server, then remove locally.
                        if (api.unsubscribeFromTaxCode(code.id).isSuccess) {
                            taxCodeDao.delete(code.id)
                            pushed++
                        } else {
                            failed++
                        }
                    } else {
                        // Favorite / notes edit — persist config.
                        val result = api.updateTaxCodeConfig(
                            taxCodeId = code.id,
                            isFavorite = code.isFavorite,
                            notes = code.notes,
                            customName = code.customName,
                        )
                        if (result.isSuccess) {
                            taxCodeDao.updateSyncStatus(code.id, "SYNCED")
                            pushed++
                        } else {
                            failed++
                        }
                    }
                } catch (e: Exception) {
                    TaxLogger.w(tag, "Push failed for tax code ${code.id}", e)
                    failed++
                }
            }

            if (pushed == 0 && failed > 0) {
                Result.failure(Exception("$failed tax code change(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(pushed)
            }
        } catch (e: Exception) {
            TaxLogger.e(tag, "Push failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val MAX_PER_CYCLE = 10_000
    }
}
