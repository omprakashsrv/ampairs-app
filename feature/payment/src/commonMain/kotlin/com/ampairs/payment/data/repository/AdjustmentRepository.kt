package com.ampairs.payment.data.repository

import com.ampairs.payment.data.db.dao.AdjustmentVoucherDao
import com.ampairs.payment.data.db.toDomain
import com.ampairs.payment.data.db.toEntity
import com.ampairs.payment.domain.AdjustmentVoucher
import com.ampairs.payment.domain.LedgerSourceType
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only adjustment-voucher store (US5, spec 013): sales return / credit note / purchase /
 * purchase-return / debit note / discount / write-off. Saving posts a signed ledger entry of the
 * mapped type ([AdjustmentVoucher.adjustmentType] → EntryType+Direction) and recomputes the balance.
 */
@OptIn(ExperimentalTime::class)
@Inject
class AdjustmentRepository(
    private val adjustmentDao: AdjustmentVoucherDao,
    private val poster: PaymentLedgerPoster,
    private val syncStateDao: SyncStateDao,
) {

    fun observeByParty(partyUid: String): Flow<List<AdjustmentVoucher>> =
        adjustmentDao.observeByParty(partyUid).map { list -> list.map { it.toDomain() } }

    suspend fun getByUid(uid: String): AdjustmentVoucher? = adjustmentDao.getByUid(uid)?.toDomain()

    suspend fun save(adjustment: AdjustmentVoucher): Result<AdjustmentVoucher> {
        require(adjustment.uid.isNotBlank()) { "UID must be set by ViewModel" }
        require(adjustment.amount.isPositive) { "Amount must be > 0" }
        adjustmentDao.insert(adjustment.copy(active = true).toEntity(synced = false))
        poster.postDocumentEntry(
            partyUid = adjustment.partyUid,
            sourceType = LedgerSourceType.ADJUSTMENT,
            sourceUid = adjustment.uid,
            entryType = adjustment.adjustmentType.entryType,
            direction = adjustment.adjustmentType.direction,
            amount = adjustment.amount,
            entryDate = adjustment.voucherDate,
            voucherNo = adjustment.voucherNo,
            narration = adjustment.narration,
        )
        markPending()
        return Result.success(adjustment)
    }

    suspend fun delete(uid: String): Result<Unit> {
        val existing = adjustmentDao.getByUid(uid) ?: return Result.success(Unit)
        adjustmentDao.insert(existing.copy(active = 0, synced = 0))
        poster.reverseDocumentEntry(uid)
        markPending()
        return Result.success(Unit)
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.ADJUSTMENT, Clock.System.now().toEpochMilliseconds())
}
