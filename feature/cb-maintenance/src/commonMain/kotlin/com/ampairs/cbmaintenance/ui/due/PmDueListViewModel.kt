package com.ampairs.cbmaintenance.ui.due

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmEntryRepository
import com.ampairs.cbmaintenance.data.repository.TicketRepository
import com.ampairs.cbmaintenance.domain.model.ChecklistItemResult
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbemployee.data.repository.EmployeeLookup
import com.ampairs.cbemployee.domain.model.Employee
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

data class PmDueListUiState(
    val entries: List<PmEntry> = emptyList(),
    val employees: List<Employee> = emptyList(),
    // Resolve raw ids to human labels for display: storeId -> "CODE · Name", ticketId -> "asset · issue".
    val storeLabels: Map<String, String> = emptyMap(),
    val ticketLabels: Map<String, String> = emptyMap(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PmDueListViewModel(
    private val repository: PmEntryRepository,
    private val employeeLookup: EmployeeLookup,
    private val storeLookup: StoreLookup,
    private val ticketRepository: TicketRepository,
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

        // Employee roster feeds the "Done by" / "Assisted by" pickers on completion.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_EMPLOYEE))
        loadEmployees()
        syncService.observeEntity(SyncEntity.CB_EMPLOYEE)
            .onEach { state -> if (state?.status is SyncStatus.Success) loadEmployees() }
            .launchIn(viewModelScope)

        // Store labels (outlet code · name) for the card, refreshed when the store feed syncs.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_STORE))
        loadStores()
        syncService.observeEntity(SyncEntity.CB_STORE)
            .onEach { state -> if (state?.status is SyncStatus.Success) loadStores() }
            .launchIn(viewModelScope)

        // Ticket labels (asset · issue) so a PM raised for a ticket reads meaningfully.
        ticketRepository.observeTickets()
            .onEach { tickets ->
                val labels = tickets.associate { t ->
                    t.uid to listOf(t.assetCategory, t.subCategory).filter { it.isNotBlank() }.joinToString(" · ")
                }
                _uiState.update { it.copy(ticketLabels = labels) }
            }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_PM_ENTRY))
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            runCatching { employeeLookup.activeEmployees() }
                .onSuccess { list -> _uiState.update { it.copy(employees = list) } }
        }
    }

    private fun loadStores() {
        viewModelScope.launch {
            runCatching { storeLookup.activeStores() }
                .onSuccess { stores ->
                    val labels = stores.associate { s ->
                        s.uid to listOf(s.code, s.name).filter { it.isNotBlank() }.joinToString(" · ")
                    }
                    _uiState.update { it.copy(storeLabels = labels) }
                }
        }
    }

    fun onSearch(q: String) {
        _uiState.update { it.copy(query = q) }
        applyFilter()
    }

    private fun applyFilter() {
        val q = _uiState.value.query.trim()
        val labels = _uiState.value.storeLabels
        val filtered = if (q.isBlank()) all else all.filter {
            it.assetCategory.contains(q, true) || it.status.contains(q, true) ||
                (labels[it.storeId] ?: it.storeId).contains(q, true)
        }
        _uiState.update { it.copy(entries = filtered, error = null) }
    }

    fun refresh() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_PM_ENTRY))

    /**
     * Assign (or self-assign) a due PM to an employee. Offline-first: writes assignedToEmployeeId
     * locally + marks pending; the server accepts it on upsert. Any employee in the same zone can
     * do this — the picker is scoped to the entry's zone in the UI.
     */
    fun assign(entryId: String, employeeId: String) {
        if (employeeId.isBlank()) return
        viewModelScope.launch {
            val result = repository.reassignEntry(entryId, employeeId)
            if (result.isFailure) reportError(result.exceptionOrNull())
        }
    }

    /** Complete with everything passing — no ticket spawned. Records who did it and who helped. */
    fun markAllOk(entryId: String, doneById: String?, assistedByIds: List<String>) {
        viewModelScope.launch {
            val result = repository.completeEntry(
                entryId,
                checklistResult = emptyList(),
                completedByEmployeeId = doneById?.takeIf { it.isNotBlank() },
                assistedByEmployeeIds = assistedByIds.filter { it.isNotBlank() },
            )
            if (result.isFailure) reportError(result.exceptionOrNull())
        }
    }

    /** Complete with a failed check — the server spawns a ticket for [issue] (module plan §6). */
    fun reportIssue(entryId: String, issue: String, doneById: String?, assistedByIds: List<String>) {
        if (issue.isBlank()) return
        viewModelScope.launch {
            val result = repository.completeEntry(
                entryId,
                checklistResult = listOf(ChecklistItemResult(item = issue.trim(), passed = false)),
                completedByEmployeeId = doneById?.takeIf { it.isNotBlank() },
                assistedByEmployeeIds = assistedByIds.filter { it.isNotBlank() },
            )
            if (result.isFailure) reportError(result.exceptionOrNull())
        }
    }

    private fun reportError(t: Throwable?) =
        _uiState.update { it.copy(error = t?.message ?: "Failed to complete PM") }
}
