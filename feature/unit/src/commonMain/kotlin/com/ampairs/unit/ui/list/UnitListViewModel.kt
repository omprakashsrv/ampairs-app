package com.ampairs.unit.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.unit.data.repository.UnitRepository
import com.ampairs.unit.domain.model.Unit
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * UI state for Unit List Screen
 */
data class UnitListUiState(
    val units: List<Unit> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

/**
 * ViewModel for Unit List Screen
 *
 * Features:
 * - Store5 integration for offline-first data
 * - Search with 300ms debouncing
 * - Pull-to-refresh sync
 * - Delete operations
 * - Error handling
 */
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class UnitListViewModel(
    private val unitRepository: UnitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitListUiState())
    val uiState: StateFlow<UnitListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        refreshUnits()
        observeSearchQuery()
    }

    /**
     * Update search query (triggers debounced search)
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Delete a unit (soft delete)
     */
    fun deleteUnit(unitId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = unitRepository.deleteUnit(unitId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete unit")
                }
            } else {
                // Refresh the list after successful deletion
                refreshUnits()
            }
        }
    }

    /**
     * Sync units with server
     *
     * Performs full two-way sync:
     * 1. Push local changes to server
     * 2. Pull server updates in batches
     */
    fun syncUnits() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                val result = unitRepository.syncUnits()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = if (result.isFailure) {
                            result.exceptionOrNull()?.message ?: "Sync failed"
                        } else null
                    )
                }

                // Refresh list after sync
                if (result.isSuccess) {
                    refreshUnits()
                }
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

    /**
     * Observe search query with debouncing
     *
     * Waits 300ms after user stops typing before executing search
     */
    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        _searchQuery
            .debounce(300) // Wait 300ms after user stops typing
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    searchUnits(query)
                } else {
                    refreshUnits()
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Refresh units from Store5 (triggers network fetch)
     */
    private fun refreshUnits() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            unitRepository.getUnitsFlow(page = 0, size = 100, forceRefresh = true)
                .collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _uiState.update {
                                it.copy(
                                    units = response.value,
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
                                    error = response.error.message ?: "Failed to load units"
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

    /**
     * Search units locally (no network call)
     */
    private fun searchUnits(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            unitRepository.searchUnits(query)
                .collect { units ->
                    _uiState.update {
                        it.copy(
                            units = units,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }
}
