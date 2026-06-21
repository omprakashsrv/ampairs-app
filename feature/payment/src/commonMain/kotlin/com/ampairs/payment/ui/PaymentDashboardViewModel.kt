package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.payment.data.repository.PartyBalanceRepository
import com.ampairs.payment.domain.AgingSummary
import com.ampairs.payment.domain.InvoiceLedgerPoster
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A party row for the collections list: its balance plus the resolved customer display name. */
data class PartyRow(val balance: PartyBalance, val name: String)

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
    private val outstandingService: OutstandingService,
    private val customerDataService: CustomerDataService,
) : ViewModel() {

    val rows: StateFlow<List<PartyRow>> =
        partyBalanceRepository.observeAll()
            .map { list ->
                list.map { balance ->
                    val name = customerDataService.getById(balance.partyUid)?.name
                        ?.ifBlank { balance.partyUid } ?: balance.partyUid
                    PartyRow(balance, name)
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
        }
    }
}
