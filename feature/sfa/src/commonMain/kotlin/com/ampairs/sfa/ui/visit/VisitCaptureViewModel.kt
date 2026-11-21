package com.ampairs.sfa.ui.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.sfa.data.repository.SfaRepository
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.util.SfaConstants
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class VisitCaptureState(
    val outcome: String = "PRODUCTIVE",
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Outcomes the rep can record (mirrors the server VisitOutcome enum). */
val VISIT_OUTCOMES = listOf("PRODUCTIVE", "NO_ORDER", "OUTLET_CLOSED")

@OptIn(ExperimentalTime::class)
@AssistedInject
class VisitCaptureViewModel(
    private val sfaRepository: SfaRepository,
    @Assisted private val plannedVisitUid: String?,
    @Assisted private val customerUid: String,
    @Assisted private val repMemberUid: String,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(
            plannedVisitUid: String?,
            customerUid: String,
            repMemberUid: String,
        ): VisitCaptureViewModel
    }

    private val _state = MutableStateFlow(VisitCaptureState())
    val state: StateFlow<VisitCaptureState> = _state.asStateFlow()

    fun selectOutcome(value: String) = _state.update { it.copy(outcome = value) }
    fun updateNotes(value: String) = _state.update { it.copy(notes = value) }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val current = _state.value
            val visit = Visit(
                uid = UidGenerator.generateUid(SfaConstants.VISIT_UID_PREFIX),
                customerUid = customerUid,
                repMemberUid = repMemberUid,
                plannedVisitUid = plannedVisitUid,
                outcome = current.outcome,
                geoFenceStatus = "NO_LOCATION",
                adHoc = plannedVisitUid == null,
                notes = current.notes.trim().ifBlank { null },
                visitedAt = Clock.System.now().toString(),
            )
            val result = sfaRepository.saveVisit(visit)
            if (result.isFailure) {
                _state.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save visit") }
                return@launch
            }
            // Mark the originating planned visit as VISITED (best-effort).
            if (plannedVisitUid != null) {
                sfaRepository.getPlannedVisitById(plannedVisitUid)?.let { pv ->
                    sfaRepository.savePlannedVisit(pv.copy(status = "VISITED"))
                }
            }
            onSuccess()
        }
    }
}
