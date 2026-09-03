package com.ampairs.cbmaintenance.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmEntryRepository
import com.ampairs.cbmaintenance.data.repository.PmScheduleRepository
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.cbstore.data.repository.StoreLookup
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** One line of the PM compliance matrix: an (outlet, asset, task) with a done-mark per month. */
data class PmReportRow(
    val outlet: String,
    val city: String,
    val asset: String,
    val task: String,
    val freq: String,
    val months: List<Boolean>, // 12 entries, Jan..Dec — true = a PM was completed that month
)

data class PmReportUiState(
    val year: String = "",
    val rows: List<PmReportRow> = emptyList(),
    val loading: Boolean = true,
    val query: String = "",
) {
    val filteredRows: List<PmReportRow>
        get() = if (query.isBlank()) rows else rows.filter {
            it.outlet.contains(query, true) || it.city.contains(query, true) ||
                it.asset.contains(query, true) || it.task.contains(query, true)
        }
}

/**
 * Builds the "PM before/compliance" report entirely from locally-synced data: completed PM entries
 * grouped by (store, asset, schedule), joined to the schedule's task/frequency and the store's
 * outlet code + city, with a done-mark in each month of the current year. Read-only.
 */
@OptIn(ExperimentalTime::class)
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PmReportViewModel(
    private val pmEntryRepository: PmEntryRepository,
    private val pmScheduleRepository: PmScheduleRepository,
    private val storeLookup: StoreLookup,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PmReportUiState())
    val uiState: StateFlow<PmReportUiState> = _uiState.asStateFlow()

    private val currentYear: String = Clock.System.now().toString().take(4)

    init {
        _uiState.update { it.copy(year = currentYear) }
        // Refresh the underlying data.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_PM_ENTRY))
        viewModelScope.launch {
            val storesById = storeLookup.activeStores().associateBy { it.uid }
            combine(
                pmEntryRepository.observeAllEntries(),
                pmScheduleRepository.observeSchedules(),
            ) { entries, schedules ->
                buildRows(entries, schedules.associateBy { it.uid }, storesById)
            }
                .onEach { rows -> _uiState.update { it.copy(rows = rows, loading = false) } }
                .launchIn(viewModelScope)
        }
    }

    fun onSearch(v: String) = _uiState.update { it.copy(query = v) }

    fun refresh() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_PM_ENTRY))

    private fun buildRows(
        entries: List<PmEntry>,
        schedulesById: Map<String, PmSchedule>,
        storesById: Map<String, Store>,
    ): List<PmReportRow> {
        // group key -> 12-month done flags
        data class Key(val storeId: String, val asset: String, val scheduleId: String)
        val grid = LinkedHashMap<Key, BooleanArray>()
        for (e in entries) {
            if (e.status != "DONE") continue
            val done = e.completedAt ?: continue
            if (done.take(4) != currentYear) continue
            val monthIdx = done.substringAfter('-', "").take(2).toIntOrNull()?.minus(1) ?: continue
            if (monthIdx !in 0..11) continue
            val key = Key(e.storeId, e.assetCategory, e.pmScheduleId ?: "")
            grid.getOrPut(key) { BooleanArray(12) }[monthIdx] = true
        }
        return grid.map { (key, flags) ->
            val store = storesById[key.storeId]
            val schedule = key.scheduleId.takeIf { it.isNotBlank() }?.let { schedulesById[it] }
            PmReportRow(
                outlet = store?.let { "${it.code} · ${it.name}" } ?: key.storeId,
                city = store?.city ?: "",
                asset = key.asset,
                task = schedule?.taskName ?: "Ad-hoc",
                freq = schedule?.let { "${it.frequencyInterval} ${it.frequencyUnit.lowercase()}" } ?: "",
                months = flags.toList(),
            )
        }.sortedWith(compareBy({ it.outlet }, { it.asset }, { it.task }))
    }
}
