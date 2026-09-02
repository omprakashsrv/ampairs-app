package com.ampairs.cbmaintenance.ui.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.TicketRepository
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbstore.data.repository.StoreLookup
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

data class TicketListUiState(
    val tickets: List<Ticket> = emptyList(),
    val storeNames: Map<String, String> = emptyMap(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class TicketListViewModel(
    private val repository: TicketRepository,
    private val storeLookup: StoreLookup,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketListUiState())
    val uiState: StateFlow<TicketListUiState> = _uiState.asStateFlow()

    private var all: List<Ticket> = emptyList()

    init {
        repository.observeTickets()
            .onEach { tickets -> all = tickets; applyFilter() }
            .catch { e -> _uiState.update { it.copy(error = e.message) } }
            .launchIn(viewModelScope)

        // Resolve store id → "CODE · Name" for display (a ticket only carries the store id).
        viewModelScope.launch {
            val names = storeLookup.activeStores().associate { it.uid to "${it.code} · ${it.name}" }
            _uiState.update { it.copy(storeNames = names) }
        }

        syncService.observeEntity(SyncEntity.CB_TICKET)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_TICKET))
    }

    fun onSearch(q: String) {
        _uiState.update { it.copy(query = q) }
        applyFilter()
    }

    private fun applyFilter() {
        val q = _uiState.value.query.trim()
        val storeNames = _uiState.value.storeNames
        val filtered = if (q.isBlank()) all else all.filter { t ->
            t.assetCategory.contains(q, true) ||
                t.subCategory.contains(q, true) ||
                t.status.contains(q, true) ||
                (t.description?.contains(q, true) == true) ||
                (storeNames[t.storeId]?.contains(q, true) == true)
        }
        _uiState.update { it.copy(tickets = filtered, error = null) }
    }

    fun refresh() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_TICKET))
}
