package com.ampairs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.model.AccountDeletionRequest
import com.ampairs.auth.api.model.AccountDeletionResponse
import com.ampairs.auth.api.model.AccountDeletionStatus
import com.ampairs.auth.api.model.BlockingWorkspace
import com.ampairs.common.model.UiState
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import kotlinx.coroutines.launch

class AccountDeletionViewModel(
    private val authApi: AuthApi
) : ViewModel() {

    var reason by mutableStateOf("")
        private set

    var confirmed by mutableStateOf(false)
        private set

    var deletionState by mutableStateOf<UiState<AccountDeletionResponse>>(UiState.Empty)
        private set

    var statusState by mutableStateOf<UiState<AccountDeletionStatus>>(UiState.Empty)
        private set

    var displayMessage by mutableStateOf("")

    var blockingWorkspaces by mutableStateOf<List<BlockingWorkspace>>(emptyList())
        private set

    var showBlockingWorkspacesDialog by mutableStateOf(false)
        private set

    init {
        loadDeletionStatus()
    }

    fun updateReason(newReason: String) {
        reason = newReason
    }

    fun toggleConfirmation() {
        confirmed = !confirmed
    }

    fun loadDeletionStatus() {
        viewModelScope.launch {
            statusState = UiState.Loading(null)
            authApi.getAccountDeletionStatus().onSuccess {
                statusState = UiState.Success(this)
            }.onError {
                // If account is not marked for deletion, this might return an error
                statusState = UiState.Empty
            }
        }
    }

    fun requestAccountDeletion(onSuccess: () -> Unit) {
        if (!confirmed) {
            displayMessage = "Please confirm that you want to delete your account"
            return
        }

        viewModelScope.launch {
            deletionState = UiState.Loading(null)
            val request = AccountDeletionRequest(
                confirmed = true,
                reason = reason.trim().ifBlank { null }
            )

            authApi.requestAccountDeletion(request).onSuccess {
                if (this.deletionRequested) {
                    deletionState = UiState.Success(this)
                    displayMessage = this.message
                    onSuccess()
                } else {
                    // Account deletion blocked due to workspace ownership
                    this@AccountDeletionViewModel.blockingWorkspaces = this.blockingWorkspaces ?: emptyList()
                    this@AccountDeletionViewModel.showBlockingWorkspacesDialog = true
                    deletionState = UiState.Error(this.message)
                    displayMessage = this.message
                }
            }.onError {
                deletionState = UiState.Error(this.message.ifEmpty { "Failed to delete account" })
                displayMessage = this.message.ifEmpty { "Failed to delete account" }
            }
        }
    }

    fun cancelAccountDeletion(onSuccess: () -> Unit) {
        viewModelScope.launch {
            deletionState = UiState.Loading(null)
            authApi.cancelAccountDeletion().onSuccess {
                deletionState = UiState.Success(this)
                displayMessage = this.message
                statusState = UiState.Empty
                onSuccess()
            }.onError {
                deletionState = UiState.Error(this.message.ifEmpty { "Failed to cancel deletion" })
                displayMessage = this.message.ifEmpty { "Failed to cancel deletion" }
            }
        }
    }

    fun dismissBlockingWorkspacesDialog() {
        showBlockingWorkspacesDialog = false
    }

    fun clearMessage() {
        displayMessage = ""
    }

    val isLoading: Boolean
        get() = deletionState is UiState.Loading || statusState is UiState.Loading

    val isFormValid: Boolean
        get() = confirmed

    val isDeletionPending: Boolean
        get() = statusState is UiState.Success && (statusState as UiState.Success<AccountDeletionStatus>).data?.isDeleted == true

    val daysRemaining: Int?
        get() = (statusState as? UiState.Success<AccountDeletionStatus>)?.data?.daysRemaining
}
