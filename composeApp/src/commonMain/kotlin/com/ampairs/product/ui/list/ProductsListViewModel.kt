package com.ampairs.product.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.product.domain.ProductListItem
import com.ampairs.product.domain.ProductListKey
import com.ampairs.product.domain.ProductStore
import com.ampairs.common.viewmodel.handleCancellation
import com.ampairs.common.viewmodel.shouldShowAsError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

data class ProductsListUiState(
    val products: List<ProductListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class ProductsListViewModel(
    private val productStore: ProductStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsListUiState())
    val uiState: StateFlow<ProductsListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSearchQuery()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            handleCancellation(
                onError = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
            ) {
                val key = ProductListKey(searchQuery = _uiState.value.searchQuery)
                productStore.productListStore
                    .stream(StoreReadRequest.cached(key, refresh = false))
                    .catch { throwable ->
                        if (throwable.shouldShowAsError()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = throwable.message ?: "Unknown error"
                                )
                            }
                        }
                    }
                    .collect { response ->
                        when (response) {
                            is StoreReadResponse.Data -> {
                                _uiState.update {
                                    it.copy(
                                        products = response.value,
                                        isLoading = false,
                                        error = null
                                    )
                                }
                            }
                            is StoreReadResponse.Loading -> {
                                _uiState.update { it.copy(isLoading = true) }
                            }
                            is StoreReadResponse.Error.Exception -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = response.error.message ?: "Unknown error"
                                    )
                                }
                            }
                            is StoreReadResponse.Error.Message -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = response.message
                                    )
                                }
                            }
                            else -> {
                                // Handle other response types if needed
                            }
                        }
                    }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun syncProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                // For now, just refresh the current data
                loadProducts()
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Sync failed"
                    )
                }
            }
        }
    }

    private fun observeSearchQuery() {
        uiState
            .map { it.searchQuery }
            .distinctUntilChanged()
            .debounce(300) // Debounce search queries
            .onEach { query ->
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    performSearch(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun performSearch(query: String) {
        try {
            val key = ProductListKey(searchQuery = query)
            productStore.productListStore
                .stream(StoreReadRequest.cached(key, refresh = false))
                .catch { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Search failed")
                    }
                }
                .collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _uiState.update {
                                it.copy(
                                    products = response.value,
                                    error = null
                                )
                            }
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _uiState.update {
                                it.copy(error = response.error.message ?: "Search failed")
                            }
                        }
                        is StoreReadResponse.Error.Message -> {
                            _uiState.update {
                                it.copy(error = response.message)
                            }
                        }
                        else -> {
                            // Handle other response types if needed
                        }
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = e.message ?: "Search failed")
            }
        }
    }
}