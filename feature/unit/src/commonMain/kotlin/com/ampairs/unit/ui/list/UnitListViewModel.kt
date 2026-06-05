package com.ampairs.unit.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.unit.data.repository.UnitRepository
import com.ampairs.unit.domain.model.Unit
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
import kotlinx.coroutines.launch

data class UnitListUiState(
    val units: List<Unit> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class UnitListViewModel(
    private val unitRepository: UnitRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitListUiState())
    val uiState: StateFlow<UnitListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeUnits()
        syncService.observeEntity(SyncEntity.UNIT)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.UNIT))
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteUnit(unitId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = unitRepository.deleteUnit(unitId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete unit")
                }
            }
        }
    }

    fun syncUnits() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.UNIT))
    }

    @OptIn(FlowPreview::class)
    private fun observeUnits() {
        _uiState.update { it.copy(isLoading = true) }
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query -> unitRepository.searchUnits(query) }
            .onEach { units ->
                _uiState.update { it.copy(units = units, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }
}
