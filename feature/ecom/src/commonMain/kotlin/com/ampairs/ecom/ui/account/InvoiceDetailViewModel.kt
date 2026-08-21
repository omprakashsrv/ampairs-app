package com.ampairs.ecom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.BuyerInvoiceDetail
import com.ampairs.ecom.data.repository.BuyerInvoiceRepository
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceDetailUiState(
    val invoice: BuyerInvoiceDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@AssistedInject
class InvoiceDetailViewModel(
    @Assisted val invoiceUid: String,
    private val repository: BuyerInvoiceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InvoiceDetailUiState())
    val state: StateFlow<InvoiceDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getInvoice(invoiceUid).fold(
                onSuccess = { detail -> _state.update { it.copy(invoice = detail, isLoading = false) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } },
            )
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(invoiceUid: String): InvoiceDetailViewModel
    }
}
