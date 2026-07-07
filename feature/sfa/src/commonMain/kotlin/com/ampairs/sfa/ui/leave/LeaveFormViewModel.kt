package com.ampairs.sfa.ui.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.sfa.data.repository.SfaRepository
import com.ampairs.sfa.domain.model.Leave
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class LeaveFormState(
    val uid: String = "",
    val repMemberUid: String = "",
    val markedBy: String = "",
    val leaveDate: String = "",
    val reason: String = "",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalTime::class)
@AssistedInject
class LeaveFormViewModel(
    private val sfaRepository: SfaRepository,
    private val tokenRepository: TokenRepository,
    @Assisted private val leaveId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(leaveId: String?): LeaveFormViewModel
    }

    private val _formState = MutableStateFlow(LeaveFormState(leaveDate = todayIso()))
    val formState: StateFlow<LeaveFormState> = _formState.asStateFlow()

    init {
        if (leaveId != null) loadLeave(leaveId)
    }

    private fun loadLeave(id: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val leave = sfaRepository.getLeaveById(id)
            if (leave != null) {
                _formState.update {
                    it.copy(
                        uid = leave.uid,
                        repMemberUid = leave.repMemberUid,
                        markedBy = leave.markedBy ?: "",
                        leaveDate = leave.leaveDate,
                        reason = leave.reason ?: "",
                        active = leave.active,
                        isLoading = false,
                    )
                }
            } else {
                _formState.update { it.copy(isLoading = false, error = "Leave not found") }
            }
        }
    }

    fun updateLeaveDate(value: String) = _formState.update { it.copy(leaveDate = value) }
    fun updateReason(value: String) = _formState.update { it.copy(reason = value) }

    fun save(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.leaveDate.isBlank()) {
            _formState.update { it.copy(error = "Date is required") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, error = null) }
            val currentUser = tokenRepository.getCurrentUserId().orEmpty()
            val uid = state.uid.ifBlank { UidGenerator.generateUid(SfaConstants.LEAVE_UID_PREFIX) }
            val leave = Leave(
                uid = uid,
                repMemberUid = state.repMemberUid.ifBlank { currentUser },
                leaveDate = state.leaveDate,
                reason = state.reason.trim().ifBlank { null },
                markedBy = state.markedBy.ifBlank { currentUser },
                active = state.active,
            )
            val result = sfaRepository.saveLeave(leave)
            if (result.isSuccess) {
                onSuccess()
            } else {
                _formState.update {
                    it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save leave")
                }
            }
        }
    }

    private fun todayIso(): String =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}
