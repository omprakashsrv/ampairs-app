package com.ampairs.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.domain.CustomerListItem
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lightweight party (customer) picker for starting a collection. Lists customers from the shared
 * [CustomerDataService]; selecting one opens the Record Payment screen for that party.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PaymentPartyPickerViewModel(
    private val customerDataService: CustomerDataService,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _customers = MutableStateFlow<List<CustomerListItem>>(emptyList())
    val customers: StateFlow<List<CustomerListItem>> = _customers.asStateFlow()

    init {
        load("")
    }

    fun onQueryChange(q: String) {
        _query.value = q
        load(q)
    }

    private fun load(q: String) {
        viewModelScope.launch {
            _customers.value = customerDataService.listCustomers(q)
        }
    }
}
