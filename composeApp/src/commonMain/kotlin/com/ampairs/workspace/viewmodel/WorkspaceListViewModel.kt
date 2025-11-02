package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.common.DeviceService
import com.ampairs.event.EventConnectionManager
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.db.UserInvitationRepository
import com.ampairs.workspace.domain.Workspace
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadRequest
import com.ampairs.workspace.ui.WorkspaceListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WorkspaceListViewModel(
    private val workspaceRepository: OfflineFirstWorkspaceRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
    private val invitationRepository: UserInvitationRepository,
    private val deviceService: DeviceService,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceListState())
    val state: StateFlow<WorkspaceListState> = _state.asStateFlow()

    private val eventConnectionManager = EventConnectionManager()

    init {
        loadUserData()
        loadInvitations()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUserLoading = true)

            try {
                val user = userRepository.getUser()
                val fullName = if (user != null) {
                    "${user.first_name} ${user.last_name}".trim()
                } else {
                    "User"
                }

                _state.value = _state.value.copy(
                    userFullName = fullName,
                    isUserLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    userFullName = "User",
                    isUserLoading = false
                )
            }
        }
    }

    suspend fun selectWorkSpace(workspaceId: String) {
        val currentUserId = tokenRepository.getCurrentUserId()
        if (currentUserId != null) {
            userWorkspaceRepository.setWorkspaceIdForUser(currentUserId, workspaceId)

            // Connect to workspace events for real-time sync
            val deviceId = deviceService.getDeviceId()
            eventConnectionManager.connectToWorkspace(
                workspaceId = workspaceId,
                userId = currentUserId,
                deviceId = deviceId,
                scope = viewModelScope
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventConnectionManager.disconnect()
    }

    fun loadWorkspaces(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, isLoading = true)

            try {
                workspaceRepository.getUserWorkspaces(
                    page = 0,
                    size = 50, // Load more workspaces for the list
                    forceRefresh = forceRefresh
                ).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            _state.value = _state.value.copy(
                                workspaces = pageResult.content,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                hasNoWorkspaces = pageResult.isEmpty,
                                isOfflineMode = false
                            )
                        }
                        is StoreReadResponse.Loading -> {
                            // Show loading only when we don't have any cached data
                            if (_state.value.workspaces.isEmpty()) {
                                _state.value = _state.value.copy(isLoading = true)
                            }
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                error = response.error.message ?: "Failed to load workspaces",
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                error = response.message,
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        else -> {
                            // Handle other response types if needed
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load workspaces",
                    isLoading = false,
                    isRefreshing = false,
                    isOfflineMode = true
                )
            }
        }
    }

    fun refreshWorkspaces() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)

            try {
                workspaceRepository.getUserWorkspaces(
                    page = 0,
                    size = 50,
                    forceRefresh = true
                ).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            _state.value = _state.value.copy(
                                workspaces = pageResult.content,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                hasNoWorkspaces = pageResult.isEmpty,
                                isOfflineMode = false
                            )
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                error = response.error.message ?: "Failed to refresh workspaces",
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                error = response.message,
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to refresh workspaces",
                    isLoading = false,
                    isRefreshing = false,
                    isOfflineMode = true
                )
            }
        }
    }

    fun searchWorkspaces(query: String) {
        _state.value = _state.value.copy(searchQuery = query)

        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    // Load all workspaces when search is empty
                    loadWorkspaces(forceRefresh = false)
                } else {
                    // Use repository's search functionality
                    workspaceRepository.searchWorkspaces(query, page = 0, size = 50)
                        .collect { workspaces ->
                            _state.value = _state.value.copy(
                                workspaces = workspaces,
                                isLoading = false,
                                error = null
                            )
                        }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to search workspaces",
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            // Store5 handles cache clearing automatically
            tokenRepository.clearTokens()
        }
    }
    
    /**
     * Get cached workspaces only (useful for offline mode)
     */
    fun loadCachedWorkspaces() {
        viewModelScope.launch {
            try {
                workspaceRepository.getCachedWorkspaces(
                    page = 0,
                    size = 50
                ).collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            _state.value = _state.value.copy(
                                workspaces = pageResult.content,
                                isLoading = false,
                                error = null,
                                hasNoWorkspaces = pageResult.isEmpty,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                error = "No cached data available",
                                isLoading = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                error = "No cached data available",
                                isLoading = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "No cached data available",
                    isLoading = false,
                    isOfflineMode = true
                )
            }
        }
    }

    // ===== INVITATION MANAGEMENT =====

    private fun loadInvitations() {
        viewModelScope.launch {
            try {
                val userId = tokenRepository.getCurrentUserId()
                if (userId != null) {
                    _state.value = _state.value.copy(isInvitationsLoading = true, invitationsError = null)

                    invitationRepository.getUserInvitationsFlow(userId).collect { response ->
                        when (response) {
                            is StoreReadResponse.Data -> {
                                _state.value = _state.value.copy(
                                    invitations = response.value,
                                    isInvitationsLoading = false,
                                    invitationsError = null
                                )
                            }
                            is StoreReadResponse.Error.Exception -> {
                                _state.value = _state.value.copy(
                                    invitationsError = response.error.message ?: "Failed to load invitations",
                                    isInvitationsLoading = false
                                )
                            }
                            is StoreReadResponse.Error.Message -> {
                                _state.value = _state.value.copy(
                                    invitationsError = response.message,
                                    isInvitationsLoading = false
                                )
                            }
                            is StoreReadResponse.Loading -> {
                                _state.value = _state.value.copy(isInvitationsLoading = true)
                            }
                            else -> {
                                // Handle other states
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    invitationsError = e.message ?: "Failed to load invitations",
                    isInvitationsLoading = false
                )
            }
        }
    }

    fun refreshInvitations() {
        viewModelScope.launch {
            try {
                val userId = tokenRepository.getCurrentUserId()
                if (userId != null) {
                    invitationRepository.refreshUserInvitations(userId)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    invitationsError = e.message ?: "Failed to refresh invitations"
                )
            }
        }
    }

    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                // Add to processing set
                _state.value = _state.value.copy(
                    processingInvitationIds = _state.value.processingInvitationIds + invitationId,
                    invitationsError = null
                )

                val result = invitationRepository.acceptInvitation(invitationId)
                result.fold(
                    onSuccess = { response ->
                        // Remove from processing set
                        _state.value = _state.value.copy(
                            processingInvitationIds = _state.value.processingInvitationIds - invitationId
                        )

                        // Refresh invitations to remove accepted one
                        refreshInvitations()
                        // Also refresh workspaces to show new workspace
                        refreshWorkspaces()
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            processingInvitationIds = _state.value.processingInvitationIds - invitationId,
                            invitationsError = error.message ?: "Failed to accept invitation"
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    processingInvitationIds = _state.value.processingInvitationIds - invitationId,
                    invitationsError = e.message ?: "Failed to accept invitation"
                )
            }
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                // Add to processing set
                _state.value = _state.value.copy(
                    processingInvitationIds = _state.value.processingInvitationIds + invitationId,
                    invitationsError = null
                )

                val result = invitationRepository.rejectInvitation(invitationId)
                result.fold(
                    onSuccess = { response ->
                        // Remove from processing set
                        _state.value = _state.value.copy(
                            processingInvitationIds = _state.value.processingInvitationIds - invitationId
                        )

                        // Refresh invitations to remove rejected one
                        refreshInvitations()
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            processingInvitationIds = _state.value.processingInvitationIds - invitationId,
                            invitationsError = error.message ?: "Failed to reject invitation"
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    processingInvitationIds = _state.value.processingInvitationIds - invitationId,
                    invitationsError = e.message ?: "Failed to reject invitation"
                )
            }
        }
    }

    fun clearInvitationsError() {
        _state.value = _state.value.copy(invitationsError = null)
    }
}