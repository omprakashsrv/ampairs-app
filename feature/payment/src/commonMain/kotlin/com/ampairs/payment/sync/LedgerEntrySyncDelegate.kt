package com.ampairs.payment.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.payment.data.api.PaymentApi
import com.ampairs.payment.data.api.toApi
import com.ampairs.payment.data.api.toEntity
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.repository.PaymentLedgerPoster
import com.ampairs.payment.domain.InvoiceLedgerPoster
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all ledger-entry ↔ server traffic (spec 013). Ledger entries reference customers and the
 * invoice they derive from, so they pull/push after those (`dependsOn`).
 *
 * On pull, the server's row wins unless a local unsynced edit exists; server-DELETED rows are
 * hard-deleted; after a pull the affected balances are recomputed locally so the cache stays live.
 * Before pushing, it backfills `LDG_<invoice.uid>` for any local finalized invoice missing one (R9).
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.LEDGER_ENTRY)
class LedgerEntrySyncDelegate(
    private val api: PaymentApi,
    private val ledgerEntryDao: LedgerEntryDao,
    private val partyBalanceDao: PartyBalanceDao,
    private val poster: PaymentLedgerPoster,
    private val invoiceLedgerPoster: InvoiceLedgerPoster,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.LEDGER_ENTRY

    // pushDependencies so the PUSH (not just pull) sends parents first — a ledger entry references a
    // customer (party) and its source invoice/order, which the server expects to already exist.
    override val pushDependencies: List<SyncEntity> =
        listOf(SyncEntity.CUSTOMER, SyncEntity.INVOICE, SyncEntity.ORDER)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    private suspend fun pushPending(): Result<Int> = try {
        invoiceLedgerPoster.backfillFinalizedInvoices()
        val unsynced = ledgerEntryDao.getUnsynced()
        if (unsynced.isEmpty()) return Result.success(0)
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.pushLedgerEntries(batch.map { it.toApi() })
                batch.forEach { e ->
                    if (e.active == 1L) ledgerEntryDao.markSynced(e.uid)
                    else ledgerEntryDao.deleteByUid(e.uid) // confirmed on server → drop locally
                }
                synced += batch.size
            } catch (e: Exception) {
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) {
            Result.failure(Exception("$failed ledger entr(ies) failed to push — will retry on reconnect"))
        } else {
            Result.success(synced)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = try {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.LEDGER_ENTRY) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        val affectedParties = mutableSetOf<String>()
        var hasNext: Boolean
        do {
            val pageResponse = api.getLedgerEntriesSync(lastSync, page, batchSize)
            val content = pageResponse.content
            for (api in content) {
                affectedParties += api.partyUid
                val existing = ledgerEntryDao.getByUid(api.uid)
                when {
                    existing != null && existing.synced == 0L -> { /* local unsynced wins */ }
                    !api.active -> ledgerEntryDao.deleteByUid(api.uid)
                    else -> ledgerEntryDao.insert(api.toEntity(synced = true))
                }
            }
            val batchMax = content.mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }.maxOrNull() ?: ""
            if (batchMax > maxTime) maxTime = batchMax
            total += content.size
            page++
            hasNext = pageResponse.hasNext
        } while (hasNext && total < 10000)

        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.LEDGER_ENTRY, maxTime)
        // Keep the local cache live after pulling new entries.
        affectedParties.forEach { poster.recomputeAndCache(it) }
        Result.success(total)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
