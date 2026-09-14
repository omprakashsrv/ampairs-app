package com.ampairs.payment.data.repository

import com.ampairs.payment.data.db.dao.PaymentAllocationDao
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity
import com.ampairs.payment.data.db.toDomain
import com.ampairs.payment.data.db.toEntity
import com.ampairs.payment.domain.ClearanceStatus
import com.ampairs.payment.domain.LedgerSourceType
import com.ampairs.payment.domain.Money
import com.ampairs.payment.domain.PaymentAllocation
import com.ampairs.payment.domain.PaymentVoucher
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only payment-voucher store (US1/US6, spec 013). Saving a voucher posts its
 * `PAYMENT_IN`(CR) / `PAYMENT_OUT`(DR) ledger entry via [PaymentLedgerPoster] in the same flow, so
 * the on-device balance updates immediately and offline. Allocations drive open-bills/aging only and
 * never touch the balance (R8). Deletes soft-delete + reverse — never hard delete (FR-023).
 */
@OptIn(ExperimentalTime::class)
@Inject
class PaymentVoucherRepository(
    private val voucherDao: PaymentVoucherDao,
    private val allocationDao: PaymentAllocationDao,
    private val poster: PaymentLedgerPoster,
    private val syncStateDao: SyncStateDao,
) {

    fun observeByParty(partyUid: String): Flow<List<PaymentVoucher>> =
        voucherDao.observeByParty(partyUid).map { list -> list.map { it.toDomain() } }

    fun observePending(): Flow<List<PaymentVoucher>> =
        voucherDao.observePending().map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<PaymentVoucher>> =
        voucherDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getByUid(uid: String): PaymentVoucher? = voucherDao.getByUid(uid)?.toDomain()

    suspend fun getAllocations(voucherUid: String): List<PaymentAllocation> =
        allocationDao.getByVoucher(voucherUid).map { it.toDomain() }

    /**
     * Save (create or edit) a voucher with its allocations (FR-007/010/011/012). The voucher uid and
     * each allocation uid are generated in the ViewModel. Posts/updates the ledger entry and
     * recomputes the balance. Unallocated remainder is retained as on-account (R8).
     */
    suspend fun save(voucher: PaymentVoucher, allocations: List<PaymentAllocation>): Result<PaymentVoucher> {
        require(voucher.uid.isNotBlank()) { "UID must be set by ViewModel" }
        require(voucher.totalAmount.isPositive) { "Amount must be > 0" }
        val allocatedSum = allocations.filter { it.active }.fold(Money.ZERO) { acc, a -> acc + a.amount }
        require(allocatedSum <= voucher.totalAmount) { "Allocated amount exceeds voucher total" }

        val withRemainder = voucher.copy(
            unallocatedAmount = voucher.totalAmount - allocatedSum,
            active = true,
        )
        voucherDao.insert(preserveTallyRef(withRemainder.toEntity(synced = false)))

        // Replace allocations: soft-delete any existing not in the new set, then upsert the new set.
        val existing = allocationDao.getByVoucher(voucher.uid)
        val keepUids = allocations.map { it.uid }.toSet()
        existing.filter { it.uid !in keepUids }.forEach {
            allocationDao.insert(it.copy(active = 0, synced = 0))
        }
        allocations.forEach { allocationDao.insert(it.copy(paymentVoucherUid = voucher.uid).toEntity(synced = false)) }

        // Post the money movement to the ledger (RECEIVED ⇒ PAYMENT_IN CR, PAID ⇒ PAYMENT_OUT DR).
        poster.postDocumentEntry(
            partyUid = voucher.partyUid,
            sourceType = LedgerSourceType.PAYMENT,
            sourceUid = voucher.uid,
            entryType = withRemainder.ledgerEntryType,
            direction = withRemainder.ledgerEntryType.naturalDirection,
            amount = withRemainder.totalAmount,
            entryDate = withRemainder.voucherDate,
            voucherNo = withRemainder.voucherNo,
            narration = withRemainder.narration,
        )
        markVoucherPending()
        markAllocationPending()
        return Result.success(withRemainder)
    }

    /** Soft-delete a voucher: reverse its ledger entry, drop its allocations, recompute (FR-023). */
    suspend fun delete(uid: String): Result<Unit> {
        val existing = voucherDao.getByUid(uid) ?: return Result.success(Unit)
        voucherDao.insert(existing.copy(active = 0, synced = 0))
        allocationDao.getByVoucher(uid).forEach { allocationDao.insert(it.copy(active = 0, synced = 0)) }
        poster.reverseDocumentEntry(uid)
        markVoucherPending()
        markAllocationPending()
        return Result.success(Unit)
    }

    /** Mark a pending cheque/online receipt as CLEARED (US6). No-op if terminal. */
    suspend fun markCleared(uid: String): Result<PaymentVoucher> {
        val existing = voucherDao.getByUid(uid)?.toDomain() ?: return Result.failure(Exception("Voucher not found"))
        if (existing.isTerminal) return Result.failure(Exception("Voucher already ${existing.clearanceStatus}"))
        val updated = existing.copy(clearanceStatus = ClearanceStatus.CLEARED)
        voucherDao.insert(preserveTallyRef(updated.toEntity(synced = false)))
        markVoucherPending()
        return Result.success(updated)
    }

    /**
     * Mark a pending receipt as BOUNCED (US6): post a contra reversal entry restoring the dues; the
     * original and reversal both stay visible. [reversalUid] is generated in the ViewModel.
     */
    suspend fun markBounced(uid: String, reversalUid: String, narration: String?): Result<PaymentVoucher> {
        val existing = voucherDao.getByUid(uid)?.toDomain() ?: return Result.failure(Exception("Voucher not found"))
        if (existing.isTerminal) return Result.failure(Exception("Voucher already ${existing.clearanceStatus}"))
        val updated = existing.copy(clearanceStatus = ClearanceStatus.BOUNCED)
        voucherDao.insert(preserveTallyRef(updated.toEntity(synced = false)))
        poster.postReversalEntry(
            partyUid = existing.partyUid,
            sourceUid = existing.uid,
            reversalUid = reversalUid,
            originalDirection = existing.ledgerEntryType.naturalDirection,
            amount = existing.totalAmount,
            entryDate = Clock.System.now().toString(),
            voucherNo = existing.voucherNo,
            narration = narration ?: "Bounced",
        )
        markVoucherPending()
        return Result.success(updated)
    }

    /** The Tally voucher reference captured by TallyPaymentPushService, if this voucher was pushed. */
    suspend fun getTallyRef(uid: String): String? = voucherDao.getByUid(uid)?.ref_id

    /**
     * Preserve an already-captured Tally reference across local edits: [PaymentVoucher.toEntity]
     * (domain → entity) doesn't carry ref_id, so a plain save/markCleared/markBounced would otherwise
     * null it and let the Tally push re-send the voucher as a duplicate (mirrors
     * InvoiceRepository.saveInvoice's identical guard).
     */
    private suspend fun preserveTallyRef(entity: PaymentVoucherEntity): PaymentVoucherEntity {
        if (!entity.ref_id.isNullOrBlank()) return entity
        val previousRef = voucherDao.getByUid(entity.uid)?.ref_id
        return if (previousRef.isNullOrBlank()) entity else entity.copy(ref_id = previousRef)
    }

    private suspend fun markVoucherPending() =
        syncStateDao.markPendingPush(SyncEntity.PAYMENT_VOUCHER, Clock.System.now().toEpochMilliseconds())

    private suspend fun markAllocationPending() =
        syncStateDao.markPendingPush(SyncEntity.PAYMENT_ALLOCATION, Clock.System.now().toEpochMilliseconds())
}
