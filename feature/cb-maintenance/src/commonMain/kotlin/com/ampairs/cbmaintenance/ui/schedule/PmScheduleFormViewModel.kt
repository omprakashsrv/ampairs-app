package com.ampairs.cbmaintenance.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmScheduleRepository
import com.ampairs.cbmaintenance.data.repository.TicketBucketRepository
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.cbmaintenance.domain.model.TicketBucket
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PM_SCHEDULE_UID_PREFIX = "PMS"

/** Frequency units the backend `PmSchedule.frequencyUnit` accepts. */
object FrequencyUnits {
    val ALL = listOf("DAY", "WEEK", "MONTH", "YEAR")
}

data class PmScheduleFormUiState(
    // Full taxonomy cascade (Department › Category › Issue [› Issue-detail]) from the ticket-bucket
    // catalog. The chosen leaf resolves to a ticket_bucket uid, stored on the schedule.
    val department: String = "",
    val assetCategory: String = "",     // = the taxonomy "category" level
    val subCategory1: String = "",      // = the taxonomy "issue" level
    val subCategory2: String = "",      // = the taxonomy "issue-detail" level (only some leaves)
    val taskName: String = "",
    val frequencyUnit: String = "MONTH",
    val frequencyInterval: String = "1",
    val buckets: List<TicketBucket> = emptyList(),
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    // Derived cascade options (distinct, sorted), each narrowed by the selection above it.
    val departmentOptions: List<String>
        get() = buckets.map { it.department }.filter { it.isNotBlank() }.distinct().sorted()

    val categoryOptions: List<String>
        get() = buckets.filter { it.department == department }
            .map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    val subCategory1Options: List<String>
        get() = buckets.filter { it.department == department && it.category == assetCategory }
            .map { it.subCategory1 }.filter { it.isNotBlank() }.distinct().sorted()

    val subCategory2Options: List<String>
        get() = buckets.filter {
            it.department == department && it.category == assetCategory && it.subCategory1 == subCategory1
        }.map { it.subCategory2 }.filter { it.isNotBlank() }.distinct().sorted()

    val isValid: Boolean
        get() = assetCategory.isNotBlank() && taskName.isNotBlank() &&
            (frequencyInterval.toIntOrNull()?.let { it > 0 } == true) &&
            // When the catalog has an Issue level for this category, require it (full-leaf link);
            // categories with no deeper level stay valid at category granularity.
            (subCategory1Options.isEmpty() || subCategory1.isNotBlank())

    /** The exact ticket_bucket leaf the current cascade selection maps to, if any. */
    fun resolveBucketId(): String? = buckets.firstOrNull {
        it.department == department && it.category == assetCategory &&
            it.subCategory1 == subCategory1 && it.subCategory2 == subCategory2
    }?.uid
}

@AssistedInject
class PmScheduleFormViewModel(
    @Assisted private val scheduleId: String?,
    private val repository: PmScheduleRepository,
    private val ticketBucketRepository: TicketBucketRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PmScheduleFormUiState(isEdit = scheduleId != null))
    val uiState: StateFlow<PmScheduleFormUiState> = _uiState.asStateFlow()

    // When editing, the schedule's stored leaf id — used to back-fill the cascade once the
    // catalog has loaded (the schedule only stores the leaf uid, not the level strings).
    private var pendingBucketId: String? = null

    init {
        ticketBucketRepository.observeBuckets()
            .onEach { list ->
                _uiState.update { state ->
                    val backfilled = pendingBucketId
                        ?.let { id -> list.firstOrNull { it.uid == id } }
                        ?.takeIf { state.subCategory1.isBlank() }
                    if (backfilled != null) {
                        pendingBucketId = null
                        state.copy(
                            buckets = list,
                            department = backfilled.department,
                            assetCategory = backfilled.category,
                            subCategory1 = backfilled.subCategory1,
                            subCategory2 = backfilled.subCategory2,
                        )
                    } else {
                        state.copy(buckets = list)
                    }
                }
            }
            .launchIn(viewModelScope)
        // Make sure the classification catalog is present (pull-only reference data).
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_TICKET_BUCKET))

        if (scheduleId != null) {
            viewModelScope.launch {
                repository.getSchedule(scheduleId)?.let { s ->
                    pendingBucketId = s.ticketBucketId
                    _uiState.update {
                        it.copy(
                            department = s.department,
                            assetCategory = s.assetCategory,
                            taskName = s.taskName,
                            frequencyUnit = s.frequencyUnit,
                            frequencyInterval = s.frequencyInterval.toString(),
                        )
                    }
                }
            }
        }
    }

    // Selecting a higher cascade level clears every lower level so the selection stays consistent.
    fun onDepartment(v: String) =
        _uiState.update { it.copy(department = v, assetCategory = "", subCategory1 = "", subCategory2 = "") }

    fun onCategory(v: String) =
        _uiState.update { it.copy(assetCategory = v, subCategory1 = "", subCategory2 = "") }

    fun onSubCategory1(v: String) = _uiState.update { it.copy(subCategory1 = v, subCategory2 = "") }
    fun onSubCategory2(v: String) = _uiState.update { it.copy(subCategory2 = v) }
    fun onTaskName(v: String) = _uiState.update { it.copy(taskName = v) }
    fun onFrequencyUnit(v: String) = _uiState.update { it.copy(frequencyUnit = v) }
    fun onFrequencyInterval(v: String) = _uiState.update { it.copy(frequencyInterval = v.filter { c -> c.isDigit() }) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Equipment, issue, task and a positive frequency are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val uid = scheduleId ?: UidGenerator.generateUid(PM_SCHEDULE_UID_PREFIX)
            val result = repository.saveSchedule(
                PmSchedule(
                    uid = uid,
                    department = state.department.trim(),
                    assetCategory = state.assetCategory.trim(),
                    ticketBucketId = state.resolveBucketId(),
                    taskName = state.taskName.trim(),
                    frequencyUnit = state.frequencyUnit,
                    frequencyInterval = state.frequencyInterval.toIntOrNull() ?: 1,
                    active = true,
                ),
            )
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, saved = true)
                else it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save schedule")
            }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(scheduleId: String?): PmScheduleFormViewModel
    }
}
