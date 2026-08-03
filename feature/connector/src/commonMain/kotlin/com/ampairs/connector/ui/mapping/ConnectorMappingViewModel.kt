package com.ampairs.connector.ui.mapping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.connector.data.api.ConnectorApi
import com.ampairs.connector.domain.FieldMappingDto
import com.ampairs.connector.domain.FieldMappingRuleDto
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One entity type's editable mapping (external field → Ampairs field, with optional transform). */
data class EntityMappingUi(
    val entityType: String,
    val rules: List<FieldMappingRuleDto>,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

data class ConnectorMappingUiState(
    val loading: Boolean = true,
    val entities: List<EntityMappingUi> = emptyList(),
    val error: String? = null,
)

/**
 * Drives the field-mapping editor (spec 029 FR-U04 / task T028b) — per entity type, view/add/edit/
 * remove mapping rows and save via [ConnectorApi.updateMapping]. Serves both hosting types (the rules
 * are the same shape whether the source is Tally or a server-side connector).
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class ConnectorMappingViewModel(
    private val api: ConnectorApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectorMappingUiState())
    val uiState: StateFlow<ConnectorMappingUiState> = _uiState.asStateFlow()

    private var installationUid: String = ""

    fun load(installationUid: String) {
        this.installationUid = installationUid
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val mappings = runCatching { api.mappings(installationUid).data }.getOrNull().orEmpty()
            val entities = mappings.map { EntityMappingUi(entityType = it.entityType, rules = it.rules) }
            _uiState.update { it.copy(loading = false, entities = entities) }
        }
    }

    fun onRuleChange(entityType: String, index: Int, rule: FieldMappingRuleDto) {
        mutateEntity(entityType) { e ->
            e.copy(rules = e.rules.mapIndexed { i, r -> if (i == index) rule else r }, saved = false)
        }
    }

    fun addRule(entityType: String) {
        mutateEntity(entityType) { e -> e.copy(rules = e.rules + FieldMappingRuleDto(), saved = false) }
    }

    fun removeRule(entityType: String, index: Int) {
        mutateEntity(entityType) { e -> e.copy(rules = e.rules.filterIndexed { i, _ -> i != index }, saved = false) }
    }

    fun saveEntity(entityType: String) {
        val entity = _uiState.value.entities.firstOrNull { it.entityType == entityType } ?: return
        viewModelScope.launch {
            mutateEntity(entityType) { it.copy(saving = true, saved = false) }
            _uiState.update { it.copy(error = null) }
            val dto = FieldMappingDto(installationUid = installationUid, entityType = entityType, rules = entity.rules)
            val resp = runCatching { api.updateMapping(installationUid, dto) }.getOrNull()
            if (resp?.data != null && resp.error == null) {
                mutateEntity(entityType) { it.copy(saving = false, saved = true, rules = resp.data!!.rules) }
            } else {
                mutateEntity(entityType) { it.copy(saving = false) }
                _uiState.update { it.copy(error = resp?.error?.message ?: "Save failed") }
            }
        }
    }

    private fun mutateEntity(entityType: String, transform: (EntityMappingUi) -> EntityMappingUi) {
        _uiState.update { s ->
            s.copy(entities = s.entities.map { if (it.entityType == entityType) transform(it) else it })
        }
    }
}
