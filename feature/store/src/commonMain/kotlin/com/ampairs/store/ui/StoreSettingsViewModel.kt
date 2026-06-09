package com.ampairs.store.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.store.data.repository.StoreSettingRepository
import com.ampairs.store.domain.definition.StoreSettingDefinition
import com.ampairs.store.domain.model.StoreSetting
import com.ampairs.store.util.StoreConstants
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A single setting row: its definition plus the currently effective value. */
data class SettingRow(
    val definition: StoreSettingDefinition,
    val value: String,
    val isOverridden: Boolean,
)

data class StoreSettingsUiState(
    val rows: List<SettingRow> = emptyList(),
    val modules: List<String> = emptyList(),
    val selectedModule: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val visibleRows: List<SettingRow>
        get() = if (selectedModule == null) rows else rows.filter { it.definition.module == selectedModule }
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class StoreSettingsViewModel(
    private val repository: StoreSettingRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreSettingsUiState())
    val uiState: StateFlow<StoreSettingsUiState> = _uiState.asStateFlow()

    init {
        observeRows()
        syncService.observeEntity(SyncEntity.STORE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        // Pull overrides + fetch the definition catalog for this workspace's installed modules.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.STORE))
        refreshDefinitions()
    }

    fun selectModule(module: String?) {
        _uiState.update { it.copy(selectedModule = module) }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.STORE))
        refreshDefinitions()
    }

    private fun refreshDefinitions() {
        viewModelScope.launch { repository.refreshDefinitions() }
    }

    /** Set a new value for a setting, generating a UID for first-time overrides. */
    fun setValue(definition: StoreSettingDefinition, newValue: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val existing = repository.getByModuleKey(definition.module, definition.key)
            val setting = StoreSetting(
                uid = existing?.uid?.takeIf { it.isNotBlank() }
                    ?: UidGenerator.generateUid(StoreConstants.SETTING_UID_PREFIX),
                module = definition.module,
                key = definition.key,
                value = newValue,
                valueType = definition.valueType.name,
                active = true,
                refId = existing?.refId,
                createdAt = existing?.createdAt,
                updatedAt = existing?.updatedAt,
            )
            val result = repository.upsert(setting)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to save setting")
                }
            }
        }
    }

    /** Merge the cached definition catalog with the synced overrides into renderable rows. */
    private fun observeRows() {
        combine(
            repository.observeDefinitions(),
            repository.observeSettings(),
        ) { definitions, overrides ->
            definitions.map { def ->
                val override = overrides.firstOrNull { it.module == def.module && it.key == def.key }
                SettingRow(
                    definition = def,
                    value = override?.value ?: def.defaultValue,
                    isOverridden = override != null,
                )
            }
        }
            .onEach { rows ->
                _uiState.update {
                    it.copy(
                        rows = rows,
                        modules = rows.map { r -> r.definition.module }.distinct(),
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
