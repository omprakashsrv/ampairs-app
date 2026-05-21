package com.ampairs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.model.AccountDeletionRequest
import com.ampairs.auth.api.model.AccountDeletionResponse
import com.ampairs.auth.api.model.AccountDeletionStatus
import com.ampairs.auth.api.model.BlockingWorkspace
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.common.model.UiState
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class AccountDeletionViewModel(
    private val authApi: AuthApi,
    private val tokenRepository: TokenRepository,
    private val userDao: UserDao
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

    var ownedWorkspaces by mutableStateOf<List<BlockingWorkspace>>(emptyList())
        private set

    var isLoadingOwnedWorkspaces by mutableStateOf(false)
        private set

    init {
        loadDeletionStatus()
        loadOwnedWorkspaces()
    }

    fun loadOwnedWorkspaces() {
        viewModelScope.launch {
            isLoadingOwnedWorkspaces = true

            try {
                // Make a deletion check request with confirmed=false to get blocking workspaces
                authApi.requestAccountDeletion(
                    AccountDeletionRequest(confirmed = false, reason = null)
                ).onSuccess {
                    println("🔍 Account deletion check - deletionRequested: ${this.deletionRequested}")
                    println("🔍 blockingWorkspaces: ${this.blockingWorkspaces?.joinToString { it.workspaceName } ?: "null"}")

                    // The backend returns blockingWorkspaces if user owns any workspaces
                    val workspaces = this.blockingWorkspaces ?: emptyList()
                    ownedWorkspaces = workspaces

                    if (workspaces.isNotEmpty()) {
                        println("✅ Found ${workspaces.size} owned workspace(s):")
                        workspaces.forEach {
                            println("   - ${it.workspaceName} (${it.memberCount} members)")
                        }
                    } else {
                        println("✅ No owned workspaces - user can proceed with deletion")
                    }
                }.onError {
                    println("❌ Error checking owned workspaces: ${this.message}")
                    // On error, assume no owned workspaces (backend will validate on actual deletion)
                    ownedWorkspaces = emptyList()
                }
            } catch (e: Exception) {
                println("❌ Network error checking owned workspaces: ${e.message}")
                // On network error, assume no owned workspaces (backend will validate on actual deletion)
                ownedWorkspaces = emptyList()
            }

            isLoadingOwnedWorkspaces = false
        }
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
            try {
                authApi.getAccountDeletionStatus().onSuccess {
                    statusState = UiState.Success(this)
                }.onError {
                    // If account is not marked for deletion, this might return an error
                    statusState = UiState.Empty
                }
            } catch (e: Exception) {
                println("❌ Network error loading deletion status: ${e.message}")
                // On network error, assume no pending deletion
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

            try {
                authApi.requestAccountDeletion(request).onSuccess {
                    if (this.deletionRequested) {
                        deletionState = UiState.Success(this)
                        displayMessage = this.message

                        // Clear local user data and logout in a coroutine
                        viewModelScope.launch {
                            try {
                                // Get current user ID before clearing
                                val currentUserId = tokenRepository.getCurrentUserId()

                                // Clear tokens
                                tokenRepository.clearTokens()
                                authApi.clearToken()

                                // Delete user entity from local database
                                currentUserId?.let { userId ->
                                    userDao.deleteById(userId)
                                    println("✅ Deleted user entity from local database: $userId")
                                }
                            } catch (e: Exception) {
                                println("⚠️ Failed to cleanup user data: ${e.message}")
                            }
                        }

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
            } catch (e: Exception) {
                println("❌ Network error requesting account deletion: ${e.message}")
                deletionState = UiState.Error("Unable to connect to server. Please check your network connection.")
                displayMessage = "Unable to connect to server. Please check your network connection."
            }
        }
    }

    fun cancelAccountDeletion(onSuccess: () -> Unit) {
        viewModelScope.launch {
            deletionState = UiState.Loading(null)
            try {
                authApi.cancelAccountDeletion().onSuccess {
                    deletionState = UiState.Success(this)
                    displayMessage = this.message
                    statusState = UiState.Empty
                    onSuccess()
                }.onError {
                    deletionState = UiState.Error(this.message.ifEmpty { "Failed to cancel deletion" })
                    displayMessage = this.message.ifEmpty { "Failed to cancel deletion" }
                }
            } catch (e: Exception) {
                println("❌ Network error canceling account deletion: ${e.message}")
                deletionState = UiState.Error("Unable to connect to server. Please check your network connection.")
                displayMessage = "Unable to connect to server. Please check your network connection."
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
