package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.form.domain.EntityAttributeDefinition
import com.ampairs.form.domain.EntityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for Business Custom Attributes Screen.
 * Manages loading custom attribute definitions and business profile data.
 */
class BusinessCustomAttributesViewModel(
    private val businessRepository: BusinessRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomAttributesUiState())
    val uiState: StateFlow<CustomAttributesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Step 1: Load business profile to get current custom attribute values
                val profileResult = businessRepository.fetchBusinessProfile()

                val businessName: String
                val customAttributeValues: Map<String, Any?>

                if (profileResult.isSuccess) {
                    val profile = profileResult.getOrThrow()
                    businessName = profile.name
                    customAttributeValues = profile.customAttributes?.mapValues { it.value as Any? } ?: emptyMap()

                    println("📋 CustomAttributes: Loaded business profile")
                    println("   Business Name: $businessName")
                    println("   Custom Attributes: $customAttributeValues")
                } else {
                    businessName = ""
                    customAttributeValues = emptyMap()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load business profile: ${profileResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                // Step 2: Load attribute definitions from config (try fetching from server first)
                configRepository.refreshConfig(EntityType.BUSINESS)

                // Step 3: Get config schema from database (use first() instead of collect to avoid overwriting values)
                val schema = configRepository.observeConfigSchema(EntityType.BUSINESS).first()
                val attributes = schema?.attributeDefinitions?.filter { it.visible } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    businessName = businessName,
                    customAttributes = attributes,
                    customAttributeValues = customAttributeValues,
                    hasCustomAttributes = attributes.isNotEmpty(),
                    isLoading = false,
                    error = null
                )

                println("📋 CustomAttributes: Initial load complete")
                println("   Attributes loaded: ${attributes.size}")
                println("   Values set: $customAttributeValues")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    fun updateAttributeValue(attributeKey: String, value: Any?) {
        val currentValues = _uiState.value.customAttributeValues.toMutableMap()
        currentValues[attributeKey] = value
        _uiState.value = _uiState.value.copy(customAttributeValues = currentValues)

        println("✏️ CustomAttributes: Updated value for '$attributeKey' = '$value'")
        println("   All values: $currentValues")
    }

    fun saveCustomAttributes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, saveSuccess = false)

            try {
                // Fetch current business profile
                val profileResult = businessRepository.fetchBusinessProfile()

                profileResult.onSuccess { currentProfile ->
                    // Convert custom attribute values to Map<String, String>
                    val customAttributesMap = _uiState.value.customAttributeValues.mapValues { (_, value) ->
                        value?.toString() ?: ""
                    }

                    println("💾 CustomAttributes: Saving custom attributes")
                    println("   Values to save: ${_uiState.value.customAttributeValues}")
                    println("   Converted to strings: $customAttributesMap")

                    // Create update request with existing profile data + new custom attributes
                    val updateRequest = com.ampairs.business.domain.BusinessProfileUpdateRequest(
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
                        taxId = currentProfile.taxId,
                        registrationNumber = currentProfile.registrationNumber,
                        active = currentProfile.active,
                        customAttributes = customAttributesMap
                    )

                    // Update business profile with custom attributes
                    businessRepository.updateBusinessProfile(updateRequest)
                        .onSuccess { updatedProfile ->
                            println("✅ CustomAttributes: Save successful")
                            println("   Server returned custom attributes: ${updatedProfile.customAttributes}")

                            // Use the values we sent if server doesn't return them (some APIs don't echo back)
                            val savedCustomAttributes = if (updatedProfile.customAttributes.isNullOrEmpty()) {
                                println("   ⚠️ Server didn't return custom attributes, using local values")
                                customAttributesMap.mapValues { it.value as Any? }
                            } else {
                                println("   ✓ Using server-returned custom attributes")
                                updatedProfile.customAttributes.mapValues { it.value as Any? }
                            }

                            println("   Final values to set in state: $savedCustomAttributes")

                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                saveSuccess = true,
                                error = null,
                                customAttributeValues = savedCustomAttributes
                            )

                            println("   State updated, current values: ${_uiState.value.customAttributeValues}")
                        }
                        .onFailure { error ->
                            println("❌ CustomAttributes: Save failed: ${error.message}")

                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                saveSuccess = false,
                                error = "Failed to save: ${error.message}"
                            )
                        }
                }

                profileResult.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = false,
                        error = "Failed to fetch current profile: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = false,
                    error = "Failed to save custom attributes: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        loadData()
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

/**
 * UI State for Custom Attributes Screen
 */
data class CustomAttributesUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val businessName: String = "",
    val hasCustomAttributes: Boolean = false,
    val customAttributes: List<EntityAttributeDefinition> = emptyList(),
    val customAttributeValues: Map<String, Any?> = emptyMap(),
    val saveSuccess: Boolean = false
)
