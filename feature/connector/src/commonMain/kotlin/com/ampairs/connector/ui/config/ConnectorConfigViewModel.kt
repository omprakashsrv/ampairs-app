package com.ampairs.connector.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.connector.data.api.ConnectorApi
import com.ampairs.connector.domain.ConfigUpdateRequest
import com.ampairs.connector.domain.ConnectionTestRequest
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One editable connection field, derived from the connector's connection schema + stored config. */
data class ConfigFieldUi(
    val key: String,
    val label: String,
    val secret: Boolean,
    val required: Boolean,
    val value: String,
    val secretIsSet: Boolean,
)

data class ConnectorConfigUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val fields: List<ConfigFieldUi> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
    val testing: Boolean = false,
    val testOk: Boolean? = null,
    val testMessage: String? = null,
)

/**
 * Drives the generic connection-config form (spec 029 FR-U02/U03). The form is generated from the
 * connector's `connectionSchema` (secret + non-secret fields); secrets are write-only — a saved secret
 * shows as "set" and is only overwritten when a new value is typed. Saves to the backend via
 * [ConnectorApi.updateConfig] (replacing the old local-DataStore-only path).
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class ConnectorConfigViewModel(
    private val api: ConnectorApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectorConfigUiState())
    val uiState: StateFlow<ConnectorConfigUiState> = _uiState.asStateFlow()

    private var installationUid: String = ""
    private var connectorType: String = ""

    fun load(installationUid: String, connectorType: String) {
        this.installationUid = installationUid
        this.connectorType = connectorType
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, saved = false, testOk = null, testMessage = null) }
            val schema = runCatching { api.catalogue().data }.getOrNull()
                ?.firstOrNull { it.type.equals(connectorType, ignoreCase = true) }
                ?.connectionSchema.orEmpty()
            val config = runCatching { api.config(installationUid).data }.getOrNull()
            val nonSecret = config?.nonSecretValues ?: emptyMap()
            val secretKeys = config?.secretKeysSet ?: emptyList()

            val fields = if (schema.isNotEmpty()) {
                schema.map { f ->
                    ConfigFieldUi(
                        key = f.key,
                        label = f.label.ifBlank { f.key },
                        secret = f.secret,
                        required = f.required,
                        value = if (f.secret) "" else (nonSecret[f.key] ?: ""),
                        secretIsSet = f.secret && secretKeys.contains(f.key),
                    )
                }
            } else {
                // No schema (offline / unknown type) — fall back to editing the existing non-secret keys.
                nonSecret.map { (k, v) -> ConfigFieldUi(k, k, secret = false, required = false, value = v, secretIsSet = false) }
            }
            _uiState.update { it.copy(loading = false, fields = fields) }
        }
    }

    fun onFieldChange(key: String, value: String) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.key == key) it.copy(value = value) else it }, saved = false) }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null, saved = false) }
            val fields = _uiState.value.fields
            val nonSecretValues = fields.filterNot { it.secret }.associate { it.key to it.value }
            // Only send secrets the user actually typed — blank means "keep the stored value".
            val secretValues = fields.filter { it.secret && it.value.isNotBlank() }.associate { it.key to it.value }
            val resp = runCatching {
                api.updateConfig(installationUid, ConfigUpdateRequest(nonSecretValues, secretValues))
            }.getOrNull()
            if (resp?.data != null && resp.error == null) {
                _uiState.update { it.copy(saving = false, saved = true) }
                load(installationUid, connectorType) // refresh (secrets now show as "set")
            } else {
                _uiState.update { it.copy(saving = false, error = resp?.error?.message ?: "Save failed") }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(testing = true, testOk = null, testMessage = null) }
            val resp = runCatching {
                api.testConnection(installationUid, ConnectionTestRequest(ok = true))
            }.getOrNull()
            val result = resp?.data
            _uiState.update { it.copy(testing = false, testOk = result?.ok, testMessage = result?.message) }
        }
    }
}
