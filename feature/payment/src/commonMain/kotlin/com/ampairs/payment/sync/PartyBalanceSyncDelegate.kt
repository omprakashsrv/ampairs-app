package com.ampairs.payment.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.payment.data.api.PaymentApi
import com.ampairs.payment.data.api.toApi
import com.ampairs.payment.data.api.toEntity
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns party-balance ↔ server traffic (spec 013). [SyncEntity.PARTY_BALANCE] is **pull-authoritative**:
 * the server's `cached_closing_balance` wins on pull (clients recompute locally between syncs for a
 * live UI). Push carries only **opening-balance edits** (unsynced rows); the server recomputes and
 * returns the authoritative closing.
 *
 * Pulled last — it depends on every ledger-affecting entity so the server's recomputed balance lands
 * after the rows that produced it.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PARTY_BALANCE)
class PartyBalanceSyncDelegate(
    private val api: PaymentApi,
    private val partyBalanceDao: PartyBalanceDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PARTY_BALANCE

    override val dependsOn: List<SyncEntity> = listOf(
        SyncEntity.CUSTOMER,
        SyncEntity.INVOICE,
        SyncEntity.LEDGER_ENTRY,
        SyncEntity.PAYMENT_VOUCHER,
        SyncEntity.ADJUSTMENT,
        SyncEntity.PAYMENT_ALLOCATION,
    )

    override suspend fun pullFromServer(): SyncResult =
        pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    private suspend fun pushPending(): Result<Int> = try {
        val unsynced = partyBalanceDao.getUnsynced()
        if (unsynced.isEmpty()) return Result.success(0)
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                val response = api.pushPartyBalances(batch.map { it.toApi() })
                // Apply the server's authoritative closing back to the local cache.
                response.forEach { partyBalanceDao.insert(it.toEntity(synced = true)) }
                batch.forEach { partyBalanceDao.markSynced(it.uid) }
                synced += batch.size
            } catch (e: Exception) {
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) {
            Result.failure(Exception("$failed party balance(s) failed to push — will retry on reconnect"))
        } else {
            Result.success(synced)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = try {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PARTY_BALANCE) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        var hasNext: Boolean
        do {
            val pageResponse = api.getPartyBalancesSync(lastSync, page, batchSize)
            val content = pageResponse.content
            for (api in content) {
                val existing = partyBalanceDao.getByUid(api.uid)
                when {
                    // Local unsynced opening edit wins; everything else (incl. server closing) is authoritative.
                    existing != null && existing.synced == 0L -> { /* keep local edit */ }
                    !api.active -> partyBalanceDao.deleteByUid(api.uid)
                    else -> partyBalanceDao.insert(api.toEntity(synced = true))
                }
            }
            val batchMax = content.mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }.maxOrNull() ?: ""
            if (batchMax > maxTime) maxTime = batchMax
            total += content.size
            page++
            hasNext = pageResponse.hasNext
        } while (hasNext && total < 10000)

        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.PARTY_BALANCE, maxTime)
        Result.success(total)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
