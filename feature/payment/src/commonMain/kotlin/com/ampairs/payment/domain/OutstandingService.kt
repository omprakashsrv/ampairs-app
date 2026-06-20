package com.ampairs.payment.domain

import com.ampairs.customer.data.CustomerDataService
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.payment.data.db.dao.PartyBalanceDao
import com.ampairs.payment.data.db.dao.PaymentAllocationDao
import com.ampairs.payment.data.db.toDomain
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Aging summary (US4 / FR-018) — totals + bucketed receivables + over-credit-limit parties. */
data class AgingSummary(
    val totalReceivable: Money,
    val totalPayable: Money,
    val buckets: List<AgingBucket>,
    val partiesOverCreditLimit: List<OverCreditLimitParty>,
)

data class AgingBucket(val label: String, val amount: Money)

data class OverCreditLimitParty(
    val partyUid: String,
    val partyName: String,
    val balance: Money,
    val creditLimit: Money,
)

/**
 * Local computation of open bills and aging (US4, spec 013 contracts/payment-actions.md). Open bill
 * outstanding = `invoice.total − Σ active allocations to it`; due date = `invoiceDate + creditDays`;
 * aging buckets come from the workspace `agingBuckets` setting. All local — no network.
 */
@OptIn(ExperimentalTime::class)
@Inject
class OutstandingService(
    private val invoiceDao: InvoiceDao,
    private val allocationDao: PaymentAllocationDao,
    private val partyBalanceDao: PartyBalanceDao,
    private val customerDataService: CustomerDataService,
) {

    private val finalizedStatus = "INVOICEED" // matches InvoiceStatus.INVOICEED (codebase spelling)

    /** Open (unpaid) bills for a party, classified into aging buckets. */
    suspend fun openBills(partyUid: String, bucketBoundaries: List<Int> = DEFAULT_BUCKETS): List<OpenBill> {
        val invoices = invoiceDao.getInvoicesByCustomer(partyUid)
            .filter { it.status.equals(finalizedStatus, ignoreCase = true) }
        val creditDays = customerDataService.getById(partyUid)?.creditDays ?: 0
        val now = Clock.System.now()
        return invoices.mapNotNull { inv ->
            val total = Money.fromDouble(inv.total_cost)
            val allocated = Money(allocationDao.sumAllocatedToTarget("INVOICE", inv.id))
            val outstanding = total - allocated
            if (!outstanding.isPositive) return@mapNotNull null
            val billDate = parseIsoOrNull(inv.invoice_date)
            val daysOverdue = billDate?.let { daysBetween(it, now) - creditDays }?.coerceAtLeast(0) ?: 0
            OpenBill(
                billUid = inv.id,
                billNo = inv.invoice_number,
                billDate = inv.invoice_date,
                total = total,
                allocated = allocated,
                outstanding = outstanding,
                dueDate = billDate?.let { plusDaysIso(it, creditDays) },
                daysOverdue = daysOverdue,
                agingBucket = bucketLabel(daysOverdue, bucketBoundaries),
            )
        }
    }

    /** Workspace-wide aging summary across all parties with a balance (US4). */
    suspend fun agingSummary(bucketBoundaries: List<Int> = DEFAULT_BUCKETS): AgingSummary {
        val balances = partyBalanceDao.getAll().map { it.toDomain() }
        var totalReceivable = Money.ZERO
        var totalPayable = Money.ZERO
        val bucketSums = LinkedHashMap<String, Long>()
        bucketLabels(bucketBoundaries).forEach { bucketSums[it] = 0L }
        val overLimit = mutableListOf<OverCreditLimitParty>()

        for (balance in balances) {
            val closing = balance.cachedClosingBalance
            if (closing.isPositive) totalReceivable += closing
            if (closing.isNegative) totalPayable += closing.abs()

            // Bucket the receivable by its oldest open bill's age.
            if (closing.isPositive) {
                val bills = openBills(balance.partyUid, bucketBoundaries)
                bills.forEach { bill ->
                    bucketSums[bill.agingBucket] = (bucketSums[bill.agingBucket] ?: 0L) + bill.outstanding.minor
                }
                val customer = customerDataService.getById(balance.partyUid)
                val creditLimit = Money.fromDouble(customer?.creditLimit)
                if (creditLimit.isPositive && closing > creditLimit) {
                    overLimit += OverCreditLimitParty(
                        partyUid = balance.partyUid,
                        partyName = customer?.name ?: balance.partyUid,
                        balance = closing,
                        creditLimit = creditLimit,
                    )
                }
            }
        }
        return AgingSummary(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            buckets = bucketSums.map { AgingBucket(it.key, Money(it.value)) },
            partiesOverCreditLimit = overLimit,
        )
    }

    /** True when the party's receivable exceeds its configured credit limit (FR-019). */
    suspend fun isOverCreditLimit(partyUid: String): Boolean {
        val balance = partyBalanceDao.getByParty(partyUid)?.toDomain() ?: return false
        val limit = Money.fromDouble(customerDataService.getById(partyUid)?.creditLimit)
        return limit.isPositive && balance.cachedClosingBalance > limit
    }

    private fun bucketLabel(daysOverdue: Int, boundaries: List<Int>): String {
        val sorted = boundaries.sorted()
        var lower = 0
        for (b in sorted) {
            if (daysOverdue <= b) return "$lower-$b"
            lower = b + 1
        }
        return "${sorted.lastOrNull()?.plus(1) ?: 0}+"
    }

    private fun bucketLabels(boundaries: List<Int>): List<String> {
        val sorted = boundaries.sorted()
        val labels = mutableListOf<String>()
        var lower = 0
        for (b in sorted) {
            labels += "$lower-$b"
            lower = b + 1
        }
        labels += "${sorted.lastOrNull()?.plus(1) ?: 0}+"
        return labels
    }

    private fun parseIsoOrNull(iso: String): Instant? = try {
        if (iso.isBlank()) null else Instant.parse(iso)
    } catch (_: Exception) {
        null
    }

    private fun daysBetween(from: Instant, to: Instant): Int {
        val millis = to.toEpochMilliseconds() - from.toEpochMilliseconds()
        return (millis / DAY_MILLIS).toInt()
    }

    private fun plusDaysIso(from: Instant, days: Int): String =
        Instant.fromEpochMilliseconds(from.toEpochMilliseconds() + days.toLong() * DAY_MILLIS).toString()

    companion object {
        val DEFAULT_BUCKETS = listOf(30, 60, 90)
        private const val DAY_MILLIS = 86_400_000L
    }
}
