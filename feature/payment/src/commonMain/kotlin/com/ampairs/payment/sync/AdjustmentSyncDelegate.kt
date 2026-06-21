package com.ampairs.payment.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.payment.data.api.PaymentApi
import com.ampairs.payment.data.api.toApi
import com.ampairs.payment.data.api.toEntity
import com.ampairs.payment.data.db.dao.AdjustmentVoucherDao
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns adjustment-voucher ↔ server traffic (spec 013, US5). Adjustments carry their own
 * `LDG_<adjustment.uid>` ledger entry (synced by [LedgerEntrySyncDelegate]); the header pushes after.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.ADJUSTMENT)
class AdjustmentSyncDelegate(
    private val api: PaymentApi,
    private val adjustmentDao: AdjustmentVoucherDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.ADJUSTMENT

    // pushDependencies so the PUSH orders parents first (customer + the adjustment's ledger entry).
    override val pushDependencies: List<SyncEntity> =
        listOf(SyncEntity.CUSTOMER, SyncEntity.LEDGER_ENTRY)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    private suspend fun pushPending(): Result<Int> = try {
        val unsynced = adjustmentDao.getUnsynced()
        if (unsynced.isEmpty()) return Result.success(0)
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.pushAdjustments(batch.map { it.toApi() })
                batch.forEach { a ->
                    if (a.active == 1L) adjustmentDao.markSynced(a.uid) else adjustmentDao.deleteByUid(a.uid)
                }
                synced += batch.size
            } catch (e: Exception) {
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) {
            Result.failure(Exception("$failed adjustment(s) failed to push — will retry on reconnect"))
        } else {
            Result.success(synced)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = try {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.ADJUSTMENT) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        var hasNext: Boolean
        do {
            val pageResponse = api.getAdjustmentsSync(lastSync, page, batchSize)
            val content = pageResponse.content
            for (api in content) {
                val existing = adjustmentDao.getByUid(api.uid)
                when {
                    existing != null && existing.synced == 0L -> { /* local unsynced wins */ }
                    !api.active -> adjustmentDao.deleteByUid(api.uid)
                    else -> adjustmentDao.insert(api.toEntity(synced = true))
                }
            }
            val batchMax = content.mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }.maxOrNull() ?: ""
            if (batchMax > maxTime) maxTime = batchMax
            total += content.size
            page++
            hasNext = pageResponse.hasNext
        } while (hasNext && total < 10000)

        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.ADJUSTMENT, maxTime)
        Result.success(total)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
