package com.ampairs.payment.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.payment.data.api.PaymentApi
import com.ampairs.payment.data.api.toApi
import com.ampairs.payment.data.api.toEntity
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns payment-voucher ↔ server traffic (spec 013). Vouchers carry their own `LDG_<voucher.uid>`
 * ledger entry (synced by [LedgerEntrySyncDelegate]); the voucher header pushes after the ledger so
 * the server already has the posting it summarises.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PAYMENT_VOUCHER)
class PaymentVoucherSyncDelegate(
    private val api: PaymentApi,
    private val voucherDao: PaymentVoucherDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PAYMENT_VOUCHER

    override val dependsOn: List<SyncEntity> =
        listOf(SyncEntity.CUSTOMER, SyncEntity.INVOICE, SyncEntity.LEDGER_ENTRY)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    private suspend fun pushPending(): Result<Int> = try {
        val unsynced = voucherDao.getUnsynced()
        if (unsynced.isEmpty()) return Result.success(0)
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.pushVouchers(batch.map { it.toApi() })
                batch.forEach { v ->
                    if (v.active == 1L) voucherDao.markSynced(v.uid) else voucherDao.deleteByUid(v.uid)
                }
                synced += batch.size
            } catch (e: Exception) {
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) {
            Result.failure(Exception("$failed voucher(s) failed to push — will retry on reconnect"))
        } else {
            Result.success(synced)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = try {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PAYMENT_VOUCHER) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        var hasNext: Boolean
        do {
            val pageResponse = api.getVouchersSync(lastSync, page, batchSize)
            val content = pageResponse.content
            for (api in content) {
                val existing = voucherDao.getByUid(api.uid)
                when {
                    existing != null && existing.synced == 0L -> { /* local unsynced wins */ }
                    !api.active -> voucherDao.deleteByUid(api.uid)
                    else -> voucherDao.insert(api.toEntity(synced = true))
                }
            }
            val batchMax = content.mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }.maxOrNull() ?: ""
            if (batchMax > maxTime) maxTime = batchMax
            total += content.size
            page++
            hasNext = pageResponse.hasNext
        } while (hasNext && total < 10000)

        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.PAYMENT_VOUCHER, maxTime)
        Result.success(total)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
