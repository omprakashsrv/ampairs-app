package com.ampairs.tax.ui.rates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.TaxRate
import com.ampairs.tax.domain.TaxRateListItem
import com.ampairs.tax.domain.TaxStore
import com.ampairs.tax.domain.TaxType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest

@OptIn(FlowPreview::class)
class TaxRatesListViewModel(
    private val taxStore: TaxStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxRatesListUiState())
    val uiState: StateFlow<TaxRatesListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedBusinessType = MutableStateFlow<BusinessType?>(null)
    private val _selectedTaxType = MutableStateFlow<TaxType?>(null)

    init {
        // Combine search and filter flows for reactive filtering
        combine(
            _searchQuery.debounce(300),
            _selectedBusinessType,
            _selectedTaxType
        ) { query, businessType, taxType ->
            Triple(query, businessType, taxType)
        }.onEach { (query, businessType, taxType) ->
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                selectedBusinessType = businessType,
                selectedTaxType = taxType
            )
            filterTaxRates(query, businessType, taxType)
        }.launchIn(viewModelScope)

        // Load initial data
        loadTaxRates()
    }

    fun loadTaxRates() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                taxStore.allTaxRatesStore
                    .stream(StoreReadRequest.cached(Unit, refresh = false))
                    .collect { response ->
                        when (response) {
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Data -> {
                                val taxRateListItems = response.value.map { taxRate ->
                                    TaxRateListItem(
                                        id = taxRate.id,
                                        hsnCode = taxRate.hsnCode,
                                        hsnDescription = "HSN ${taxRate.hsnCode}", // TODO: Get actual description
                                        gstRate = taxRate.ratePercentage,
                                        cessRate = taxRate.cessRate,
                                        effectiveFrom = taxRate.effectiveFrom,
                                        effectiveTo = taxRate.effectiveTo,
                                        businessType = taxRate.businessType,
                                        isActive = taxRate.isActive
                                    )
                                }

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    allTaxRates = taxRateListItems,
                                    filteredTaxRates = taxRateListItems,
                                    error = null
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Exception -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load tax rates: ${response.error.message}"
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Message -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load tax rates: ${response.message}"
                                )
                            }
                            else -> {
                                // Loading state - do nothing
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading tax rates: ${e.message}"
                )
            }
        }
    }

    fun refreshTaxRates() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)

        viewModelScope.launch {
            try {
                taxStore.allTaxRatesStore
                    .stream(StoreReadRequest.fresh(Unit))
                    .collect { response ->
                        when (response) {
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Data -> {
                                val taxRateListItems = response.value.map { taxRate ->
                                    TaxRateListItem(
                                        id = taxRate.id,
                                        hsnCode = taxRate.hsnCode,
                                        hsnDescription = "HSN ${taxRate.hsnCode}",
                                        gstRate = taxRate.ratePercentage,
                                        cessRate = taxRate.cessRate,
                                        effectiveFrom = taxRate.effectiveFrom,
                                        effectiveTo = taxRate.effectiveTo,
                                        businessType = taxRate.businessType,
                                        isActive = taxRate.isActive
                                    )
                                }

                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    allTaxRates = taxRateListItems,
                                    filteredTaxRates = applyFilters(taxRateListItems),
                                    error = null
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Exception -> {
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = "Failed to refresh tax rates: ${response.error.message}"
                                )
                            }
                            else -> {
                                // Loading state - continue
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Error refreshing tax rates: ${e.message}"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateBusinessTypeFilter(businessType: BusinessType?) {
        _selectedBusinessType.value = businessType
    }

    fun updateTaxTypeFilter(taxType: TaxType?) {
        _selectedTaxType.value = taxType
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedBusinessType.value = null
        _selectedTaxType.value = null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun filterTaxRates(
        query: String,
        businessType: BusinessType?,
        taxType: TaxType?
    ) {
        val currentState = _uiState.value
        val filtered = applyFilters(currentState.allTaxRates, query, businessType, taxType)
        _uiState.value = currentState.copy(filteredTaxRates = filtered)
    }

    private fun applyFilters(
        taxRates: List<TaxRateListItem>,
        query: String = _uiState.value.searchQuery,
        businessType: BusinessType? = _uiState.value.selectedBusinessType,
        taxType: TaxType? = _uiState.value.selectedTaxType
    ): List<TaxRateListItem> {
        return taxRates.filter { taxRate ->
            val matchesQuery = if (query.isBlank()) true else {
                taxRate.hsnCode.contains(query, ignoreCase = true) ||
                taxRate.hsnDescription.contains(query, ignoreCase = true)
            }

            val matchesBusinessType = businessType == null || taxRate.businessType == businessType
            val matchesTaxType = taxType == null // TODO: Add tax type to TaxRateListItem if needed

            matchesQuery && matchesBusinessType && matchesTaxType
        }
    }
}

data class TaxRatesListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val allTaxRates: List<TaxRateListItem> = emptyList(),
    val filteredTaxRates: List<TaxRateListItem> = emptyList(),
    val searchQuery: String = "",
    val selectedBusinessType: BusinessType? = null,
    val selectedTaxType: TaxType? = null
) {
    val isEmpty: Boolean get() = allTaxRates.isEmpty()
    val hasResults: Boolean get() = filteredTaxRates.isNotEmpty()
    val showEmptyState: Boolean get() = !isLoading && !isRefreshing && isEmpty
    val showNoResults: Boolean get() = !isLoading && !isRefreshing && !isEmpty && filteredTaxRates.isEmpty()
}