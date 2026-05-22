package com.ampairs.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
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
import kotlinx.coroutines.launch

data class UserSelectionState(
    val users: List<UserInfo> = emptyList(),
    val isLoading: Boolean = true, // Start with loading = true to prevent race condition
    val error: String? = null
)

sealed interface UserSelectionNavEvent {
    data class LoginSuccess(val userInfo: UserInfo?) : UserSelectionNavEvent
    data class NavigateToWorkspace(val userInfo: UserInfo?) : UserSelectionNavEvent
}

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class UserSelectionViewModel(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val analytics: FirebaseAnalytics,
    private val appPreferences: AppPreferencesDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(UserSelectionState())
    val state: StateFlow<UserSelectionState> = _state.asStateFlow()

    private val _navEvent = MutableSharedFlow<UserSelectionNavEvent>()
    val navEvent: SharedFlow<UserSelectionNavEvent> = _navEvent.asSharedFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val activeUserIds = tokenRepository.getAllAuthenticatedUsers()
                val userInfoList = mutableListOf<UserInfo>()
                
                for (userId in activeUserIds) {
                    val user = userRepository.getUserById(userId)
                    val isAuthenticated = tokenRepository.isUserAuthenticated(userId)
                    val workspaceId = userWorkspaceRepository.getWorkspaceIdForUser(userId)
                    
                    if (user != null) {
                        // NOTE: Profile picture URLs are NOT provided here because:
                        // 1. On User Selection screen, no user is currently logged in
                        // 2. Profile picture API endpoints require authentication
                        // 3. We can't make authenticated requests without a current user
                        // Profile pictures will show as initials on this screen
                        userInfoList.add(
                            UserInfo(
                                id = user.id,
                                firstName = user.first_name,
                                lastName = user.last_name,
                                userName = user.user_name,
                                countryCode = user.country_code,
                                phone = user.phone,
                                profilePictureUrl = null, // Not available without auth
                                profilePictureThumbnailUrl = null, // Not available without auth
                                isAuthenticated = isAuthenticated,
                                hasSelectedWorkspace = workspaceId.isNotBlank()
                            )
                        )
                    }
                }
                
                _state.value = _state.value.copy(
                    users = userInfoList.sortedByDescending { it.lastLogin },
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load users",
                    isLoading = false
                )
            }
        }
    }

    fun selectUser(userId: String) {
        viewModelScope.launch {
            try {
                tokenRepository.setCurrentUser(userId)
                appPreferences.setLastUserId(userId)
                analytics.setUserId(userId)

                var userInfo: UserInfo? = null
                userRepository.getUserById(userId)?.let { userEntity ->
                    val profilePictureUrl = userEntity.profile_picture_url?.let { url ->
                        if (url.isNotBlank() && !url.startsWith("http")) ApiUrlBuilder.currentUserPictureUrl()
                        else url.takeIf { it.isNotBlank() }
                    }
                    val profilePictureThumbnailUrl = userEntity.profile_picture_thumbnail_url?.let { url ->
                        if (url.isNotBlank() && !url.startsWith("http")) ApiUrlBuilder.currentUserPictureThumbnailUrl()
                        else url.takeIf { it.isNotBlank() }
                    }
                    userInfo = UserInfo(
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
                }

                val hasWorkspace = userWorkspaceRepository.getWorkspaceIdForUser(userId).isNotBlank()
                if (hasWorkspace) {
                    _navEvent.emit(UserSelectionNavEvent.LoginSuccess(userInfo))
                } else {
                    _navEvent.emit(UserSelectionNavEvent.NavigateToWorkspace(userInfo))
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to select user"
                )
            }
        }
    }

    fun removeUser(userId: String) {
        viewModelScope.launch {
            try {
                tokenRepository.logoutUser(userId)
                userRepository.deleteUser(userId)

                // Clear Firebase Analytics user ID if removing current user
                val currentUserId = tokenRepository.getCurrentUserId()
                if (currentUserId == userId) {
                    analytics.setUserId(null)
                }

                // Reload the user list
                loadUsers()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to remove user"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}