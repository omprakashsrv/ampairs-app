package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.Constants.Companion.PAGE_SIZE
import com.ampairs.invoice.db.InvoiceRepository
import kotlinx.coroutines.Dispatchers
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class InvoicesViewModel(val invoiceRepository: InvoiceRepository) : ViewModel() {

    var searchText by mutableStateOf("")
    val invoicesState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    init {
        syncTaxInfos()
    }

    private fun syncTaxInfos() {
        viewModelScope.launch(DispatcherProvider.io) {
            invoiceRepository.getInvoiceResource().collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            invoicesState.value = UiState.Loading(false)
                        }

                        is Resource.Status.Success -> {
                            invoicesState.value = UiState.Success(true)
                        }

                        is Resource.Status.EmptySuccess -> {
                            invoicesState.value = UiState.Empty
                        }

                        is Resource.Status.Error -> {
                            val status = response.status as Resource.Status.Error
                            invoicesState.value = UiState.Error(status.errorMessage)
                        }
                    }
                }
            }
        }
    }

    val invoices = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        invoiceRepository.getInvoicePaging(searchText)
    }).flow
        .cachedIn(viewModelScope)
}