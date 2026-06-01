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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

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

    val searchQuery = MutableStateFlow("")

    private val allItems: StateFlow<List<CatalogItem>> = repository.observeAllItems(catalogType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<CatalogItem>> = combine(allItems, searchQuery) { items, query ->
        if (query.isBlank()) items
        else items.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Mark not-loading after the first real DB emission
        repository.observeAllItems(catalogType)
            .take(1)
            .onEach { _isLoading.value = false }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}
