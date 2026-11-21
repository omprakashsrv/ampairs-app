package com.ampairs.sfa.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sfa.data.repository.SfaRepository
import com.ampairs.sfa.domain.model.Leave
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
import kotlinx.coroutines.launch

data class LeaveListUiState(
    val leaves: List<Leave> = emptyList(),
    val isRefreshing: Boolean = false,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class LeaveListViewModel(
    private val sfaRepository: SfaRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaveListUiState())
    val uiState: StateFlow<LeaveListUiState> = _uiState.asStateFlow()

    init {
        sfaRepository.observeLeaves()
            .onEach { rows -> _uiState.update { it.copy(leaves = rows) } }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.SFA_LEAVE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.SFA_LEAVE))
    }

    fun delete(uid: String) {
        viewModelScope.launch { sfaRepository.deleteLeave(uid) }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.SFA_LEAVE))
    }
}
