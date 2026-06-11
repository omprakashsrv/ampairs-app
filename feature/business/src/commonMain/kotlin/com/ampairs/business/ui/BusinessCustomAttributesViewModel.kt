package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessProfileUpdateRequest
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.repository.ConfigLookup
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.render.CustomFieldWidgetRegistry
import com.ampairs.form.render.DynamicOptionRegistry
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Business Custom Attributes screen (spec 011, US5). Custom fields come from the
 * unified business `FormSchema`; values bridge to `BusinessProfile.customAttributes`
 * (`Map<String, String>`), so the save path is unchanged.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class BusinessCustomAttributesViewModel(
    private val businessRepository: BusinessRepository,
    private val configRepository: ConfigLookup,
    private val syncService: CentralSyncService,
    val optionRegistry: DynamicOptionRegistry,
    val widgetRegistry: CustomFieldWidgetRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomAttributesUiState())
    val uiState: StateFlow<CustomAttributesUiState> = _uiState.asStateFlow()

    /** Live unified schema for the business form — consumed by the shared attributes section. */
    val formSchema: StateFlow<FormSchema?> =
        configRepository.observeSchema("business")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        loadData()
        // Pull the form schema via CentralSyncService → FormSyncDelegate.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.FORM))
        observeSchemaPresence()
    }

    private fun observeSchemaPresence() {
        viewModelScope.launch {
            formSchema.collect { schema ->
                val hasCustom = schema?.fields?.any { it.source == FieldSource.CUSTOM && it.visible } == true
                _uiState.value = _uiState.value.copy(hasCustomAttributes = hasCustom)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profileResult = businessRepository.fetchBusinessProfile()
                profileResult.onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        businessName = profile.name,
                        customAttributeValues = profile.customAttributes ?: emptyMap(),
                        isLoading = false,
                        error = null,
                    )
                }
                profileResult.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load business profile: ${error.message}",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}",
                )
            }
        }
    }

    fun updateAttributes(values: Map<String, String>) {
        _uiState.value = _uiState.value.copy(customAttributeValues = values)
    }

    fun saveCustomAttributes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, saveSuccess = false)

            try {
                val profileResult = businessRepository.fetchBusinessProfile()

                profileResult.onSuccess { currentProfile ->
                    val customAttributesMap = _uiState.value.customAttributeValues

                    val updateRequest = BusinessProfileUpdateRequest(
                        name = currentProfile.name,
                        businessType = currentProfile.businessType,
                        description = currentProfile.description,
                        ownerName = currentProfile.ownerName,
                        addressLine1 = currentProfile.addressLine1,
                        addressLine2 = currentProfile.addressLine2,
                        city = currentProfile.city,
                        state = currentProfile.state,
                        postalCode = currentProfile.postalCode,
                        country = currentProfile.country,
                        latitude = currentProfile.latitude,
                        longitude = currentProfile.longitude,
                        phone = currentProfile.phone,
                        email = currentProfile.email,
                        website = currentProfile.website,
                        active = currentProfile.active,
                        customAttributes = customAttributesMap,
                    )

                    businessRepository.updateBusinessProfile(updateRequest)
                        .onSuccess { updatedProfile ->
                            val saved = updatedProfile.customAttributes?.takeIf { it.isNotEmpty() } ?: customAttributesMap
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                saveSuccess = true,
                                error = null,
                                customAttributeValues = saved,
                            )
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                saveSuccess = false,
                                error = "Failed to save: ${error.message}",
                            )
                        }
                }

                profileResult.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = false,
                        error = "Failed to fetch current profile: ${error.message}",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = false,
                    error = "Failed to save custom attributes: ${e.message}",
                )
            }
        }
    }

    fun refresh() {
        loadData()
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.FORM))
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

/**
 * UI State for the Custom Attributes screen. Field definitions come from [BusinessCustomAttributesViewModel.formSchema];
 * this carries the value map and screen status only.
 */
data class CustomAttributesUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val businessName: String = "",
    val hasCustomAttributes: Boolean = false,
    val customAttributeValues: Map<String, String> = emptyMap(),
    val saveSuccess: Boolean = false,
)
