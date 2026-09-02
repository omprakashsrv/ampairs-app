package com.ampairs.cbmaintenance.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmScheduleRepository
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PM_SCHEDULE_UID_PREFIX = "PMS"

/** Frequency units the backend `PmSchedule.frequencyUnit` accepts. */
object FrequencyUnits {
    val ALL = listOf("DAY", "WEEK", "MONTH", "YEAR")
}

data class PmScheduleFormUiState(
    val assetCategory: String = "",
    val taskName: String = "",
    val frequencyUnit: String = "MONTH",
    val frequencyInterval: String = "1",
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean
        get() = assetCategory.isNotBlank() && taskName.isNotBlank() &&
            (frequencyInterval.toIntOrNull()?.let { it > 0 } == true)
}

@AssistedInject
class PmScheduleFormViewModel(
    @Assisted private val scheduleId: String?,
    private val repository: PmScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PmScheduleFormUiState(isEdit = scheduleId != null))
    val uiState: StateFlow<PmScheduleFormUiState> = _uiState.asStateFlow()

    init {
        if (scheduleId != null) {
            viewModelScope.launch {
                repository.getSchedule(scheduleId)?.let { s ->
                    _uiState.update {
                        it.copy(
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

    fun onAssetCategory(v: String) = _uiState.update { it.copy(assetCategory = v) }
    fun onTaskName(v: String) = _uiState.update { it.copy(taskName = v) }
    fun onFrequencyUnit(v: String) = _uiState.update { it.copy(frequencyUnit = v) }
    fun onFrequencyInterval(v: String) = _uiState.update { it.copy(frequencyInterval = v.filter { c -> c.isDigit() }) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Asset, task and a positive frequency are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val uid = scheduleId ?: UidGenerator.generateUid(PM_SCHEDULE_UID_PREFIX)
            val result = repository.saveSchedule(
                PmSchedule(
                    uid = uid,
                    assetCategory = state.assetCategory.trim(),
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
