package com.ampairs.notification.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.notification.data.repository.NotificationRepository
import com.ampairs.notification.domain.model.AppNotification
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationListUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/** One-off UI events (e.g. follow a notification's deep link). */
sealed interface NotificationListEvent {
    data class OpenDeepLink(val deepLink: String) : NotificationListEvent
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class NotificationListViewModel(
    private val repository: NotificationRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationListUiState())
    val uiState: StateFlow<NotificationListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotificationListEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<NotificationListEvent> = _events.asSharedFlow()

    init {
        observeNotifications()
        syncService.observeEntity(SyncEntity.NOTIFICATION)
            .onEach { state ->
                _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) }
            }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.NOTIFICATION))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.NOTIFICATION))
    }

    /** Mark the tapped notification read and surface its deep link (data_payload) if present. */
    fun onItemClick(notification: AppNotification) {
        viewModelScope.launch {
            repository.markRead(notification.uid)
            notification.dataPayload
                ?.takeIf { it.isNotBlank() }
                ?.let { _events.tryEmit(NotificationListEvent.OpenDeepLink(it)) }
        }
    }

    fun onDismiss(uid: String) {
        viewModelScope.launch {
            val result = repository.dismiss(uid)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to dismiss notification")
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead() }
    }

    private fun observeNotifications() {
        repository.observeNotifications()
            .onEach { items ->
                _uiState.update { it.copy(notifications = items, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }
}
