package com.ampairs.product.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.ProductListItem
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductsListUiState(
    val products: List<ProductListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class ProductsListViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsListUiState())
    val uiState: StateFlow<ProductsListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeProducts()
        syncProducts()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun syncProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                productRepository.syncProducts()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Sync failed") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeProducts() {
        _uiState.update { it.copy(isLoading = true) }
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query -> productRepository.searchProducts(query) }
            .onEach { products ->
                _uiState.update { it.copy(products = products, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }
}
