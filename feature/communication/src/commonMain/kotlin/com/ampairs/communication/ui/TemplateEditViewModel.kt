package com.ampairs.communication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.communication.data.api.CommunicationActionApi
import com.ampairs.communication.data.api.PreviewRequest
import com.ampairs.communication.data.api.TemplateAggregateDto
import com.ampairs.communication.data.api.TemplateVariantDto
import com.ampairs.communication.data.repository.TemplateRepository
import com.ampairs.communication.util.CommunicationConstants
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateEditUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val isNew: Boolean = true,
    val code: String = "",
    val name: String = "",
    val category: String = "TRANSACTIONAL",
    val defaultLocale: String = "",
    val description: String = "",
    val variants: List<TemplateVariantDto> = emptyList(),
    val previewHtml: String? = null,
    val previewText: String? = null,
    val previewSubject: String? = null,
    val missingVariables: List<String> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
)

@AssistedInject
class TemplateEditViewModel(
    @Assisted val templateUid: String,
    private val repository: TemplateRepository,
    private val actionApi: CommunicationActionApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateEditUiState())
    val uiState: StateFlow<TemplateEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = templateUid.takeIf { it.isNotBlank() }?.let { repository.get(it) }
            if (existing == null) {
                _uiState.update { it.copy(loading = false, isNew = true) }
            } else {
                _uiState.update {
                    it.copy(
                        loading = false,
                        isNew = false,
                        code = existing.code,
                        name = existing.name,
                        category = existing.category,
                        defaultLocale = existing.defaultLocale.orEmpty(),
                        description = existing.description.orEmpty(),
                        variants = existing.variants,
                    )
                }
            }
        }
    }

    fun onCode(v: String) = _uiState.update { it.copy(code = v) }
    fun onName(v: String) = _uiState.update { it.copy(name = v) }
    fun onCategory(v: String) = _uiState.update { it.copy(category = v) }
    fun onLocale(v: String) = _uiState.update { it.copy(defaultLocale = v) }
    fun onDescription(v: String) = _uiState.update { it.copy(description = v) }

    fun addVariant() {
        val variant = TemplateVariantDto(
            uid = UidGenerator.generateUid(CommunicationConstants.VARIANT_UID_PREFIX),
            channel = "EMAIL",
            locale = _uiState.value.defaultLocale.takeIf { it.isNotBlank() },
        )
        _uiState.update { it.copy(variants = it.variants + variant) }
    }

    fun updateVariant(index: Int, variant: TemplateVariantDto) {
        _uiState.update { state ->
            state.copy(variants = state.variants.toMutableList().also { if (index in it.indices) it[index] = variant })
        }
    }

    fun removeVariant(index: Int) {
        _uiState.update { state ->
            state.copy(variants = state.variants.toMutableList().also { if (index in it.indices) it.removeAt(index) })
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.code.isBlank() || state.name.isBlank()) {
            _uiState.update { it.copy(error = "Code and name are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val uid = templateUid.takeIf { it.isNotBlank() }
                ?: UidGenerator.generateUid(CommunicationConstants.TEMPLATE_UID_PREFIX)
            val dto = TemplateAggregateDto(
                uid = uid,
                code = state.code.trim(),
                name = state.name.trim(),
                category = state.category,
                defaultLocale = state.defaultLocale.takeIf { it.isNotBlank() },
                description = state.description.takeIf { it.isNotBlank() },
                variants = state.variants,
            )
            runCatching { repository.save(dto) }
                .onSuccess { _uiState.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, error = e.message ?: "Save failed") } }
        }
    }

    /** Server-rendered preview of the first variant's channel (FR-006). Requires the template to be saved. */
    fun preview() {
        val state = _uiState.value
        val channel = state.variants.firstOrNull()?.channel ?: "EMAIL"
        viewModelScope.launch {
            val resp = actionApi.previewTemplate(
                code = state.code.trim(),
                request = PreviewRequest(channel = channel, locale = state.defaultLocale.takeIf { it.isNotBlank() }),
            )
            val data = resp.data
            if (data != null && resp.error == null) {
                _uiState.update {
                    it.copy(
                        previewHtml = data.renderedHtml,
                        previewText = data.renderedText,
                        previewSubject = data.subject,
                        missingVariables = data.missingVariables,
                        error = null,
                    )
                }
            } else {
                _uiState.update { it.copy(error = resp.error?.message ?: "Preview failed") }
            }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(templateUid: String): TemplateEditViewModel
    }
}
