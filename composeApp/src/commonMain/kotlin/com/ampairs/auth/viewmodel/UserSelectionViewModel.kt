package com.ampairs.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserSelectionState(
    val users: List<UserInfo> = emptyList(),
    val isLoading: Boolean = true, // Start with loading = true to prevent race condition
    val error: String? = null
)

class UserSelectionViewModel(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val analytics: FirebaseAnalytics,
) : ViewModel() {

    private val _state = MutableStateFlow(UserSelectionState())
    val state: StateFlow<UserSelectionState> = _state.asStateFlow()

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
                        userInfoList.add(
                            UserInfo(
                                id = user.id,
                                firstName = user.first_name,
                                lastName = user.last_name,
                                userName = user.user_name,
                                countryCode = user.country_code,
                                phone = user.phone,
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

                // Set Firebase Analytics user ID
                analytics.setUserId(userId)
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