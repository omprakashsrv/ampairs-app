package com.ampairs.supplier.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.supplier.domain.SupplierListItem
import com.ampairs.supplier.domain.SupplierStore
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class SuppliersListUiState(
    val suppliers: List<SupplierListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class SuppliersListViewModel(
    private val supplierStore: SupplierStore,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuppliersListUiState())
    val uiState: StateFlow<SuppliersListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeSuppliers()
        syncService.observeEntity(SyncEntity.SUPPLIER)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        // Initial pull on screen open
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.SUPPLIER))
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** Retry hook: re-run a full sync; the reactive flow re-emits once data lands locally. */
    fun loadSuppliers() {
        syncSuppliers()
    }

    fun syncSuppliers() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.SUPPLIER))
    }

    @OptIn(FlowPreview::class)
    private fun observeSuppliers() {
        _uiState.update { it.copy(isLoading = true) }
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query -> supplierStore.filterSuppliers(query) }
            .onEach { suppliers ->
                _uiState.update { it.copy(suppliers = suppliers, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
            .launchIn(viewModelScope)
    }
}
