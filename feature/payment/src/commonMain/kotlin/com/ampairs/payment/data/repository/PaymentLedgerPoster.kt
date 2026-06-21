package com.ampairs.payment.data.repository

import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.db.toDomain
import com.ampairs.payment.data.db.toEntity
import com.ampairs.payment.domain.BalanceMath
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.domain.EntryType
import com.ampairs.payment.domain.LedgerEntry
import com.ampairs.payment.domain.LedgerSourceType
import com.ampairs.payment.domain.Money
import com.ampairs.payment.domain.PartyBalance
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Shared posting engine (spec 013 R4): whoever authors a source document (invoice / voucher /
 * adjustment) posts its corresponding [LedgerEntry] in the **same local write**, then recomputes and
 * caches the party's closing balance. Document-derived entries use the deterministic uid
 * `LDG_<sourceUid>` so concurrent offline clients can never duplicate (data-model.md).
 *
 * This keeps every write offline-first by construction; the [com.ampairs.payment.sync] delegates
 * push these rows. Corrections are made by re-posting the same uid (edit) or a reversal entry +
 * soft-delete — never a hard delete (FR-023).
 */
@OptIn(ExperimentalTime::class)
@Inject
class PaymentLedgerPoster(
    private val ledgerEntryDao: LedgerEntryDao,
    private val partyBalanceDao: PartyBalanceDao,
    private val syncStateDao: SyncStateDao,
) {

    fun ledgerUidFor(sourceUid: String): String = "LDG_$sourceUid"

    /**
     * Upsert a document-derived ledger entry (uid = `LDG_<sourceUid>`) and recompute the balance.
     * Used by invoice-finalize, voucher save, and adjustment save.
     */
    suspend fun postDocumentEntry(
        partyUid: String,
        sourceType: LedgerSourceType,
        sourceUid: String,
        entryType: EntryType,
        direction: Direction,
        amount: Money,
        entryDate: String,
        voucherNo: String = "",
        narration: String? = null,
    ): LedgerEntry {
        val entry = LedgerEntry(
            uid = ledgerUidFor(sourceUid),
            partyUid = partyUid,
            entryDate = entryDate,
            entryType = entryType,
            direction = direction,
            amount = amount.abs(),
            sourceType = sourceType,
            sourceUid = sourceUid,
            voucherNo = voucherNo,
            narration = narration,
            active = true,
        )
        ledgerEntryDao.insert(entry.toEntity(synced = false))
        recomputeAndCache(partyUid)
        markLedgerPending()
        return entry
    }

    /**
     * Soft-delete a document's ledger entry (active = false, synced = false) and recompute.
     * Used when a voucher/adjustment/invoice is cancelled — keeps the audit trail (FR-023).
     */
    suspend fun reverseDocumentEntry(sourceUid: String) {
        val existing = ledgerEntryDao.getByUid(ledgerUidFor(sourceUid)) ?: return
        ledgerEntryDao.insert(existing.copy(active = 0, synced = 0))
        recomputeAndCache(existing.party_uid)
        markLedgerPending()
    }

    /**
     * Post a contra reversal entry that restores the dues (US6 cheque bounce). A new entry with the
     * opposite direction and `reversalOf = LDG_<sourceUid>`; the original stays active and visible.
     */
    suspend fun postReversalEntry(
        partyUid: String,
        sourceUid: String,
        reversalUid: String,
        originalDirection: Direction,
        amount: Money,
        entryDate: String,
        voucherNo: String = "",
        narration: String? = null,
    ): LedgerEntry {
        val contra = LedgerEntry(
            uid = reversalUid,
            partyUid = partyUid,
            entryDate = entryDate,
            entryType = EntryType.ADJUSTMENT,
            direction = if (originalDirection == Direction.DR) Direction.CR else Direction.DR,
            amount = amount.abs(),
            sourceType = LedgerSourceType.PAYMENT,
            sourceUid = sourceUid,
            voucherNo = voucherNo,
            narration = narration,
            reversalOf = ledgerUidFor(sourceUid),
            active = true,
        )
        ledgerEntryDao.insert(contra.toEntity(synced = false))
        // Mark the original as reversed (audit), but keep it active.
        ledgerEntryDao.getByUid(ledgerUidFor(sourceUid))?.let {
            ledgerEntryDao.insert(it.copy(reversed = 1, synced = 0))
        }
        recomputeAndCache(partyUid)
        markLedgerPending()
        return contra
    }

    /** Recompute the signed closing balance from active entries and write it to the cache row. */
    suspend fun recomputeAndCache(partyUid: String): Money {
        val entries = ledgerEntryDao.getActiveByParty(partyUid).map { it.toDomain() }
        val balanceRow = partyBalanceDao.getByParty(partyUid)
        val openingSigned = balanceRow?.toDomain()?.openingSigned ?: Money.ZERO
        val closing = BalanceMath.recompute(openingSigned, entries)
        val now = Clock.System.now().toString()
        if (balanceRow == null) {
            // Auto-create a balance row so the badge/statement have something to read.
            val created = PartyBalance(
                uid = "PBL_$partyUid",
                partyUid = partyUid,
                cachedClosingBalance = closing,
                lastComputedAt = now,
                openingAsOf = now,
            )
            partyBalanceDao.insert(created.toEntity(synced = false))
            syncStateDao.markPendingPush(SyncEntity.PARTY_BALANCE, Clock.System.now().toEpochMilliseconds())
        } else {
            partyBalanceDao.updateCachedClosing(partyUid, closing.minor, now)
        }
        return closing
    }

    private suspend fun markLedgerPending() =
        syncStateDao.markPendingPush(SyncEntity.LEDGER_ENTRY, Clock.System.now().toEpochMilliseconds())
}
