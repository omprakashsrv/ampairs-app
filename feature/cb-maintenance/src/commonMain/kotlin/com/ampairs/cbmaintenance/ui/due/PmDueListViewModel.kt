package com.ampairs.cbmaintenance.ui.due

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmEntryRepository
import com.ampairs.cbmaintenance.domain.model.ChecklistItemResult
import com.ampairs.cbmaintenance.domain.model.PmEntry
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

data class PmDueListUiState(
    val entries: List<PmEntry> = emptyList(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val isGenerating: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PmDueListViewModel(
    private val repository: PmEntryRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PmDueListUiState())
    val uiState: StateFlow<PmDueListUiState> = _uiState.asStateFlow()

    private var all: List<PmEntry> = emptyList()

    init {
        repository.observeOpenEntries()
            .onEach { entries -> all = entries; applyFilter() }
            .catch { e -> _uiState.update { it.copy(error = e.message) } }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.CB_PM_ENTRY)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_PM_ENTRY))
    }

    fun onSearch(q: String) {
        _uiState.update { it.copy(query = q) }
        applyFilter()
    }

    private fun applyFilter() {
        val q = _uiState.value.query.trim()
        val filtered = if (q.isBlank()) all else all.filter {
            it.assetCategory.contains(q, true) || it.status.contains(q, true) || it.storeId.contains(q, true)
        }
        _uiState.update { it.copy(entries = filtered, error = null) }
    }

    fun refresh() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_PM_ENTRY))

    /** Ask the server to roll due PM entries forward now, then pull them in. */
    fun generate() {
        if (_uiState.value.isGenerating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = null, error = null) }
            val result = repository.generateNow()
            _uiState.update {
                if (result.isSuccess) it.copy(isGenerating = false, message = "Generated ${result.getOrNull() ?: 0} PM entries")
                else it.copy(isGenerating = false, error = result.exceptionOrNull()?.message ?: "Failed to generate PM")
            }
            if (result.isSuccess) syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_PM_ENTRY))
        }
    }

    /** Complete with everything passing — no ticket spawned. */
    fun markAllOk(entryId: String) {
        viewModelScope.launch {
            val result = repository.completeEntry(entryId, checklistResult = emptyList())
            if (result.isFailure) reportError(result.exceptionOrNull())
        }
    }

    /** Complete with a failed check — the server spawns a ticket for [issue] (module plan §6). */
    fun reportIssue(entryId: String, issue: String) {
        if (issue.isBlank()) return
        viewModelScope.launch {
            val result = repository.completeEntry(
                entryId,
                checklistResult = listOf(ChecklistItemResult(item = issue.trim(), passed = false)),
            )
            if (result.isFailure) reportError(result.exceptionOrNull())
        }
    }

    private fun reportError(t: Throwable?) =
        _uiState.update { it.copy(error = t?.message ?: "Failed to complete PM") }
}
