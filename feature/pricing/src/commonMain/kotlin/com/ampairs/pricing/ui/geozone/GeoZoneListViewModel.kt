package com.ampairs.pricing.ui.geozone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.GeoZone
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

data class GeoZoneListUiState(
    val zones: List<GeoZone> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class GeoZoneListViewModel(
    private val repository: PriceListRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeoZoneListUiState())
    val uiState: StateFlow<GeoZoneListUiState> = _uiState.asStateFlow()

    init {
        repository.observeGeoZones()
            .onEach { zones -> _uiState.update { it.copy(zones = zones, isLoading = false, error = null) } }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
        syncService.observeEntity(SyncEntity.GEO_ZONE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.GEO_ZONE))
    }

    fun deleteZone(id: String) {
        viewModelScope.launch {
            val result = repository.deleteGeoZone(id)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete geo zone") }
            }
        }
    }
}
