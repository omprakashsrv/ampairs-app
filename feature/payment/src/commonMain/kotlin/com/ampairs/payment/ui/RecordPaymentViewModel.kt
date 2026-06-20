package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.payment.data.repository.PaymentVoucherRepository
import com.ampairs.payment.domain.AllocationTarget
import com.ampairs.payment.domain.ClearanceStatus
import com.ampairs.payment.domain.Money
import com.ampairs.payment.domain.OpenBill
import com.ampairs.payment.domain.OutstandingService
import com.ampairs.payment.domain.PaymentAllocation
import com.ampairs.payment.domain.PaymentDirection
import com.ampairs.payment.domain.PaymentMode
import com.ampairs.payment.domain.PaymentSettings
import com.ampairs.payment.domain.PaymentVoucher
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import com.ampairs.common.di.WorkspaceScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A single bill the user may allocate part of the receipt to. */
data class AllocationRow(
    val bill: OpenBill,
    val allocatedMinor: Long = 0,
)

data class RecordPaymentUiState(
    val partyUid: String = "",
    val partyName: String = "",
    val direction: PaymentDirection = PaymentDirection.RECEIVED,
    val amountText: String = "",
    val mode: PaymentMode = PaymentMode.CASH,
    val enabledModes: List<PaymentMode> = PaymentSettings.DEFAULT_MODES,
    val referenceNumber: String = "",
    val chequeNumber: String = "",
    val bankName: String = "",
    val instrumentDate: String = "",
    val narration: String = "",
    val openBills: List<AllocationRow> = emptyList(),
    val overCreditLimit: Boolean = false,
    val saving: Boolean = false,
) {
    val amount: Money get() = Money.fromDecimalString(amountText)
    val allocatedTotal: Money
        get() = Money(openBills.sumOf { it.allocatedMinor })
    val onAccount: Money get() = (amount - allocatedTotal).let { if (it.isPositive) it else Money.ZERO }
}

sealed interface RecordPaymentEvent {
    data class Saved(val voucherUid: String) : RecordPaymentEvent
    data class Error(val message: String) : RecordPaymentEvent
}

@OptIn(ExperimentalTime::class)
@AssistedInject
class RecordPaymentViewModel(
    @Assisted val partyUid: String,
    private val voucherRepository: PaymentVoucherRepository,
    private val outstandingService: OutstandingService,
    private val customerDataService: CustomerDataService,
    private val paymentSettings: PaymentSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordPaymentUiState(partyUid = partyUid))
    val uiState: StateFlow<RecordPaymentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RecordPaymentEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RecordPaymentEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val modes = paymentSettings.enabledPaymentModes()
            val default = paymentSettings.defaultPaymentMode().takeIf { it in modes } ?: modes.firstOrNull() ?: PaymentMode.CASH
            val name = if (partyUid.isNotBlank()) customerDataService.getById(partyUid)?.name.orEmpty() else ""
            val bills = if (partyUid.isNotBlank()) {
                outstandingService.openBills(partyUid, paymentSettings.agingBuckets()).map { AllocationRow(it) }
            } else emptyList()
            val overLimit = if (partyUid.isNotBlank()) outstandingService.isOverCreditLimit(partyUid) else false
            _uiState.update {
                it.copy(
                    partyName = name,
                    enabledModes = modes,
                    mode = default,
                    openBills = bills,
                    overCreditLimit = overLimit,
                )
            }
        }
    }

    fun onDirectionChange(direction: PaymentDirection) = _uiState.update { it.copy(direction = direction) }
    fun onAmountChange(text: String) = _uiState.update { it.copy(amountText = text.filter { c -> c.isDigit() || c == '.' }) }
    fun onModeChange(mode: PaymentMode) = _uiState.update { it.copy(mode = mode) }
    fun onReferenceChange(text: String) = _uiState.update { it.copy(referenceNumber = text) }
    fun onChequeNumberChange(text: String) = _uiState.update { it.copy(chequeNumber = text) }
    fun onBankNameChange(text: String) = _uiState.update { it.copy(bankName = text) }
    fun onInstrumentDateChange(text: String) = _uiState.update { it.copy(instrumentDate = text) }
    fun onNarrationChange(text: String) = _uiState.update { it.copy(narration = text) }

    fun onAllocationChange(billUid: String, text: String) {
        val minor = Money.fromDecimalString(text).minor
        _uiState.update { state ->
            state.copy(openBills = state.openBills.map {
                if (it.bill.billUid == billUid) it.copy(allocatedMinor = minor) else it
            })
        }
    }

    /** FIFO auto-allocate the entered amount across the oldest open bills first. */
    fun autoAllocate() {
        _uiState.update { state ->
            var remaining = state.amount.minor
            val rows = state.openBills
                .sortedByDescending { it.bill.daysOverdue }
                .map { row ->
                    val take = minOf(remaining, row.bill.outstanding.minor)
                    remaining -= take
                    row.copy(allocatedMinor = take)
                }
            state.copy(openBills = rows)
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.amount.isPositive) {
            _events.tryEmit(RecordPaymentEvent.Error("Amount must be greater than zero"))
            return
        }
        if (state.partyUid.isBlank()) {
            _events.tryEmit(RecordPaymentEvent.Error("Select a party"))
            return
        }
        if (state.allocatedTotal > state.amount) {
            _events.tryEmit(RecordPaymentEvent.Error("Allocated amount exceeds the payment amount"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val prefix = if (state.direction == PaymentDirection.RECEIVED) "RCP" else "PAY"
            val voucherUid = UidGenerator.generateUid(prefix)
            val requiresClearance = state.mode.isCheque && paymentSettings.chequeRequiresClearance()
            val clearance = if (state.mode.isInstant || !requiresClearance) ClearanceStatus.CLEARED else ClearanceStatus.PENDING
            val reference = if (state.mode.isCheque) state.chequeNumber.ifBlank { null } else state.referenceNumber.ifBlank { null }
            val voucher = PaymentVoucher(
                uid = voucherUid,
                partyUid = state.partyUid,
                voucherNo = "", // numbering assigned at the backend / sequence (provisional ok offline)
                voucherDate = Clock.System.now().toString(),
                direction = state.direction,
                totalAmount = state.amount,
                paymentMode = state.mode,
                referenceNumber = reference,
                instrumentDate = if (state.mode.isCheque) state.instrumentDate.ifBlank { null } else null,
                bankName = if (state.mode.isCheque) state.bankName.ifBlank { null } else null,
                clearanceStatus = clearance,
                narration = state.narration.ifBlank { null },
            )
            val allocations = state.openBills
                .filter { it.allocatedMinor > 0 }
                .map {
                    PaymentAllocation(
                        uid = UidGenerator.generateUid("ALC"),
                        paymentVoucherUid = voucherUid,
                        targetType = AllocationTarget.INVOICE,
                        targetUid = it.bill.billUid,
                        amount = Money(it.allocatedMinor),
                    )
                }
            val result = voucherRepository.save(voucher, allocations)
            _uiState.update { it.copy(saving = false) }
            result.fold(
                onSuccess = { _events.tryEmit(RecordPaymentEvent.Saved(voucherUid)) },
                onFailure = { _events.tryEmit(RecordPaymentEvent.Error(it.message ?: "Failed to save payment")) },
            )
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(partyUid: String): RecordPaymentViewModel
    }
}
