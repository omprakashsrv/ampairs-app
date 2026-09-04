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
    // Combinable filters (all AND-ed together with the search query).
    val zoneFilter: String? = null,                 // zonalOfficeId
    val cityFilter: String? = null,
    // Facet options derived from the current outlets (for the zone/city dropdowns).
    val availableZones: List<Pair<String, String>> = emptyList(),  // (zonalOfficeId, label)
    val availableCities: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val activeFilterCount: Int
        get() = (if (zoneFilter != null) 1 else 0) + (if (cityFilter != null) 1 else 0)
}

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
                applyFilter()   // refresh zone-facet labels now that office names are known
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

    fun onZoneFilter(zoneId: String?) {
        _uiState.update { it.copy(zoneFilter = zoneId) }
        applyFilter()
    }

    fun onCityFilter(city: String?) {
        _uiState.update { it.copy(cityFilter = city) }
        applyFilter()
    }

    fun clearFilters() {
        _uiState.update { it.copy(zoneFilter = null, cityFilter = null) }
        applyFilter()
    }

    private fun applyFilter() {
        val s = _uiState.value
        val q = s.query.trim()
        val zoneNames = s.zonalOfficeNames

        // Refresh the facet lists from the current outlets (kept in sync with the zone/city pickers).
        val availableCities = all.map { it.city }.filter { it.isNotBlank() }.distinct().sorted()
        val availableZones = all.map { it.zonalOfficeId }.filter { it.isNotBlank() }.distinct()
            .map { id -> id to (zoneNames[id] ?: id) }.sortedBy { it.second }

        val filtered = all.filter {
            (s.zoneFilter == null || it.zonalOfficeId == s.zoneFilter) &&
                (s.cityFilter == null || it.city == s.cityFilter) &&
                (q.isBlank() || it.code.contains(q, true) || it.name.contains(q, true) || it.city.contains(q, true))
        }
        _uiState.update {
            it.copy(
                stores = filtered,
                availableZones = availableZones,
                availableCities = availableCities,
                error = null,
            )
        }
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
