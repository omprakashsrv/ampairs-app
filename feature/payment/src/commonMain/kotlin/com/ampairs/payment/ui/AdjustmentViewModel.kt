package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.payment.data.repository.AdjustmentRepository
import com.ampairs.payment.domain.AdjustmentType
import com.ampairs.payment.domain.AdjustmentVoucher
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

data class AdjustmentUiState(
    val partyUid: String = "",
    val partyName: String = "",
    val type: AdjustmentType = AdjustmentType.SALES_RETURN,
    val amountText: String = "",
    val narration: String = "",
    val saving: Boolean = false,
) {
    val amount: Money get() = Money.fromDecimalString(amountText)
}

sealed interface AdjustmentEvent {
    data class Saved(val uid: String) : AdjustmentEvent
    data class Error(val message: String) : AdjustmentEvent
}

/** Create a non-payment ledger movement (return / credit-debit note / write-off / purchase). Offline. */
@OptIn(ExperimentalTime::class)
@AssistedInject
class AdjustmentViewModel(
    @Assisted val partyUid: String,
    private val adjustmentRepository: AdjustmentRepository,
    private val customerDataService: CustomerDataService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdjustmentUiState(partyUid = partyUid))
    val uiState: StateFlow<AdjustmentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AdjustmentEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AdjustmentEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val name = if (partyUid.isNotBlank()) customerDataService.getById(partyUid)?.name.orEmpty() else ""
            _uiState.update { it.copy(partyName = name) }
        }
    }

    fun onTypeChange(type: AdjustmentType) = _uiState.update { it.copy(type = type) }
    fun onAmountChange(text: String) = _uiState.update { it.copy(amountText = text.filter { c -> c.isDigit() || c == '.' }) }
    fun onNarrationChange(text: String) = _uiState.update { it.copy(narration = text) }

    fun save() {
        val state = _uiState.value
        if (!state.amount.isPositive) {
            _events.tryEmit(AdjustmentEvent.Error("Amount must be greater than zero"))
            return
        }
        if (state.partyUid.isBlank()) {
            _events.tryEmit(AdjustmentEvent.Error("Select a party"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val uid = UidGenerator.generateUid("ADJ")
            val voucher = AdjustmentVoucher(
                uid = uid,
                partyUid = state.partyUid,
                voucherNo = "",
                voucherDate = Clock.System.now().toString(),
                adjustmentType = state.type,
                amount = state.amount,
                narration = state.narration.ifBlank { null },
            )
            val result = adjustmentRepository.save(voucher)
            _uiState.update { it.copy(saving = false) }
            result.fold(
                onSuccess = { _events.tryEmit(AdjustmentEvent.Saved(uid)) },
                onFailure = { _events.tryEmit(AdjustmentEvent.Error(it.message ?: "Failed to save adjustment")) },
            )
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(partyUid: String): AdjustmentViewModel
    }
}
