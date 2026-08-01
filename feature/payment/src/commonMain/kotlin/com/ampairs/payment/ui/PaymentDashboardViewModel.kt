package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.payment.data.repository.PartyBalanceRepository
import com.ampairs.payment.domain.AgingSummary
import com.ampairs.payment.domain.InvoiceLedgerPoster
import com.ampairs.payment.domain.PurchaseLedgerPoster
import com.ampairs.supplier.data.SupplierDataService
import com.ampairs.payment.domain.OutstandingService
import com.ampairs.payment.domain.PartyBalance
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A party row for the collections list: its balance plus resolved customer display fields. */
data class PartyRow(
    val balance: PartyBalance,
    val name: String,
    val phone: String? = null,
    /** True when a local edit on this balance is still pending push (UI shows a cloud_off icon). */
    val pendingSync: Boolean = false,
)

/**
 * Collections landing for the payment module (spec 013). Lists every party's current closing
 * balance (receivable / payable). Reactive off the local [PartyBalanceRepository] — the cached
 * closing balance is recomputed locally and reconciled from the server on pull (R3/R4).
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PaymentDashboardViewModel(
    partyBalanceRepository: PartyBalanceRepository,
    private val invoiceLedgerPoster: InvoiceLedgerPoster,
    private val purchaseLedgerPoster: PurchaseLedgerPoster,
    private val outstandingService: OutstandingService,
    private val customerDataService: CustomerDataService,
    private val supplierDataService: SupplierDataService,
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    /** True until the first balance emission arrives — drives the dashboard loading skeleton. */
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val rows: StateFlow<List<PartyRow>> =
        partyBalanceRepository.observeAll()
            .onEach { _loading.value = false }
            .map { list ->
                list.map { balance ->
                    // A party is a customer (sales receivable) or a supplier (purchase payable); the
                    // ledger sign decides "To Collect" vs "To Pay". Resolve the name from whichever owns it.
                    val customer = customerDataService.getById(balance.partyUid)
                    val supplier = if (customer == null) supplierDataService.getById(balance.partyUid) else null
                    val resolvedName = (customer?.name ?: supplier?.name)?.ifBlank { null }
                    PartyRow(
                        balance = balance,
                        name = resolvedName ?: balance.partyUid,
                        phone = customer?.phone ?: supplier?.phone,
                        pendingSync = !balance.synced,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(text: String) { _query.value = text }

    /** Rows filtered by the customer-name search box (case-insensitive; blank shows all). */
    val filteredRows: StateFlow<List<PartyRow>> =
        combine(rows, _query) { all, q ->
            val term = q.trim()
            if (term.isBlank()) all else all.filter { it.name.contains(term, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Workspace-wide receivable/payable totals + aging buckets (US4), recomputed locally on change. */
    val aging: StateFlow<AgingSummary?> =
        partyBalanceRepository.observeAll()
            .map { outstandingService.agingSummary() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Reconcile finalized invoices into the party ledger locally (offline) so sales receivables
        // appear without a server round-trip. Posting is idempotent (deterministic LDG_<invoice.uid>).
        viewModelScope.launch {
            runCatching { invoiceLedgerPoster.backfillFinalizedInvoices() }
            // Buy side: reconcile received purchases into supplier payables ("To Pay"), same idempotent
            // deterministic LDG_<purchase.uid> posting so they appear offline without a server round-trip.
            runCatching { purchaseLedgerPoster.backfillReceivedPurchases() }
        }
    }
}
