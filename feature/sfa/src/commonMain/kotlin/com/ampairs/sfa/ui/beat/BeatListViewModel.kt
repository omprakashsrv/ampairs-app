package com.ampairs.sfa.ui.beat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sfa.data.repository.SfaRepository
import com.ampairs.sfa.domain.model.Beat
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

data class BeatListUiState(
    val beats: List<Beat> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class BeatListViewModel(
    private val sfaRepository: SfaRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BeatListUiState())
    val uiState: StateFlow<BeatListUiState> = _uiState.asStateFlow()

    init {
        sfaRepository.observeBeats()
            .onEach { beats -> _uiState.update { it.copy(beats = beats) } }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.SFA_BEAT)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.SFA_BEAT))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.SFA_BEAT))
    }

    fun deleteBeat(beatId: String) {
        viewModelScope.launch {
            val result = sfaRepository.deleteBeat(beatId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete beat") }
            }
        }
    }
}
