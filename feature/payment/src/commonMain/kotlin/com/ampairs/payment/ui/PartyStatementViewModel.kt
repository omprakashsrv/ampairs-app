package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.payment.data.repository.LedgerEntryRepository
import com.ampairs.payment.data.repository.PartyBalanceRepository
import com.ampairs.payment.domain.PartyBalance
import com.ampairs.payment.domain.StatementBuilder
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PartyStatementUiState(
    val partyUid: String = "",
    val partyName: String = "",
    val statement: StatementBuilder.Statement? = null,
)

/**
 * Party ledger statement (US2 / FR-004). Reactive and **offline** — built from the local party
 * balance + ledger entries via [StatementBuilder]; the last line's running balance equals the
 * closing balance (SC-002).
 */
@AssistedInject
class PartyStatementViewModel(
    @Assisted val partyUid: String,
    ledgerEntryRepository: LedgerEntryRepository,
    partyBalanceRepository: PartyBalanceRepository,
    customerDataService: CustomerDataService,
) : ViewModel() {

    private val partyName = MutableStateFlow("")

    init {
        viewModelScope.launch {
            partyName.value = customerDataService.getById(partyUid)?.name.orEmpty()
        }
    }

    val uiState: StateFlow<PartyStatementUiState> = combine(
        partyBalanceRepository.observeBalance(partyUid),
        ledgerEntryRepository.observeByParty(partyUid),
        partyName,
    ) { balance, entries, name ->
        val resolved = balance ?: PartyBalance(partyUid = partyUid)
        PartyStatementUiState(
            partyUid = partyUid,
            partyName = name,
            statement = StatementBuilder.build(resolved, entries),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PartyStatementUiState(partyUid = partyUid),
    )

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(partyUid: String): PartyStatementViewModel
    }
}
