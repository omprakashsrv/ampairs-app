package com.ampairs.ecom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.BuyerInvoiceSummary
import com.ampairs.ecom.api.model.isNotLinked
import com.ampairs.ecom.data.repository.BuyerInvoiceRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceListUiState(
    val invoices: List<BuyerInvoiceSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // true only when the read failed because the buyer isn't linked to a CRM account in this store
    // (server 403 ECOM_NOT_LINKED) — drives the "link your account" hint vs a generic transient error.
    val notLinked: Boolean = false,
)

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class InvoiceListViewModel(
    private val repository: BuyerInvoiceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InvoiceListUiState())
    val state: StateFlow<InvoiceListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getInvoices().fold(
                onSuccess = { page -> _state.update { it.copy(invoices = page.content, isLoading = false) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message, notLinked = e.isNotLinked()) } },
            )
        }
    }
}
