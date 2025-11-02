package com.ampairs.form.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.form.domain.EntityAttributeDefinition
import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.EntityFieldConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

data class FormConfigUiState(
    val entityType: String = "",
    val fieldConfigs: List<EntityFieldConfig> = emptyList(),
    val attributeDefinitions: List<EntityAttributeDefinition> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class FormConfigViewModel(
    private val entityType: String,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormConfigUiState(entityType = entityType))
    val uiState: StateFlow<FormConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        _uiState.update { it.copy(isLoading = true) }

        // Observe database for reactive updates
        configRepository.observeConfigSchema(entityType)
            .onEach { schema ->
                _uiState.update {
                    it.copy(
                        fieldConfigs = schema?.fieldConfigs ?: emptyList(),
                        attributeDefinitions = schema?.attributeDefinitions ?: emptyList(),
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)

        // Sync from backend for this specific entity type
        viewModelScope.launch {
            try {
                configRepository.getConfigSchema(entityType)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to sync config",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateFieldConfig(fieldConfig: EntityFieldConfig) {
        _uiState.update { state ->
            val updated = state.fieldConfigs.map {
                if (it.uid == fieldConfig.uid) fieldConfig else it
            }
            state.copy(fieldConfigs = updated)
        }
    }

    fun updateAttributeDefinition(attributeDefinition: EntityAttributeDefinition) {
        _uiState.update { state ->
            val updated = state.attributeDefinitions.map {
                if (it.uid == attributeDefinition.uid) attributeDefinition else it
            }
            state.copy(attributeDefinitions = updated)
        }
    }

    fun saveChanges() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }

        viewModelScope.launch {
            try {
                // Use bulk save endpoint - single API call for both field configs and attributes
                val schema = EntityConfigSchema(
                    fieldConfigs = state.fieldConfigs,
                    attributeDefinitions = state.attributeDefinitions
                )

                val result = configRepository.saveConfigSchema(entityType, schema)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Configuration saved successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message
                                ?: "Failed to save configuration"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save configuration"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    @OptIn(ExperimentalTime::class)
    fun addNewAttribute() {
        val newAttribute = EntityAttributeDefinition(
            uid = "temp-${
                kotlin.time.Clock.System.now().toEpochMilliseconds()
            }", // Temporary UID until saved
            entityType = entityType,
            attributeKey = "", // User will fill this
            displayName = "",
            dataType = "STRING",
            visible = true,
            mandatory = false,
            enabled = true,
            displayOrder = _uiState.value.attributeDefinitions.size + 1
        )

        _uiState.update { state ->
            state.copy(attributeDefinitions = state.attributeDefinitions + newAttribute)
        }
    }

    fun deleteAttributeDefinition(attributeDefinition: EntityAttributeDefinition) {
        _uiState.update { state ->
            state.copy(
                attributeDefinitions = state.attributeDefinitions.filter {
                    it.uid != attributeDefinition.uid
                }
            )
        }
    }
}
