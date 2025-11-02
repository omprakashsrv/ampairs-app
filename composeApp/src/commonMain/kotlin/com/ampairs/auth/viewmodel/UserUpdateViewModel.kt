package com.ampairs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.api.model.UserUpdateRequest
import com.ampairs.common.model.UiState
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import kotlinx.coroutines.launch

class UserUpdateViewModel(
    private val authApi: AuthApi
) : ViewModel() {

    var firstName by mutableStateOf("")
        private set
    
    var lastName by mutableStateOf("")
        private set
    
    var userState by mutableStateOf<UiState<UserApiModel>>(UiState.Empty)
        private set
    
    var updateUserState by mutableStateOf<UiState<UserApiModel>>(UiState.Empty)
        private set
    
    var displayMessage by mutableStateOf("")

    init {
        loadUserDetails()
    }

    fun updateFirstName(newFirstName: String) {
        firstName = newFirstName
    }

    fun updateLastName(newLastName: String) {
        lastName = newLastName
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            userState = UiState.Loading(null)
            authApi.getUser().onSuccess {
                this@UserUpdateViewModel.firstName = this.firstName
                this@UserUpdateViewModel.lastName = this.lastName
                userState = UiState.Success(this)
            }.onError {
                userState = UiState.Error(this.message.ifEmpty { "Failed to load user details" })
            }
        }
    }

    fun updateUser(onSuccess: () -> Unit) {
        if (firstName.isBlank()) {
            displayMessage = "First name is required"
            return
        }

        viewModelScope.launch {
            updateUserState = UiState.Loading(null)
            val request = UserUpdateRequest(
                firstName = firstName.trim(),
                lastName = lastName.trim()
            )
            authApi.updateUser(request).onSuccess {
                updateUserState = UiState.Success(this)
                displayMessage = "Profile updated successfully"
                onSuccess()
            }.onError {
                updateUserState = UiState.Error(this.message.ifEmpty { "Failed to update user details" })
                displayMessage = this.message.ifEmpty { "Failed to update user details" }
            }
        }
    }

    fun clearMessage() {
        displayMessage = ""
    }

    val isLoading: Boolean
        get() = userState is UiState.Loading || updateUserState is UiState.Loading

    val isFormValid: Boolean
        get() = firstName.isNotBlank()
}