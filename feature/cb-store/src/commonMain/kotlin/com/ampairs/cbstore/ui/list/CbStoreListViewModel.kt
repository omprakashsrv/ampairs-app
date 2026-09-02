package com.ampairs.cbstore.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbstore.data.repository.StoreRepository
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.domain.model.ZonalOffice
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CbStoreListUiState(
    val stores: List<Store> = emptyList(),
    val zonalOfficeNames: Map<String, String> = emptyMap(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class CbStoreListViewModel(
    private val repository: StoreRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CbStoreListUiState())
    val uiState: StateFlow<CbStoreListUiState> = _uiState.asStateFlow()

    private var all: List<Store> = emptyList()

    init {
        repository.observeStores()
            .onEach { stores -> all = stores; applyFilter() }
            .catch { e -> _uiState.update { it.copy(error = e.message) } }
            .launchIn(viewModelScope)

        repository.observeZonalOffices()
            .onEach { offices ->
                _uiState.update { it.copy(zonalOfficeNames = offices.associate { z -> z.uid to z.name }) }
            }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.CB_STORE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_ZONAL_OFFICE))
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_STORE))
    }

    fun onSearch(q: String) {
        _uiState.update { it.copy(query = q) }
        applyFilter()
    }

    private fun applyFilter() {
        val q = _uiState.value.query.trim()
        val filtered = if (q.isBlank()) all else all.filter {
            it.code.contains(q, true) || it.name.contains(q, true) || it.city.contains(q, true)
        }
        _uiState.update { it.copy(stores = filtered, error = null) }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_ZONAL_OFFICE))
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_STORE))
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch {
            val result = repository.deleteStore(storeId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete outlet") }
            }
        }
    }
}
