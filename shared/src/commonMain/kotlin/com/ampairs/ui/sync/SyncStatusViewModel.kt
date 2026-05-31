package com.ampairs.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.EntitySyncState
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncLogEntry
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SyncStatusUiState(
    val entityStates: List<EntitySyncState> = emptyList(),
    val logs: List<SyncLogEntry> = emptyList(),
)

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class SyncStatusViewModel(
    private val syncService: CentralSyncService,
) : ViewModel() {

    val entityStates: StateFlow<List<EntitySyncState>> = syncService.syncStates
        .map { map -> SyncEntity.entries.mapNotNull { map[it] } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<SyncLogEntry>> = syncService.syncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun triggerFullSync() {
        syncService.emit(SyncEvent.TriggerFullSync(entity = null))
    }

    fun triggerEntitySync(entity: SyncEntity) {
        syncService.emit(SyncEvent.TriggerFullSync(entity))
    }
}
