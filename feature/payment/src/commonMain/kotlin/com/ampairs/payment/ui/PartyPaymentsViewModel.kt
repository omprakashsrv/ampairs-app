package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.payment.data.repository.PaymentVoucherRepository
import com.ampairs.payment.domain.PaymentVoucher
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A party's recorded payments/receipts with their clearance status, plus mark-cleared / mark-bounced
 * actions (US6). Bounce posts a reversal that restores the party's outstanding. All offline.
 */
@AssistedInject
class PartyPaymentsViewModel(
    @Assisted val partyUid: String,
    private val voucherRepository: PaymentVoucherRepository,
    customerDataService: CustomerDataService,
) : ViewModel() {

    private val _partyName = MutableStateFlow("")
    val partyName: StateFlow<String> = _partyName.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val payments: StateFlow<List<PaymentVoucher>> =
        voucherRepository.observeByParty(partyUid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _partyName.value = customerDataService.getById(partyUid)?.name.orEmpty()
        }
    }

    fun markCleared(uid: String) {
        viewModelScope.launch {
            voucherRepository.markCleared(uid).onFailure { _events.tryEmit(it.message ?: "Failed to clear") }
        }
    }

    fun markBounced(uid: String) {
        viewModelScope.launch {
            val reversalUid = UidGenerator.generateUid("REV")
            voucherRepository.markBounced(uid, reversalUid, null)
                .onFailure { _events.tryEmit(it.message ?: "Failed to mark bounced") }
        }
    }

    /** Cancel (soft-delete + reverse the ledger entry) a voucher (FR-012/FR-023). Offline. */
    fun cancel(uid: String) {
        viewModelScope.launch {
            voucherRepository.delete(uid).onFailure { _events.tryEmit(it.message ?: "Failed to cancel") }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(partyUid: String): PartyPaymentsViewModel
    }
}
