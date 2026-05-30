package com.ampairs.auth.viewmodel

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.auth_error_confirm_deletion
import ampairsapp.feature.auth.generated.resources.auth_error_network
import ampairsapp.feature.auth.generated.resources.auth_error_failed_delete_account
import ampairsapp.feature.auth.generated.resources.auth_error_failed_cancel_deletion

data class AccountDeletionUiState(
    val reason: String = "",
    val confirmed: Boolean = false,
    val deletionState: UiState<AccountDeletionResponse> = UiState.Empty,
    val statusState: UiState<AccountDeletionStatus> = UiState.Empty,
    val blockingWorkspaces: List<BlockingWorkspace> = emptyList(),
    val showBlockingWorkspacesDialog: Boolean = false,
    val ownedWorkspaces: List<BlockingWorkspace> = emptyList(),
    val isLoadingOwnedWorkspaces: Boolean = false,
)

sealed interface AccountDeletionNavEvent {
    data object DeletionSuccess : AccountDeletionNavEvent
    data object CancellationSuccess : AccountDeletionNavEvent
    data object LogoutComplete : AccountDeletionNavEvent
}

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class AccountDeletionViewModel(
    private val authApi: AuthApi,
    private val tokenRepository: TokenRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDeletionUiState())
    val state: StateFlow<AccountDeletionUiState> = _state.asStateFlow()

    private val _navEvent = MutableSharedFlow<AccountDeletionNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<AccountDeletionNavEvent> = _navEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadDeletionStatus()
        loadOwnedWorkspaces()
    }

    fun loadOwnedWorkspaces() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingOwnedWorkspaces = true) }
            try {
                authApi.requestAccountDeletion(
                    AccountDeletionRequest(confirmed = false, reason = null)
                ).onSuccess {
                    _state.update { s -> s.copy(ownedWorkspaces = this.blockingWorkspaces ?: emptyList()) }
                }.onError {
                    _state.update { it.copy(ownedWorkspaces = emptyList()) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(ownedWorkspaces = emptyList()) }
            }
            _state.update { it.copy(isLoadingOwnedWorkspaces = false) }
        }
    }

    fun updateReason(newReason: String) {
        _state.update { it.copy(reason = newReason) }
    }

    fun toggleConfirmation() {
        _state.update { it.copy(confirmed = !it.confirmed) }
    }

    fun loadDeletionStatus() {
        viewModelScope.launch {
            _state.update { it.copy(statusState = UiState.Loading(null)) }
            try {
                authApi.getAccountDeletionStatus().onSuccess {
                    _state.update { s -> s.copy(statusState = UiState.Success(this)) }
                }.onError {
                    _state.update { it.copy(statusState = UiState.Empty) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(statusState = UiState.Empty) }
            }
        }
    }

    fun requestAccountDeletion() {
        viewModelScope.launch {
            if (!_state.value.confirmed) {
                _snackbarMessage.emit(getString(Res.string.auth_error_confirm_deletion))
                return@launch
            }
            _state.update { it.copy(deletionState = UiState.Loading(null)) }
            val request = AccountDeletionRequest(
                confirmed = true,
                reason = _state.value.reason.trim().ifBlank { null }
            )
            try {
                val response = authApi.requestAccountDeletion(request)
                if (response.data != null && response.error == null) {
                    val data = response.data!!
                    if (data.deletionRequested) {
                        _state.update { s -> s.copy(deletionState = UiState.Success(data)) }
                        _snackbarMessage.emit(data.message)
                        viewModelScope.launch {
                            try {
                                val currentUserId = tokenRepository.getCurrentUserId()
                                tokenRepository.clearTokens()
                                authApi.clearToken()
                                currentUserId?.let { userId -> userDao.deleteById(userId) }
                            } catch (_: Exception) { }
                        }
                        _navEvent.emit(AccountDeletionNavEvent.DeletionSuccess)
                    } else {
                        _state.update { s ->
                            s.copy(
                                blockingWorkspaces = data.blockingWorkspaces ?: emptyList(),
                                showBlockingWorkspacesDialog = true,
                                deletionState = UiState.Error(data.message),
                            )
                        }
                        _snackbarMessage.emit(data.message)
                    }
                } else {
                    val errMsg = response.error?.message.orEmpty().ifEmpty { getString(Res.string.auth_error_failed_delete_account) }
                    _state.update { it.copy(deletionState = UiState.Error(errMsg)) }
                    _snackbarMessage.emit(errMsg)
                }
            } catch (_: Exception) {
                val errMsg = getString(Res.string.auth_error_network)
                _state.update { it.copy(deletionState = UiState.Error(errMsg)) }
                _snackbarMessage.emit(errMsg)
            }
        }
    }

    fun cancelAccountDeletion() {
        viewModelScope.launch {
            _state.update { it.copy(deletionState = UiState.Loading(null)) }
            try {
                val response = authApi.cancelAccountDeletion()
                if (response.data != null && response.error == null) {
                    val data = response.data!!
                    _state.update { s ->
                        s.copy(deletionState = UiState.Success(data), statusState = UiState.Empty)
                    }
                    _snackbarMessage.emit(data.message)
                    _navEvent.emit(AccountDeletionNavEvent.CancellationSuccess)
                } else {
                    val errMsg = response.error?.message.orEmpty().ifEmpty { getString(Res.string.auth_error_failed_cancel_deletion) }
                    _state.update { it.copy(deletionState = UiState.Error(errMsg)) }
                    _snackbarMessage.emit(errMsg)
                }
            } catch (_: Exception) {
                val errMsg = getString(Res.string.auth_error_network)
                _state.update { it.copy(deletionState = UiState.Error(errMsg)) }
                _snackbarMessage.emit(errMsg)
            }
        }
    }

    fun dismissBlockingWorkspacesDialog() {
        _state.update { it.copy(showBlockingWorkspacesDialog = false) }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val currentUserId = tokenRepository.getCurrentUserId()
                tokenRepository.clearTokens()
                currentUserId?.let { userId ->
                    try { userDao.deleteById(userId) } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
            _navEvent.emit(AccountDeletionNavEvent.LogoutComplete)
        }
    }

}
