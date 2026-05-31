package com.ampairs.product.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProductCatalogType { BRANDS, CATEGORIES, SUB_CATEGORIES, GROUPS }

@AssistedInject
class ProductCatalogViewModel(
    private val repository: ProductCatalogRepository,
    @Assisted val catalogType: ProductCatalogType,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(catalogType: ProductCatalogType): ProductCatalogViewModel
    }

    private val _items = MutableStateFlow<List<CatalogItem>>(emptyList())
    val items: StateFlow<List<CatalogItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allItems: List<CatalogItem> = emptyList()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            allItems = when (catalogType) {
                ProductCatalogType.BRANDS -> repository.getBrands()
                ProductCatalogType.CATEGORIES -> repository.getCategories()
                ProductCatalogType.SUB_CATEGORIES -> repository.getSubCategories()
                ProductCatalogType.GROUPS -> repository.getGroups()
            }
            applyFilter()
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val q = _searchQuery.value.trim()
        _items.value = if (q.isEmpty()) allItems
        else allItems.filter { it.name.contains(q, ignoreCase = true) }
    }
}
