package com.ampairs.tax.ui.hsn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.domain.HsnCategory
import com.ampairs.tax.domain.HsnListItem
import com.ampairs.tax.domain.TaxStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest

@OptIn(FlowPreview::class)
class HsnCodesListViewModel(
    private val taxStore: TaxStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HsnCodesListUiState())
    val uiState: StateFlow<HsnCodesListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<HsnCategory?>(null)

    init {
        // Combine search and filter flows for reactive filtering
        combine(
            _searchQuery.debounce(300),
            _selectedCategory
        ) { query, category ->
            Pair(query, category)
        }.onEach { (query, category) ->
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                selectedCategory = category
            )
            filterHsnCodes(query, category)
        }.launchIn(viewModelScope)
    }

    fun loadHsnCodes() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                taxStore.allHsnCodesStore
                    .stream(StoreReadRequest.cached(Unit, refresh = false))
                    .collect { response ->
                        when (response) {
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Data -> {
                                val hsnListItems = response.value.map { hsnCode ->
                                    // Get current GST rate for this HSN code
                                    val currentRate = try {
                                        taxStore.getTaxRatesByHsnCode(hsnCode.hsnCode)
                                            .filter { it.isActive && it.isCurrentlyEffective }
                                            .firstOrNull()?.ratePercentage
                                    } catch (e: Exception) {
                                        null
                                    }

                                    HsnListItem(
                                        id = hsnCode.id,
                                        hsnCode = hsnCode.hsnCode,
                                        description = hsnCode.description,
                                        category = hsnCode.category,
                                        currentGstRate = currentRate,
                                        isActive = hsnCode.isActive
                                    )
                                }

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    allHsnCodes = hsnListItems,
                                    hsnCodes = filterHsnCodes(
                                        hsnListItems,
                                        _uiState.value.searchQuery,
                                        _uiState.value.selectedCategory
                                    )
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Exception -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = response.error.message ?: "Failed to load HSN codes"
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Message -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = response.message
                                )
                            }
                            else -> {
                                // Handle loading state
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load HSN codes"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(category: HsnCategory?) {
        _selectedCategory.value = category
    }

    fun syncHsnCodes() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)

        viewModelScope.launch {
            try {
                // Trigger a fresh fetch from the server
                taxStore.allHsnCodesStore
                    .stream(StoreReadRequest.fresh(Unit))
                    .collect { response ->
                        when (response) {
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Data -> {
                                _uiState.value = _uiState.value.copy(isRefreshing = false)
                                // The data will be automatically updated through the existing flow
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Exception -> {
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = response.error.message ?: "Failed to sync HSN codes"
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Message -> {
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = response.message
                                )
                            }
                            else -> {
                                // Handle loading state
                            }
                        }
                    }

                // Also sync the offline data
                taxStore.syncData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Failed to sync HSN codes"
                )
            }
        }
    }

    private fun filterHsnCodes(query: String, category: HsnCategory?) {
        val currentState = _uiState.value
        val filteredCodes = filterHsnCodes(currentState.allHsnCodes, query, category)
        _uiState.value = currentState.copy(hsnCodes = filteredCodes)
    }

    private fun filterHsnCodes(
        hsnCodes: List<HsnListItem>,
        query: String,
        category: HsnCategory?
    ): List<HsnListItem> {
        return hsnCodes.filter { hsnCode ->
            val matchesQuery = query.isBlank() ||
                hsnCode.hsnCode.contains(query, ignoreCase = true) ||
                hsnCode.description.contains(query, ignoreCase = true)

            val matchesCategory = category == null || hsnCode.category == category

            matchesQuery && matchesCategory
        }
    }
}

data class HsnCodesListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val allHsnCodes: List<HsnListItem> = emptyList(),
    val hsnCodes: List<HsnListItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: HsnCategory? = null
)