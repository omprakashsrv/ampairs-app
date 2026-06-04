package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserDataService
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreManager
import com.ampairs.common.workspace.WorkspaceActivator
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.db.UserInvitationRepository
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.ui.WorkspaceListState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface WorkspaceListEvent {
    data class NavigateToModules(val workspaceId: String, val workspaceSlug: String) : WorkspaceListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class WorkspaceListViewModel(
    private val workspaceRepository: OfflineFirstWorkspaceRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val tokenRepository: TokenRepository,
    private val userDataService: UserDataService,
    private val invitationRepository: UserInvitationRepository,
    private val workspaceActivator: WorkspaceActivator,
    private val appPreferences: AppPreferencesDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceListState())
    val state: StateFlow<WorkspaceListState> = _state.asStateFlow()

    private val _events = Channel<WorkspaceListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadUserData()
        observeWorkspaces()
        observeInvitations()
        viewModelScope.launch { syncWorkspaces() }
        viewModelScope.launch { syncInvitations() }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUserLoading = true)
            try {
                val fullName = userDataService.getUserDisplayName() ?: "User"
                _state.value = _state.value.copy(userFullName = fullName, isUserLoading = false)
            } catch (_: Exception) {
                _state.value = _state.value.copy(userFullName = "User", isUserLoading = false)
            }
        }
    }

    private fun observeWorkspaces() {
        _state.value = _state.value.copy(isLoading = true)
        _searchQuery
            .flatMapLatest { query ->
                if (query.isEmpty()) workspaceRepository.observeWorkspaces()
                else workspaceRepository.searchWorkspaces(query)
            }
            .onEach { workspaces ->
                _state.value = _state.value.copy(
                    workspaces = workspaces,
                    isLoading = false,
                    isRefreshing = false,
                    hasNoWorkspaces = workspaces.isEmpty(),
                    error = null
                )
            }
            .catch { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load workspaces",
                    isOfflineMode = true
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeInvitations() {
        viewModelScope.launch {
            val userId = tokenRepository.getCurrentUserId() ?: return@launch
            _state.value = _state.value.copy(isInvitationsLoading = true)
            invitationRepository.observeUserInvitations(userId)
                .onEach { invitations ->
                    _state.value = _state.value.copy(
                        invitations = invitations,
                        isInvitationsLoading = false,
                        invitationsError = null
                    )
                }
                .catch { e ->
                    _state.value = _state.value.copy(
                        isInvitationsLoading = false,
                        invitationsError = e.message ?: "Failed to load invitations"
                    )
                }
                .launchIn(viewModelScope)
        }
    }

    private suspend fun syncWorkspaces() {
        workspaceRepository.syncWorkspaces()
    }

    private suspend fun syncInvitations() {
        val userId = tokenRepository.getCurrentUserId() ?: return
        invitationRepository.syncUserInvitations(userId)
    }

    fun selectWorkSpace(workspaceId: String) {
        viewModelScope.launch {
            val workspace = _state.value.workspaces.find { it.id == workspaceId } ?: return@launch
            val currentUserId = tokenRepository.getCurrentUserId() ?: return@launch

            val previousWorkspaceId = appPreferences.getLastWorkspaceId().first()
            val previousSlug = _state.value.workspaces.find { it.id == previousWorkspaceId }?.slug
            if (previousSlug != null && previousSlug != workspace.slug) {
                DataStoreManager.clearDataStoresForWorkspace(previousSlug)
            }

            userWorkspaceRepository.setWorkspaceIdForUser(currentUserId, workspaceId)
            appPreferences.setLastWorkspaceId(workspaceId)
            // Immediately refresh the in-memory token-repo cache so that the Ktor
            // defaultRequest plugin sends the correct X-Workspace-ID header for
            // every API call that follows (including syncFromRemote in new VMs).
            tokenRepository.getWorkspaceId()

            // Activate Metro workspace graph FIRST (tears down old graph, creates new one),
            // then update the legacy global singleton so both systems reflect the same workspace.
            workspaceActivator.activateWorkspace(workspaceId, workspace.slug, currentUserId)

            GlobalNavigationManager.getInstance().onWorkspaceSelected()
            _events.send(WorkspaceListEvent.NavigateToModules(workspaceId, workspace.slug))
        }
    }

    fun loadWorkspaces() = refreshWorkspaces()

    fun refreshWorkspaces() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            syncWorkspaces()
        }
    }

    fun searchWorkspaces(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        _searchQuery.value = query
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            tokenRepository.clearTokens()
        }
    }

    fun refreshInvitations() {
        viewModelScope.launch {
            syncInvitations()
        }
    }

    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    processingInvitationIds = _state.value.processingInvitationIds + invitationId,
                    invitationsError = null
                )
                val result = invitationRepository.acceptInvitation(invitationId)
                result.fold(
                    onSuccess = {
                        _state.value = _state.value.copy(
                            processingInvitationIds = _state.value.processingInvitationIds - invitationId
                        )
                        refreshInvitations()
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
                _state.value = _state.value.copy(
                    processingInvitationIds = _state.value.processingInvitationIds + invitationId,
                    invitationsError = null
                )
                val result = invitationRepository.rejectInvitation(invitationId)
                result.fold(
                    onSuccess = {
                        _state.value = _state.value.copy(
                            processingInvitationIds = _state.value.processingInvitationIds - invitationId
                        )
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
