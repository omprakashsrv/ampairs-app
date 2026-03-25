package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessProfile
import com.ampairs.business.domain.BusinessProfileUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Business Profile Form Screen.
 * Manages loading, editing, and saving business profile.
 */
class BusinessProfileViewModel(
    private val repository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.fetchBusinessProfile()
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        formState = profile.toFormState(),
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    val errorMessage = error.message ?: ""

                    // If profile doesn't exist (404), treat it as create mode - not an error
                    if (errorMessage.contains("not found", ignoreCase = true) ||
                        errorMessage.contains("404", ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            profile = null,
                            formState = BusinessProfileFormState(), // Empty form for creation
                            isLoading = false,
                            error = null // No error - this is create mode
                        )
                    } else {
                        // Real error - show error message
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage.ifBlank { "Failed to load business profile" }
                        )
                    }
                }
        }
    }

    fun updateFormState(formState: BusinessProfileFormState) {
        _uiState.value = _uiState.value.copy(formState = formState)
    }

    fun saveProfile() {
        val formState = _uiState.value.formState
        val isCreateMode = _uiState.value.profile == null

        // Validate
        if (formState.name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Business name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            if (isCreateMode) {
                // Create new business profile
                val createRequest = com.ampairs.business.domain.BusinessCreateRequest(
                    name = formState.name,
                    businessType = formState.businessType,
                    description = formState.description.takeIf { it.isNotBlank() },
                    ownerName = formState.ownerName.takeIf { it.isNotBlank() },
                    addressLine1 = formState.addressLine1.takeIf { it.isNotBlank() },
                    addressLine2 = formState.addressLine2.takeIf { it.isNotBlank() },
                    city = formState.city.takeIf { it.isNotBlank() },
                    state = formState.state.takeIf { it.isNotBlank() },
                    postalCode = formState.postalCode.takeIf { it.isNotBlank() },
                    country = formState.country.takeIf { it.isNotBlank() },
                    latitude = formState.latitude.toDoubleOrNull(),
                    longitude = formState.longitude.toDoubleOrNull(),
                    phone = formState.phone.takeIf { it.isNotBlank() },
                    email = formState.email.takeIf { it.isNotBlank() },
                    website = formState.website.takeIf { it.isNotBlank() },
                    taxId = formState.taxId.takeIf { it.isNotBlank() },
                    registrationNumber = formState.registrationNumber.takeIf { it.isNotBlank() }
                )

                repository.createBusinessProfile(createRequest)
                    .onSuccess { createdProfile ->
                        _uiState.value = _uiState.value.copy(
                            profile = createdProfile,
                            formState = createdProfile.toFormState(),
                            isSaving = false,
                            saveSuccess = true,
                            wasCreateOperation = true,
                            error = null
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to create business profile"
                        )
                    }
            } else {
                // Update existing business profile
                val updateRequest = BusinessProfileUpdateRequest(
                    name = formState.name,
                    businessType = formState.businessType,
                    description = formState.description.takeIf { it.isNotBlank() },
                    ownerName = formState.ownerName.takeIf { it.isNotBlank() },
                    addressLine1 = formState.addressLine1.takeIf { it.isNotBlank() },
                    addressLine2 = formState.addressLine2.takeIf { it.isNotBlank() },
                    city = formState.city.takeIf { it.isNotBlank() },
                    state = formState.state.takeIf { it.isNotBlank() },
                    postalCode = formState.postalCode.takeIf { it.isNotBlank() },
                    country = formState.country.takeIf { it.isNotBlank() },
                    latitude = formState.latitude.toDoubleOrNull(),
                    longitude = formState.longitude.toDoubleOrNull(),
                    phone = formState.phone.takeIf { it.isNotBlank() },
                    email = formState.email.takeIf { it.isNotBlank() },
                    website = formState.website.takeIf { it.isNotBlank() },
                    taxId = formState.taxId.takeIf { it.isNotBlank() },
                    registrationNumber = formState.registrationNumber.takeIf { it.isNotBlank() },
                    active = formState.active
                )

                repository.updateBusinessProfile(updateRequest)
                    .onSuccess { updatedProfile ->
                        _uiState.value = _uiState.value.copy(
                            profile = updatedProfile,
                            formState = updatedProfile.toFormState(),
                            isSaving = false,
                            saveSuccess = true,
                            wasCreateOperation = false,
                            error = null
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save business profile"
                        )
                    }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, wasCreateOperation = false)
    }

    fun refresh() {
        loadProfile()
    }
}

data class BusinessProfileUiState(
    val profile: BusinessProfile? = null,
    val formState: BusinessProfileFormState = BusinessProfileFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val wasCreateOperation: Boolean = false,
    val error: String? = null
)

data class BusinessProfileFormState(
    val name: String = "",
    val businessType: String = "RETAIL",
    val description: String = "",
    val ownerName: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val taxId: String = "",
    val registrationNumber: String = "",
    val active: Boolean = true
)

private fun BusinessProfile.toFormState() = BusinessProfileFormState(
    name = name,
    businessType = businessType,
    description = description ?: "",
    ownerName = ownerName ?: "",
    addressLine1 = addressLine1 ?: "",
    addressLine2 = addressLine2 ?: "",
    city = city ?: "",
    state = state ?: "",
    postalCode = postalCode ?: "",
    country = country ?: "",
    latitude = latitude?.toString() ?: "",
    longitude = longitude?.toString() ?: "",
    phone = phone ?: "",
    email = email ?: "",
    website = website ?: "",
    taxId = taxId ?: "",
    registrationNumber = registrationNumber ?: "",
    active = active
)
