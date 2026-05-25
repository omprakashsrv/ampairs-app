package com.ampairs.common.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.domain.Workspace
import com.ampairs.workspace.domain.WorkspaceList
import com.ampairs.workspace.integration.WorkspaceContextIntegration
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class GlobalAppLayoutViewModel(
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val tokenRepository: com.ampairs.auth.api.TokenRepository,
    private val analytics: FirebaseAnalytics,
    private val appPreferences: AppPreferencesDataStore,
) : ViewModel() {

    sealed interface NavEvent {
        object NavigateToLogin : NavEvent
        object NavigateToWorkspace : NavEvent
        object NavigateToUserSelection : NavEvent
    }

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent: SharedFlow<NavEvent> = _navEvent.asSharedFlow()

    init {
        loadHeaderData()
        observeWorkspaceContext()
    }

    // Observe WorkspaceContextManager so the header updates immediately whenever
    // the user switches workspaces (WorkspaceContextIntegration.setWorkspaceFromDomain
    // updates WorkspaceContextManager but not AppHeaderStateManager directly).
    private fun observeWorkspaceContext() {
        viewModelScope.launch {
            WorkspaceContextManager.getInstance().currentWorkspace.collect { context ->
                if (context != null) {
                    loadWorkspaceById(context.id)
                }
            }
        }
    }

    private fun loadHeaderData() {
        viewModelScope.launch {
            try {
                userRepository.getUser()?.let { userEntity ->
                    val profilePictureUrl = userEntity.profile_picture_url?.let { url ->
                        if (url.isNotBlank() && !url.startsWith("http")) ApiUrlBuilder.currentUserPictureUrl()
                        else url.takeIf { it.isNotBlank() }
                    }
                    val profilePictureThumbnailUrl = userEntity.profile_picture_thumbnail_url?.let { url ->
                        if (url.isNotBlank() && !url.startsWith("http")) ApiUrlBuilder.currentUserPictureThumbnailUrl()
                        else url.takeIf { it.isNotBlank() }
                    }
                    val userInfo = UserInfo(
                        id = userEntity.id,
                        firstName = userEntity.first_name,
                        lastName = userEntity.last_name,
                        userName = userEntity.user_name,
                        countryCode = userEntity.country_code,
                        phone = userEntity.phone,
                        profilePictureUrl = profilePictureUrl,
                        profilePictureThumbnailUrl = profilePictureThumbnailUrl,
                        lastLogin = 0L,
                        loginCount = 0,
                        isAuthenticated = true,
                        hasSelectedWorkspace = true
                    )
                    AppHeaderStateManager.instance.updateUser(userInfo)
                    analytics.setUserId(userEntity.id)

                    val selectedWorkspaceId = userWorkspaceRepository.getWorkspaceIdForUser(userEntity.id)
                    if (selectedWorkspaceId.isNotEmpty()) {
                        loadWorkspaceById(selectedWorkspaceId)
                    } else {
                        loadFirstWorkspace()
                    }
                }
            } catch (e: Exception) {
                // Header will remain in its default state
            }
        }
    }

    private suspend fun loadWorkspaceById(workspaceId: String) {
        workspaceRepository.getWorkspaceById(workspaceId)?.let { workspace ->
            AppHeaderStateManager.instance.updateWorkspace(workspace.toWorkspaceList())
        }
    }

    private suspend fun loadFirstWorkspace() {
        workspaceRepository.getLocalWorkspaces().firstOrNull()?.firstOrNull()?.let { workspace ->
            AppHeaderStateManager.instance.updateWorkspace(workspace.toWorkspaceList())
        }
    }

    fun clearWorkspace() {
        viewModelScope.launch {
            WorkspaceContextIntegration.clearWorkspaceContext()
            appPreferences.clearLastWorkspaceId()
            _navEvent.emit(NavEvent.NavigateToWorkspace)
        }
    }

    fun logout() {
        viewModelScope.launch {
            analytics.logEvent(AnalyticsEvents.LOGOUT)
            analytics.setUserId(null)
            WorkspaceContextIntegration.clearWorkspaceContext()
            val currentUserId = tokenRepository.getCurrentUserId()
            currentUserId?.let { userId ->
                try {
                    tokenRepository.logoutUser(userId)
                    userRepository.deleteUserById(userId)
                } catch (e: Exception) { }
            }
            appPreferences.clearLastWorkspaceId()
            appPreferences.clearLastUserId()
            AppHeaderStateManager.instance.reset()
            _navEvent.emit(NavEvent.NavigateToLogin)
        }
    }

    fun switchUser() {
        viewModelScope.launch {
            analytics.setUserId(null)
            tokenRepository.clearCurrentUser()
            appPreferences.clearLastUserId()
            appPreferences.clearLastWorkspaceId()
            WorkspaceContextIntegration.clearWorkspaceContext()
            AppHeaderStateManager.instance.reset()
            _navEvent.emit(NavEvent.NavigateToUserSelection)
        }
    }
}

private fun Workspace.toWorkspaceList() = WorkspaceList(
    id = id,
    name = name,
    slug = slug,
    description = description,
    workspaceType = workspaceType,
    avatarUrl = avatarUrl,
    subscriptionPlan = subscriptionPlan,
    memberCount = memberCount ?: 1,
    lastActivityAt = lastActivityAt,
    createdAt = createdAt
)
