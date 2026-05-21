package com.ampairs.navigation.providers

import AuthRoute
import Route
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.auth.DesktopBrowserAuthScreen
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.LoginStatus
import com.ampairs.auth.domain.UserInfo
import com.ampairs.auth.ui.AccountDeletionScreen
import com.ampairs.auth.ui.AccountRestoreScreen
import com.ampairs.auth.ui.LoginScreen
import com.ampairs.auth.ui.OtpScreen
import com.ampairs.auth.ui.PhoneScreen
import com.ampairs.auth.ui.UserSelectionScreen
import com.ampairs.auth.ui.UserUpdateScreen
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.localization.LocalLocaleManager
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.di.LocalAppGraph
import getPlatformName

/**
 * Entry provider for Auth module routes in Navigation 3.
 * Returns NavEntry for auth routes or null if route doesn't match.
 */
fun authEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
    onLoginSuccess: () -> Unit
): NavEntry<NavKey>? = when (key) {
    is AuthRoute.UserSelection -> NavEntry(key) {
        val tokenRepository = LocalAppGraph.current.tokenRepository
        val userWorkspaceRepository = LocalAppGraph.current.userWorkspaceRepository
        val userRepository = LocalAppGraph.current.userRepository
        val appPreferences = LocalAppGraph.current.appPreferences

        UserSelectionScreen(
            appPreferences = appPreferences,
            onUserSelected = { userId ->
                kotlinx.coroutines.runBlocking {
                    tokenRepository.setCurrentUser(userId)

                    userRepository.getUserById(userId)?.let { userEntity ->
                        val profilePictureUrl = userEntity.profile_picture_url?.let { url ->
                            if (url.isNotBlank() && !url.startsWith("http")) {
                                ApiUrlBuilder.currentUserPictureUrl()
                            } else {
                                url.takeIf { it.isNotBlank() }
                            }
                        }
                        val profilePictureThumbnailUrl = userEntity.profile_picture_thumbnail_url?.let { url ->
                            if (url.isNotBlank() && !url.startsWith("http")) {
                                ApiUrlBuilder.currentUserPictureThumbnailUrl()
                            } else {
                                url.takeIf { it.isNotBlank() }
                            }
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
                    }

                    val hasSelectedWorkspace =
                        userWorkspaceRepository.getWorkspaceIdForUser(userId).isNotBlank()
                    if (hasSelectedWorkspace) {
                        onLoginSuccess()
                    } else {
                        backStack.clear()
                        backStack.add(Route.Workspace)
                    }
                }
            },
            onAddNewUser = {
                val authRoute = getAuthRouteForPlatform()
                backStack.add(authRoute)
            },
            onNoUsers = {
                backStack.clear()
                backStack.add(AuthRoute.LoginRoot)
            }
        )
    }

    is AuthRoute.LoginRoot -> NavEntry(key) {
        val tokenRepository = LocalAppGraph.current.tokenRepository
        val userWorkspaceRepository = LocalAppGraph.current.userWorkspaceRepository
        LoginScreen(
            localeManager = LocalLocaleManager.current,
            onLoginStatus = { loginStatus, userEntity ->
            if (loginStatus == LoginStatus.LOGGED_IN) {
                if (userEntity?.first_name.isNullOrBlank()) {
                    backStack.add(AuthRoute.UserUpdate)
                } else {
                    kotlinx.coroutines.runBlocking {
                        val accessToken = tokenRepository.getAccessToken()
                        val refreshToken = tokenRepository.getRefreshToken()

                        if (!accessToken.isNullOrBlank()) {
                            tokenRepository.addAuthenticatedUser(
                                userId = userEntity.id,
                                accessToken = accessToken,
                                refreshToken = refreshToken
                            )
                            tokenRepository.setCurrentUser(userEntity.id)
                        }

                        val hasSelectedWorkspace =
                            userWorkspaceRepository.getWorkspaceIdForUser(userId = userEntity.id)
                                .isNotBlank()
                        if (hasSelectedWorkspace) {
                            onLoginSuccess()
                        } else {
                            backStack.clear()
                            backStack.add(Route.Workspace)
                        }
                    }
                }
            } else {
                val authRoute = getAuthRouteForPlatform()
                backStack.add(authRoute)
            }
        })
    }

    is AuthRoute.DesktopBrowserAuth -> NavEntry(key) {
        val tokenRepository = LocalAppGraph.current.tokenRepository
        val userWorkspaceRepository = LocalAppGraph.current.userWorkspaceRepository

        DesktopBrowserAuthScreen {
            kotlinx.coroutines.runBlocking {
                val currentUserId = tokenRepository.getCurrentUserId()
                if (currentUserId != null) {
                    val hasSelectedWorkspace =
                        userWorkspaceRepository.getWorkspaceIdForUser(currentUserId)
                            .isNotBlank()
                    if (hasSelectedWorkspace) {
                        onLoginSuccess()
                    } else {
                        backStack.clear()
                        backStack.add(Route.Workspace)
                    }
                }
            }
        }
    }

    is AuthRoute.Phone -> NavEntry(key) {
        val tokenRepository = LocalAppGraph.current.tokenRepository
        val userWorkspaceRepository = LocalAppGraph.current.userWorkspaceRepository

        PhoneScreen(
            onAuthSuccess = { sessionId, verificationId ->
                backStack.add(
                    AuthRoute.Otp(
                        sessionId = sessionId,
                        verificationId = verificationId
                    )
                )
            },
            onExistingUserSelected = {
                kotlinx.coroutines.runBlocking {
                    val currentUserId = tokenRepository.getCurrentUserId()
                    if (currentUserId != null) {
                        val hasSelectedWorkspace =
                            userWorkspaceRepository.getWorkspaceIdForUser(currentUserId)
                                .isNotBlank()
                        if (hasSelectedWorkspace) {
                            onLoginSuccess()
                        } else {
                            backStack.clear()
                            backStack.add(Route.Workspace)
                        }
                    }
                }
            }
        )
    }

    is AuthRoute.Otp -> NavEntry(key) {
        val userRepository = LocalAppGraph.current.userRepository

        OtpScreen(
            sessionId = key.sessionId,
            verificationId = key.verificationId,
            onAuthSuccess = {
                kotlinx.coroutines.runBlocking {
                    val deletionStatusResponse = userRepository.getAccountDeletionStatus()
                    deletionStatusResponse.onSuccess {
                        if (this.isDeleted && this.canRestore) {
                            backStack.add(AuthRoute.AccountRestore)
                        } else {
                            backStack.add(AuthRoute.UserUpdate)
                        }
                    }.onError {
                        backStack.add(AuthRoute.UserUpdate)
                    }
                }
            }
        )
    }

    is AuthRoute.UserUpdate -> NavEntry(key) {
        val tokenRepository = LocalAppGraph.current.tokenRepository
        val userWorkspaceRepository = LocalAppGraph.current.userWorkspaceRepository
        val userRepository = LocalAppGraph.current.userRepository

        UserUpdateScreen(
            onUpdateSuccess = {
            kotlinx.coroutines.runBlocking {
                val accessToken = tokenRepository.getAccessToken()
                val refreshToken = tokenRepository.getRefreshToken()

                if (!accessToken.isNullOrBlank()) {
                    val currentUserId = tokenRepository.getCurrentUserId()
                    if (currentUserId != null) {
                        tokenRepository.addAuthenticatedUser(
                            userId = currentUserId,
                            accessToken = accessToken,
                            refreshToken = refreshToken
                        )

                        userRepository.getUserById(currentUserId)?.let { userEntity ->
                            val profilePictureUrl = userEntity.profile_picture_url?.let { url ->
                                if (url.isNotBlank() && !url.startsWith("http")) {
                                    ApiUrlBuilder.currentUserPictureUrl()
                                } else {
                                    url.takeIf { it.isNotBlank() }
                                }
                            }
                            val profilePictureThumbnailUrl = userEntity.profile_picture_thumbnail_url?.let { url ->
                                if (url.isNotBlank() && !url.startsWith("http")) {
                                    ApiUrlBuilder.currentUserPictureThumbnailUrl()
                                } else {
                                    url.takeIf { it.isNotBlank() }
                                }
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
                        }

                        val hasSelectedWorkspace =
                            userWorkspaceRepository.getWorkspaceIdForUser(currentUserId)
                                .isNotBlank()
                        if (hasSelectedWorkspace) {
                            onLoginSuccess()
                        } else {
                            backStack.clear()
                            backStack.add(Route.Workspace)
                        }
                    }
                }
            }
        },
            onProfilePictureUpdated = { newThumbnailUrl ->
                AppHeaderStateManager.instance.updateUser(
                    AppHeaderStateManager.instance.headerState.value.currentUser?.copy(
                        profilePictureThumbnailUrl = newThumbnailUrl
                    )
                )
            }
        )
    }

    is AuthRoute.AccountDeletion -> NavEntry(key) {
        val tokenRepository = LocalAppGraph.current.tokenRepository
        val userRepository = LocalAppGraph.current.userRepository

        AccountDeletionScreen(
            onDeletionSuccess = {
                kotlinx.coroutines.runBlocking {
                    val currentUserId = tokenRepository.getCurrentUserId()
                    tokenRepository.clearTokens()
                    currentUserId?.let { userId ->
                        try {
                            userRepository.deleteUserById(userId)
                        } catch (e: Exception) {
                            println("Failed to delete user entity: ${e.message}")
                        }
                    }
                    backStack.clear()
                    backStack.add(Route.Login)
                }
            },
            onNavigateBack = {
                backStack.removeLastOrNull()
            }
        )
    }

    is AuthRoute.AccountRestore -> NavEntry(key) {
        val tokenRepository = LocalAppGraph.current.tokenRepository
        val userRepository = LocalAppGraph.current.userRepository

        AccountRestoreScreen(
            onRestoreSuccess = {
                backStack.add(AuthRoute.UserUpdate)
            },
            onLogout = {
                kotlinx.coroutines.runBlocking {
                    val currentUserId = tokenRepository.getCurrentUserId()
                    tokenRepository.clearTokens()
                    currentUserId?.let { userId ->
                        try {
                            userRepository.deleteUserById(userId)
                        } catch (e: Exception) {
                            println("Failed to delete user entity: ${e.message}")
                        }
                    }
                    backStack.clear()
                    backStack.add(Route.Login)
                }
            }
        )
    }

    else -> null
}

/**
 * Get the appropriate authentication route for the current platform
 */
private fun getAuthRouteForPlatform(): AuthRoute {
    return when (getPlatformName()) {
        "Desktop" -> AuthRoute.DesktopBrowserAuth
        else -> AuthRoute.Phone
    }
}
