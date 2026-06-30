package com.ampairs.sfa.ui.plannedvisit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sfa.data.repository.SfaRepository
import com.ampairs.sfa.domain.model.PlannedVisit
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class PlannedVisitListUiState(
    val plannedVisits: List<PlannedVisit> = emptyList(),
    val isRefreshing: Boolean = false,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PlannedVisitListViewModel(
    private val sfaRepository: SfaRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannedVisitListUiState())
    val uiState: StateFlow<PlannedVisitListUiState> = _uiState.asStateFlow()

    init {
        sfaRepository.observePlannedVisits()
            .onEach { rows -> _uiState.update { it.copy(plannedVisits = rows) } }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.SFA_PLANNED_VISIT)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.SFA_PLANNED_VISIT))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.SFA_PLANNED_VISIT))
    }
}
