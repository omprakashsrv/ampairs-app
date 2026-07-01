package com.ampairs.sfa.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.sfa.data.repository.SfaRepository
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.util.SfaConstants
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class AttendanceUiState(
    val openRecord: Attendance? = null,
    val recent: List<Attendance> = emptyList(),
    val isSaving: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * Rep check-in / check-out. Offline-first: a check-in writes an OPEN [Attendance] locally; the
 * check-out replaces it (same uid) with a CLOSED copy. `rep_member_uid` is the current user id
 * (see plan decision — MVP, no member-uid lookup). All server traffic runs through
 * [com.ampairs.sfa.sync.AttendanceSyncDelegate].
 */
@OptIn(ExperimentalTime::class)
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class AttendanceViewModel(
    private val sfaRepository: SfaRepository,
    private val tokenRepository: TokenRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        sfaRepository.observeAttendance()
            .onEach { rows ->
                _uiState.update {
                    it.copy(
                        openRecord = rows.firstOrNull { r -> r.active && r.status == "OPEN" },
                        recent = rows,
                    )
                }
            }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.SFA_ATTENDANCE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.SFA_ATTENDANCE))
    }

    fun checkIn() {
        if (_uiState.value.openRecord != null || _uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val record = Attendance(
                uid = UidGenerator.generateUid(SfaConstants.ATTENDANCE_UID_PREFIX),
                repMemberUid = tokenRepository.getCurrentUserId().orEmpty(),
                checkInAt = Clock.System.now().toString(),
                status = "OPEN",
            )
            val result = sfaRepository.saveAttendance(record)
            _uiState.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun checkOut() {
        val open = _uiState.value.openRecord ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val closed = open.copy(checkOutAt = Clock.System.now().toString(), status = "CLOSED")
            val result = sfaRepository.saveAttendance(closed)
            _uiState.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.SFA_ATTENDANCE))
    }
}
