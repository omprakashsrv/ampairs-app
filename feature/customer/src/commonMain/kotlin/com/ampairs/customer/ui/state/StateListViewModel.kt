package com.ampairs.customer.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.domain.MasterState
import com.ampairs.common.di.WorkspaceScope
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

data class StateListUiState(
    val states: List<State> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val availableStatesForImport: List<MasterState> = emptyList(),
    val isLoadingImportStates: Boolean = false
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class StateListViewModel(
    private val stateStore: StateStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StateListUiState())
    val uiState: StateFlow<StateListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        observeStates()
        syncStates()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteState(stateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = stateStore.deleteState(stateId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete state")
                }
            }
        }
    }

    fun syncStates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                stateStore.syncStates()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Sync failed") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun importState(stateCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = stateStore.importState(stateCode)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (result.isFailure) result.exceptionOrNull()?.message ?: "Failed to import state" else null
                )
            }
        }
    }

    fun bulkImportStates(stateCodes: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = stateStore.bulkImportStates(stateCodes)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (result.isFailure) result.exceptionOrNull()?.message ?: "Failed to import states" else null
                )
            }
        }
    }

    fun loadAvailableStatesForImport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImportStates = true, error = null) }
            val result = stateStore.getAvailableStatesForImport()
            _uiState.update {
                it.copy(
                    isLoadingImportStates = false,
                    availableStatesForImport = result.getOrElse { emptyList() },
                    error = if (result.isFailure) result.exceptionOrNull()?.message ?: "Failed to load available states" else null
                )
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeStates() {
        _uiState.update { it.copy(isLoading = true) }
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query -> stateStore.searchStatesFlow(query) }
            .onEach { states ->
                _uiState.update { it.copy(states = states, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }
}
