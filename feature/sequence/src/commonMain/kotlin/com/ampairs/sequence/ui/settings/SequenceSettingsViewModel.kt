package com.ampairs.sequence.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sequence.data.repository.SequenceRepository
import com.ampairs.sequence.domain.model.SequenceDefinition
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

data class SequenceSettingsUiState(
    val definitions: List<SequenceDefinition> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class SequenceSettingsViewModel(
    private val repository: SequenceRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SequenceSettingsUiState())
    val uiState: StateFlow<SequenceSettingsUiState> = _uiState.asStateFlow()

    init {
        repository.observeActiveDefinitions()
            .onEach { definitions ->
                _uiState.update { it.copy(definitions = definitions, isLoading = false) }
            }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.SEQUENCE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.SEQUENCE))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.SEQUENCE))
    }
}
