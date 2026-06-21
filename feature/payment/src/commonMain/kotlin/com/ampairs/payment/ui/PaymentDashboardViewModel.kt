package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.payment.data.repository.PartyBalanceRepository
import com.ampairs.payment.domain.InvoiceLedgerPoster
import com.ampairs.payment.domain.PartyBalance
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) : ViewModel() {

    val balances: StateFlow<List<PartyBalance>> =
        partyBalanceRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Reconcile finalized invoices into the party ledger locally (offline) so sales receivables
        // appear without a server round-trip. Posting is idempotent (deterministic LDG_<invoice.uid>).
        viewModelScope.launch {
            runCatching { invoiceLedgerPoster.backfillFinalizedInvoices() }
        }
    }
}
