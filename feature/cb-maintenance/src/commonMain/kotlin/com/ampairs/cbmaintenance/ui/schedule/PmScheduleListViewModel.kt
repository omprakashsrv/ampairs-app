package com.ampairs.cbmaintenance.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmScheduleRepository
import com.ampairs.cbmaintenance.domain.model.PmSchedule
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

data class PmScheduleListUiState(
    val schedules: List<PmSchedule> = emptyList(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PmScheduleListViewModel(
    private val repository: PmScheduleRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PmScheduleListUiState())
    val uiState: StateFlow<PmScheduleListUiState> = _uiState.asStateFlow()

    private var all: List<PmSchedule> = emptyList()

    init {
        repository.observeSchedules()
            .onEach { schedules -> all = schedules; applyFilter() }
            .catch { e -> _uiState.update { it.copy(error = e.message) } }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.CB_PM_SCHEDULE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_PM_SCHEDULE))
    }

    fun onSearch(q: String) {
        _uiState.update { it.copy(query = q) }
        applyFilter()
    }

    private fun applyFilter() {
        val q = _uiState.value.query.trim()
        val filtered = if (q.isBlank()) all else all.filter {
            it.assetCategory.contains(q, true) || it.taskName.contains(q, true)
        }
        _uiState.update { it.copy(schedules = filtered, error = null) }
    }

    fun refresh() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_PM_SCHEDULE))
}
