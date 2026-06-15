package com.ampairs.product.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.components.FilterOption
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.ProductListItem
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Applied product-list filter selection. Empty sets mean the dimension is not filtered. */
data class ProductFilter(
    val brands: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val subCategories: Set<String> = emptySet(),
    val groups: Set<String> = emptySet(),
) {
    val activeCount: Int get() = brands.size + categories.size + subCategories.size + groups.size
}

data class ProductsListUiState(
    val products: List<ProductListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val filter: ProductFilter = ProductFilter(),
    val brandOptions: List<FilterOption> = emptyList(),
    val categoryOptions: List<FilterOption> = emptyList(),
    val subCategoryOptions: List<FilterOption> = emptyList(),
    val groupOptions: List<FilterOption> = emptyList(),
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class ProductsListViewModel(
    private val productRepository: ProductRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsListUiState())
    val uiState: StateFlow<ProductsListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(ProductFilter())

    init {
        observeProducts()
        syncService.observeEntity(SyncEntity.PRODUCT)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        loadFilterOptions()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** Reload available filter options — call when opening the filter sheet so they reflect latest data. */
    fun onFilterOpened() {
        loadFilterOptions()
    }

    fun applyFilter(filter: ProductFilter) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter) }
    }

    fun clearFilter() {
        applyFilter(ProductFilter())
    }

    fun syncProducts() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.PRODUCT))
    }

    @OptIn(FlowPreview::class)
    private fun observeProducts() {
        _uiState.update { it.copy(isLoading = true) }
        combine(
            _searchQuery.debounce(300).distinctUntilChanged(),
            _filter,
        ) { query, filter -> query to filter }
            .flatMapLatest { (query, filter) ->
                // FTS-backed, capped search/browse (name / code / description) — scales to large catalogs.
                productRepository.searchAndFilter(
                    query = query,
                    brands = filter.brands.toList(),
                    categories = filter.categories.toList(),
                    subCategories = filter.subCategories.toList(),
                    groups = filter.groups.toList(),
                )
            }
            .onEach { products ->
                _uiState.update { it.copy(products = products, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            val brands = productRepository.getBrandFilterOptions()
            val categories = productRepository.getCategoryFilterOptions()
            val subCategories = productRepository.getSubCategoryFilterOptions()
            val groups = productRepository.getGroupFilterOptions()
            _uiState.update {
                it.copy(
                    brandOptions = brands,
                    categoryOptions = categories,
                    subCategoryOptions = subCategories,
                    groupOptions = groups,
                )
            }
        }
    }
}
