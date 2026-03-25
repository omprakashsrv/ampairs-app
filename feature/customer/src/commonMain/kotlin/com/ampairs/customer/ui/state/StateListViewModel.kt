package com.ampairs.customer.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.StateKey
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.domain.MasterState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlinx.coroutines.FlowPreview

data class StateListUiState(
    val states: List<State> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val availableStatesForImport: List<MasterState> = emptyList(),
    val isLoadingImportStates: Boolean = false
)

class StateListViewModel(
    private val stateStore: StateStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(StateListUiState())
    val uiState: StateFlow<StateListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        refreshStates()
        observeSearchQuery()
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
            } else {
                // Refresh the list after successful deletion
                refreshStates()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        searchQuery
            .debounce(300) // Debounce search queries
            .distinctUntilChanged()
            .onEach { query ->
                searchStates(query)
            }
            .launchIn(viewModelScope)
    }

    private fun searchStates(query: String) {
        viewModelScope.launch {
            stateStore.searchStatesFlow(query)
                .collect { filteredStates ->
                    _uiState.update {
                        it.copy(states = filteredStates)
                    }
                }
        }
    }

    fun refreshStates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val key = StateKey()
                stateStore.stateStore
                    .stream(StoreReadRequest.cached(key, refresh = true))
                    .collect { response ->
                        when (response) {
                            is StoreReadResponse.Data -> {
                                _uiState.update {
                                    it.copy(
                                        states = response.value,
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
                                        error = response.error.message ?: "Failed to refresh states"
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to refresh states"
                    )
                }
            }
        }
    }

    fun importState(stateCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = stateStore.importState(stateCode)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, error = null) }
                // Refresh the list from backend after successful import
                refreshStates()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to import state"
                    )
                }
            }
        }
    }

    fun bulkImportStates(stateCodes: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = stateStore.bulkImportStates(stateCodes)
            if (result.isSuccess) {
                val response = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
                // Refresh the list from backend after successful import
                refreshStates()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to import states"
                    )
                }
            }
        }
    }

    fun loadAvailableStatesForImport(workspaceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImportStates = true, error = null) }

            val result = stateStore.getAvailableStatesForImport(workspaceId)
            if (result.isSuccess) {
                val masterStates = result.getOrNull() ?: emptyList()
                _uiState.update {
                    it.copy(
                        availableStatesForImport = masterStates,
                        isLoadingImportStates = false,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingImportStates = false,
                        error = result.exceptionOrNull()?.message
                            ?: "Failed to load available states"
                    )
                }
            }
        }
    }
}