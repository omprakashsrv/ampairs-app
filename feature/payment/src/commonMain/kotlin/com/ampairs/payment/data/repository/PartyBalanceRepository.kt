package com.ampairs.payment.data.repository

import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.db.toDomain
import com.ampairs.payment.domain.BalanceMath
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.domain.Money
import com.ampairs.payment.domain.PartyBalance
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only party-balance store (spec 013, offline-first). The opening balance is the only
 * editable field; the cached closing balance is **derived** via [BalanceMath.recompute] from active
 * ledger entries and refreshed on every posting (see [PaymentLedgerPoster.recompute]).
 *
 * [PartyBalance] is **pull-authoritative**: on pull the server's `cachedClosingBalance` wins; between
 * syncs we recompute locally for a live UI (the formula is identical to the backend, R3/R4).
 *
 * Opening-balance edits push via `/party-balances/sync` (`PartyBalanceSyncDelegate`).
 */
@OptIn(ExperimentalTime::class)
@Inject
class PartyBalanceRepository(
    private val partyBalanceDao: PartyBalanceDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val syncStateDao: SyncStateDao,
) {

    fun observeBalance(partyUid: String): Flow<PartyBalance?> =
        partyBalanceDao.observeByParty(partyUid).map { it?.toDomain() }

    fun observeAll(): Flow<List<PartyBalance>> =
        partyBalanceDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getBalance(partyUid: String): PartyBalance? =
        partyBalanceDao.getByParty(partyUid)?.toDomain()

    /** Ensure a [PartyBalance] row exists for a party (used before first posting / opening edit). */
    suspend fun ensureBalance(partyUid: String, uidFactory: () -> String): PartyBalance {
        partyBalanceDao.getByParty(partyUid)?.let { return it.toDomain() }
        val now = nowIso()
        val balance = PartyBalance(
            uid = uidFactory(),
            partyUid = partyUid,
            openingAsOf = now,
            lastComputedAt = now,
        )
        partyBalanceDao.insert(
            com.ampairs.payment.data.db.run { balance.toEntity(synced = true) },
        )
        return balance
    }

    /**
     * Set / edit the opening balance (US3). Recomputes and caches closing, then marks PENDING_PUSH.
     * The [uid] must be supplied by the ViewModel for a brand-new balance row.
     */
    suspend fun setOpeningBalance(
        partyUid: String,
        uid: String,
        openingBalance: Money,
        openingDirection: Direction,
        openingAsOf: String,
    ): Result<PartyBalance> {
        require(openingBalance.minor >= 0L) { "Opening balance must be ≥ 0" }
        val existing = partyBalanceDao.getByParty(partyUid)
        val rowUid = existing?.uid ?: uid.also { require(it.isNotBlank()) { "UID must be set by ViewModel" } }
        val entries = ledgerEntryDao.getActiveByParty(partyUid).map { it.toDomain() }
        val openingSigned = openingBalance.signed(openingDirection)
        val closing = BalanceMath.recompute(openingSigned, entries)
        val now = nowIso()
        val balance = PartyBalance(
            uid = rowUid,
            partyUid = partyUid,
            openingBalance = openingBalance,
            openingDirection = openingDirection,
            openingAsOf = openingAsOf,
            cachedClosingBalance = closing,
            lastComputedAt = now,
            active = true,
        )
        partyBalanceDao.insert(com.ampairs.payment.data.db.run { balance.toEntity(synced = false) })
        markPending()
        return Result.success(balance)
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.PARTY_BALANCE, Clock.System.now().toEpochMilliseconds())

    private fun nowIso(): String = Clock.System.now().toString()
}
