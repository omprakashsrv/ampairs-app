package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.payment.data.repository.PartyBalanceRepository
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.domain.Money
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class OpeningBalanceUiState(
    val partyUid: String = "",
    val partyName: String = "",
    val amountText: String = "",
    val direction: Direction = Direction.DR, // DR = to receive, CR = to pay
    val asOf: String = "",
    val saving: Boolean = false,
) {
    val amount: Money get() = Money.fromDecimalString(amountText)
}

sealed interface OpeningBalanceEvent {
    data object Saved : OpeningBalanceEvent
    data class Error(val message: String) : OpeningBalanceEvent
}

/** Set / edit a party's opening balance (cutover). Local-only; recomputes the closing balance offline. */
@OptIn(ExperimentalTime::class)
@AssistedInject
class OpeningBalanceViewModel(
    @Assisted val partyUid: String,
    private val partyBalanceRepository: PartyBalanceRepository,
    private val customerDataService: CustomerDataService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpeningBalanceUiState(partyUid = partyUid))
    val uiState: StateFlow<OpeningBalanceUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OpeningBalanceEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OpeningBalanceEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val name = if (partyUid.isNotBlank()) customerDataService.getById(partyUid)?.name.orEmpty() else ""
            val existing = if (partyUid.isNotBlank()) partyBalanceRepository.getBalance(partyUid) else null
            _uiState.update {
                it.copy(
                    partyName = name,
                    amountText = existing?.openingBalance?.takeIf { b -> !b.isZero }?.toDecimalString() ?: "",
                    direction = existing?.openingDirection ?: Direction.DR,
                    asOf = existing?.openingAsOf.orEmpty(),
                )
            }
        }
    }

    fun onAmountChange(text: String) = _uiState.update { it.copy(amountText = text.filter { c -> c.isDigit() || c == '.' }) }
    fun onDirectionChange(direction: Direction) = _uiState.update { it.copy(direction = direction) }
    fun onAsOfChange(text: String) = _uiState.update { it.copy(asOf = text) }

    fun save() {
        val state = _uiState.value
        if (state.partyUid.isBlank()) {
            _events.tryEmit(OpeningBalanceEvent.Error("Select a party"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val uid = UidGenerator.generateUid("PBL")
            val asOf = state.asOf.ifBlank { Clock.System.now().toString() }
            val result = partyBalanceRepository.setOpeningBalance(
                partyUid = state.partyUid,
                uid = uid,
                openingBalance = state.amount,
                openingDirection = state.direction,
                openingAsOf = asOf,
            )
            _uiState.update { it.copy(saving = false) }
            result.fold(
                onSuccess = { _events.tryEmit(OpeningBalanceEvent.Saved) },
                onFailure = { _events.tryEmit(OpeningBalanceEvent.Error(it.message ?: "Failed to save opening balance")) },
            )
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(partyUid: String): OpeningBalanceViewModel
    }
}
