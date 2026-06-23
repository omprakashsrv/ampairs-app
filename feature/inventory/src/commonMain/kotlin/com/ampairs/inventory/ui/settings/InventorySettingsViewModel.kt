package com.ampairs.inventory.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.domain.InventorySettings
import com.ampairs.inventory.domain.InventorySettingsProvider
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

data class InventorySettingsUiState(
    val settings: InventorySettings = InventorySettings.Default,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class InventorySettingsViewModel(
    private val settingsProvider: InventorySettingsProvider,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventorySettingsUiState())
    val uiState: StateFlow<InventorySettingsUiState> = _uiState.asStateFlow()

    init {
        settingsProvider.observe()
            .onEach { s -> _uiState.update { it.copy(settings = s) } }
            .launchIn(viewModelScope)
        syncService.observeEntity(SyncEntity.STORE)
            .onEach { st -> _uiState.update { it.copy(isRefreshing = st?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.STORE))
    }

    fun setFlag(key: String, value: Boolean) {
        viewModelScope.launch {
            val result = settingsProvider.setFlag(key, value)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }
}
