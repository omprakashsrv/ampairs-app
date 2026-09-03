package com.ampairs.cbmaintenance.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmEntryRepository
import com.ampairs.cbmaintenance.data.repository.PmScheduleRepository
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.cbemployee.data.repository.EmployeeLookup
import com.ampairs.cbstore.data.repository.StoreLookup
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metro.ContributesIntoMap
import com.ampairs.sync.SyncStatus
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

/** Compliance state of one month cell for a report row. */
enum class MonthStatus { NONE, DONE, DUE, OVERDUE }

/** One line of the PM compliance matrix: an (outlet, asset, task) with a status-mark per month. */
data class PmReportRow(
    val outlet: String,
    val city: String,
    val asset: String,
    val task: String,
    val freq: String,
    val doneBy: String,      // distinct people who completed this row's PM work this year
    val assistedBy: String,  // distinct people who assisted on it this year
    // 12 entries, Jan..Dec. DONE = completed that month; DUE/OVERDUE = a PM scheduled that month is
    // still open (OVERDUE once its due month is in the past); NONE = nothing scheduled/done.
    val months: List<MonthStatus>,
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
                it.asset.contains(query, true) || it.task.contains(query, true) ||
                it.doneBy.contains(query, true) || it.assistedBy.contains(query, true)
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
    private val employeeLookup: EmployeeLookup,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PmReportUiState())
    val uiState: StateFlow<PmReportUiState> = _uiState.asStateFlow()

    private val currentYear: String = Clock.System.now().toString().take(4)
    // 0-based current month (UTC), matching the report's UTC month bucketing — used to split a still
    // open PM into DUE (this month / upcoming) vs OVERDUE (its due month already passed).
    private val currentMonthIdx: Int =
        Clock.System.now().toString().substring(5, 7).toIntOrNull()?.minus(1)?.coerceIn(0, 11) ?: 0

    // Store/employee lookups are one-shot suspend calls; hold them as flows so the report rebuilds
    // when they finish syncing (otherwise a snapshot taken before the pull lands stays empty).
    private val storesById = MutableStateFlow<Map<String, Store>>(emptyMap())
    private val nameById = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        _uiState.update { it.copy(year = currentYear) }
        // Refresh every feed the report joins on. Without CB_PM_SCHEDULE, entries can't resolve
        // their schedule → task shows "Ad-hoc" and Freq is blank.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_PM_ENTRY))
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_PM_SCHEDULE))
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_STORE))
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_EMPLOYEE))

        loadStores(); loadNames()
        syncService.observeEntity(SyncEntity.CB_STORE)
            .onEach { if (it?.status is SyncStatus.Success) loadStores() }.launchIn(viewModelScope)
        syncService.observeEntity(SyncEntity.CB_EMPLOYEE)
            .onEach { if (it?.status is SyncStatus.Success) loadNames() }.launchIn(viewModelScope)

        combine(
            pmEntryRepository.observeAllEntries(),
            pmScheduleRepository.observeSchedules(),
            storesById,
            nameById,
        ) { entries, schedules, stores, names ->
            buildRows(entries, schedules.associateBy { it.uid }, stores, names)
        }
            .onEach { rows -> _uiState.update { it.copy(rows = rows, loading = false) } }
            .launchIn(viewModelScope)
    }

    private fun loadStores() {
        viewModelScope.launch {
            runCatching { storeLookup.activeStores() }
                .onSuccess { list -> storesById.value = list.associateBy { it.uid } }
        }
    }

    private fun loadNames() {
        viewModelScope.launch {
            runCatching { employeeLookup.activeEmployees() }
                .onSuccess { list -> nameById.value = list.associate { it.uid to it.name.ifBlank { it.employeeNo } } }
        }
    }

    fun onSearch(v: String) = _uiState.update { it.copy(query = v) }

    fun refresh() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CB_PM_ENTRY))

    private fun buildRows(
        entries: List<PmEntry>,
        schedulesById: Map<String, PmSchedule>,
        storesById: Map<String, Store>,
        nameById: Map<String, String>,
    ): List<PmReportRow> {
        data class Key(val storeId: String, val asset: String, val scheduleId: String)
        // Per group: 12-month status cells, plus the distinct people who did / assisted the work.
        class Agg {
            val months = Array(12) { MonthStatus.NONE }
            val doneBy = LinkedHashSet<String>()
            val assistedBy = LinkedHashSet<String>()
        }
        val grid = LinkedHashMap<Key, Agg>()
        fun monthOf(iso: String?): Int? {
            val s = iso ?: return null
            if (s.take(4) != currentYear) return null
            return s.substringAfter('-', "").take(2).toIntOrNull()?.minus(1)?.takeIf { it in 0..11 }
        }
        for (e in entries) {
            val agg = grid.getOrPut(Key(e.storeId, e.assetCategory, e.pmScheduleId ?: "")) { Agg() }
            if (e.status == "DONE") {
                val monthIdx = monthOf(e.completedAt) ?: continue
                agg.months[monthIdx] = MonthStatus.DONE   // completion always wins over a pending mark
                e.completedByEmployeeId?.takeIf { it.isNotBlank() }?.let { agg.doneBy += it }
                e.assistedByEmployeeIds?.forEach { id -> id.takeIf { it.isNotBlank() }?.let { agg.assistedBy += it } }
            } else {
                // Still-open PM (DUE / OVERDUE / ASSIGNED / IN_PROGRESS) — mark its scheduled month as
                // pending, unless that month is already completed. Past due month → OVERDUE.
                val monthIdx = monthOf(e.dueDate) ?: continue
                if (agg.months[monthIdx] == MonthStatus.NONE) {
                    agg.months[monthIdx] =
                        if (monthIdx < currentMonthIdx) MonthStatus.OVERDUE else MonthStatus.DUE
                }
            }
        }
        // A group created only to look up a pending entry that fell outside the current year would be
        // all-NONE — drop those so the report shows only rows with real activity this year.
        grid.entries.retainAll { (_, agg) -> agg.months.any { it != MonthStatus.NONE } }
        fun names(ids: Set<String>): String =
            ids.map { nameById[it] ?: it }.filter { it.isNotBlank() }.joinToString(", ")
        return grid.map { (key, agg) ->
            val store = storesById[key.storeId]
            val schedule = key.scheduleId.takeIf { it.isNotBlank() }?.let { schedulesById[it] }
            PmReportRow(
                outlet = store?.let { "${it.code} · ${it.name}" } ?: key.storeId,
                city = store?.city ?: "",
                asset = key.asset,
                task = schedule?.taskName ?: "Ad-hoc",
                freq = schedule?.let { "${it.frequencyInterval} ${it.frequencyUnit.lowercase()}" } ?: "",
                doneBy = names(agg.doneBy),
                assistedBy = names(agg.assistedBy),
                months = agg.months.toList(),
            )
        }.sortedWith(compareBy({ it.outlet }, { it.asset }, { it.task }))
    }
}
