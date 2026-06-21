package com.ampairs.payment.data.repository

import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.db.toDomain
import com.ampairs.payment.domain.LedgerEntry
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read-side access to ledger entries (the statement / running balance). Writes go through
 * [PaymentLedgerPoster] (which posts + recomputes); this repository never talks to the network.
 */
@Inject
class LedgerEntryRepository(
    private val ledgerEntryDao: LedgerEntryDao,
) {
    fun observeByParty(partyUid: String): Flow<List<LedgerEntry>> =
        ledgerEntryDao.observeActiveByParty(partyUid).map { list -> list.map { it.toDomain() } }

    suspend fun getByParty(partyUid: String): List<LedgerEntry> =
        ledgerEntryDao.getActiveByParty(partyUid).map { it.toDomain() }
}
