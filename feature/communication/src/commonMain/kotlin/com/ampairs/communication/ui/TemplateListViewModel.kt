package com.ampairs.communication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.communication.data.api.TemplateAggregateDto
import com.ampairs.communication.data.repository.TemplateRepository
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

data class TemplateListUiState(
    val templates: List<TemplateAggregateDto> = emptyList(),
    val isRefreshing: Boolean = false,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class TemplateListViewModel(
    private val repository: TemplateRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    init {
        repository.observeAll()
            .onEach { list -> _uiState.update { it.copy(templates = list) } }
            .launchIn(viewModelScope)
        syncService.observeEntity(SyncEntity.COMM_TEMPLATE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.COMM_TEMPLATE))
    }

    fun refresh() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.COMM_TEMPLATE))

    fun delete(uid: String) {
        viewModelScope.launch { repository.delete(uid) }
    }
}
