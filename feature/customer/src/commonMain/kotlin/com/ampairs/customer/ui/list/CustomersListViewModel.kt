package com.ampairs.customer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.common.viewmodel.handleCancellation
import com.ampairs.common.viewmodel.shouldShowAsError
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomersListUiState(
    val customers: List<CustomerListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class CustomersListViewModel(
    private val customerStore: CustomerStore,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersListUiState())
    val uiState: StateFlow<CustomersListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSearchQuery()
        syncService.observeEntity(SyncEntity.CUSTOMER)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            handleCancellation(
                onError = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
            ) {
                val query = _uiState.value.searchQuery
                val flow = if (query.isBlank()) {
                    customerStore.observeCustomers()
                } else {
                    customerStore.searchCustomers(query)
                }

                flow.catch { throwable ->
                    if (throwable.shouldShowAsError()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                }.collect { customers ->
                    _uiState.update {
                        it.copy(
                            customers = customers,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun syncCustomers() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CUSTOMER))
    }

    private fun observeSearchQuery() {
        uiState
            .map { it.searchQuery }
            .distinctUntilChanged()
            .onEach { query ->
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    performSearch(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun performSearch(query: String) {
        handleCancellation(
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        ) {
            val flow = if (query.isBlank()) {
                customerStore.observeCustomers()
            } else {
                customerStore.searchCustomers(query)
            }

            flow.catch { throwable ->
                if (throwable.shouldShowAsError()) {
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Search failed")
                    }
                }
            }.collect { customers ->
                _uiState.update {
                    it.copy(
                        customers = customers,
                        error = null
                    )
                }
            }
        }
    }
}