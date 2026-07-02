package com.ampairs.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.notification.data.repository.NotificationRepository
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the unread notification count for the bell badge. Workspace-scoped so the count is per
 * workspace and reset on workspace switch. Triggers an initial pull so the badge reflects the
 * server feed without opening the list.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class NotificationBadgeViewModel(
    repository: NotificationRepository,
    syncService: CentralSyncService,
) : ViewModel() {

    val unreadCount: StateFlow<Int> = repository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.NOTIFICATION))
    }
}
