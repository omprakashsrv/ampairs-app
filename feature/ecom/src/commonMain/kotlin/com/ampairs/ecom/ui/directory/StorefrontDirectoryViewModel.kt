package com.ampairs.ecom.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.data.repository.StorefrontDirectoryRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class StorefrontDirectoryUiState(
    val query: String = "",
    val storefronts: List<Storefront> = emptyList(),
    val isLoading: Boolean = false,
    // True when the shown list came from the offline cache (network was unavailable).
    val isOffline: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the storefront directory (picker) of the common multi-store app. AppScope — it runs before
 * any storefront is selected/activated, so it must not depend on workspace-scoped state. Reactive
 * debounced search over [StorefrontDirectoryRepository.listStorefronts].
 */
@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class StorefrontDirectoryViewModel(
    private val repository: StorefrontDirectoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorefrontDirectoryUiState())
    val uiState: StateFlow<StorefrontDirectoryUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")
    // Bumped by refresh() to force a reload even when the query text is unchanged.
    private val refreshTrigger = MutableStateFlow(0L)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val loader = combine(query, refreshTrigger) { q, tick -> q to tick }
        .debounce { (q, _) -> if (q.isBlank()) 0L else 300L }
        .distinctUntilChanged()
        .onEach { _uiState.update { s -> s.copy(isLoading = true, error = null) } }
        .mapLatest { (q, _) -> q to repository.listStorefronts(q.ifBlank { null }) }
        .onEach { (q, result) ->
            result.fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            query = q,
                            storefronts = page.stores,
                            isLoading = false,
                            isOffline = page.fromCache,
                            error = null,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(query = q, isLoading = false, isOffline = false, error = e.message ?: "Failed to load stores")
                    }
                },
            )
        }
        .launchIn(viewModelScope)

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        query.value = value
    }

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }
}
