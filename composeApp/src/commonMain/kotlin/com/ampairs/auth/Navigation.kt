package com.ampairs.auth

import AuthRoute
import Route
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.domain.LoginStatus
import com.ampairs.auth.ui.LoginScreen
import com.ampairs.auth.ui.OtpScreen
import com.ampairs.auth.ui.PhoneScreen
import com.ampairs.auth.ui.UserSelectionScreen
import com.ampairs.auth.ui.UserUpdateScreen
import org.koin.compose.koinInject
import getPlatformName

fun NavGraphBuilder.authNavigation(navigator: NavController, onLoginSuccess: () -> Unit) {

    navigation<Route.Login>(startDestination = AuthRoute.UserSelection) {

        composable<AuthRoute.UserSelection> {
            val tokenRepository = koinInject<TokenRepository>()
            val userWorkspaceRepository = koinInject<UserWorkspaceRepository>()

            UserSelectionScreen(
                onUserSelected = { userId ->
                    // Set the selected user as current and check their state
                    kotlinx.coroutines.runBlocking {
                        tokenRepository.setCurrentUser(userId)
                        val hasSelectedWorkspace =
                            userWorkspaceRepository.getWorkspaceIdForUser(userId).isNotBlank()
                        if (hasSelectedWorkspace) {
                            // User has selected workspace, go to main app
                            onLoginSuccess()
                        } else {
                            // User needs to select workspace, go to workspace selection
                            navigator.navigate(Route.Workspace) {
                                popUpTo(Route.Login) { inclusive = true }
                            }
                        }
                    }
                },
                onAddNewUser = {
                    // Navigate to appropriate auth screen based on platform
                    val authRoute = getAuthRouteForPlatform()
                    navigator.navigate(authRoute)
                },
                onNoUsers = {
                    // If no users, go directly to login
                    navigator.navigate(AuthRoute.LoginRoot) {
                        popUpTo(AuthRoute.UserSelection) { inclusive = true }
                    }
                }
            )
        }

        composable<AuthRoute.LoginRoot> {
            val tokenRepository = koinInject<TokenRepository>()
            val userWorkspaceRepository = koinInject<UserWorkspaceRepository>()

            LoginScreen { loginStatus, userEntity ->
                if (loginStatus == LoginStatus.LOGGED_IN) {
                    // Check if user's first name is empty, then navigate to UserUpdate screen
                    if (userEntity?.first_name.isNullOrBlank()) {
                        navigator.navigate(AuthRoute.UserUpdate)
                    } else {
                        kotlinx.coroutines.runBlocking {
                            // Add user to multi-user system if they have tokens
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

                            // Check if user has selected a workspace
                            val hasSelectedWorkspace =
                                userWorkspaceRepository.getWorkspaceIdForUser(userId = userEntity.id)
                                    .isNotBlank()
                            if (hasSelectedWorkspace) {
                                // User has selected workspace, go to main app
                                onLoginSuccess()
                            } else {
                                // User needs to select workspace, go to workspace selection
                                navigator.navigate(Route.Workspace) {
                                    popUpTo(Route.Login) { inclusive = true }
                                }
                            }
                        }
                    }
                } else {
                    // Navigate to appropriate auth screen based on platform
                    val authRoute = getAuthRouteForPlatform()
                    navigator.navigate(authRoute)
                }
            }
        }

        // Desktop Browser Authentication Route
        composable<AuthRoute.DesktopBrowserAuth> {
            val tokenRepository = koinInject<TokenRepository>()
            val userWorkspaceRepository = koinInject<UserWorkspaceRepository>()

            DesktopBrowserAuthScreen {
                // After successful browser authentication, check workspace status
                kotlinx.coroutines.runBlocking {
                    val currentUserId = tokenRepository.getCurrentUserId()
                    if (currentUserId != null) {
                        val hasSelectedWorkspace =
                            userWorkspaceRepository.getWorkspaceIdForUser(currentUserId).isNotBlank()
                        if (hasSelectedWorkspace) {
                            // User has selected workspace, go to main app
                            onLoginSuccess()
                        } else {
                            // User needs to select workspace, go to workspace selection
                            navigator.navigate(Route.Workspace) {
                                popUpTo(Route.Login) { inclusive = true }
                            }
                        }
                    }
                }
            }
        }

        composable<AuthRoute.Phone> {
            val tokenRepository = koinInject<TokenRepository>()
            val userWorkspaceRepository = koinInject<UserWorkspaceRepository>()

            PhoneScreen(
                onAuthSuccess = { sessionId, verificationId ->
                    navigator.navigate(AuthRoute.Otp(
                        sessionId = sessionId,
                        verificationId = verificationId
                    ))
                },
                onExistingUserSelected = {
                    // When existing user is selected, check their workspace status
                    kotlinx.coroutines.runBlocking {
                        val currentUserId = tokenRepository.getCurrentUserId()
                        if (currentUserId != null) {
                            val hasSelectedWorkspace =
                                userWorkspaceRepository.getWorkspaceIdForUser(currentUserId).isNotBlank()
                            if (hasSelectedWorkspace) {
                                // User has selected workspace, go to main app
                                onLoginSuccess()
                            } else {
                                // User needs to select workspace, go to workspace selection
                                navigator.navigate(Route.Workspace) {
                                    popUpTo(Route.Login) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            )
        }
        composable<AuthRoute.Otp> { backStackEntry ->
            val otp = backStackEntry.toRoute<AuthRoute.Otp>()
            OtpScreen(
                sessionId = otp.sessionId,
                verificationId = otp.verificationId,
                onAuthSuccess = {
                    navigator.navigate(AuthRoute.UserUpdate)
                }
            )
        }
        composable<AuthRoute.UserUpdate> {
            val tokenRepository = koinInject<TokenRepository>()
            val userWorkspaceRepository = koinInject<UserWorkspaceRepository>()

            UserUpdateScreen {
                kotlinx.coroutines.runBlocking {
                    // Add user to multi-user system after profile update
                    val accessToken = tokenRepository.getAccessToken()
                    val refreshToken = tokenRepository.getRefreshToken()

                    // Get current user after update (should be available from the UserUpdateScreen)
                    if (!accessToken.isNullOrBlank()) {
                        val currentUserId = tokenRepository.getCurrentUserId()
                        if (currentUserId != null) {
                            tokenRepository.addAuthenticatedUser(
                                userId = currentUserId,
                                accessToken = accessToken,
                                refreshToken = refreshToken
                            )

                            // After user update, check if workspace is selected
                            val hasSelectedWorkspace =
                                userWorkspaceRepository.getWorkspaceIdForUser(currentUserId)
                                    .isNotBlank()
                            if (hasSelectedWorkspace) {
                                // User has selected workspace, go to main app
                                onLoginSuccess()
                            } else {
                                // User needs to select workspace, go to workspace selection
                                navigator.navigate(Route.Workspace) {
                                    popUpTo(Route.Login) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get the appropriate authentication route for the current platform
 * Desktop uses browser-based authentication, mobile uses phone authentication
 */
private fun getAuthRouteForPlatform(): AuthRoute {
    return when (getPlatformName()) {
        "Desktop" -> AuthRoute.DesktopBrowserAuth
        else -> AuthRoute.Phone
    }
}

/**
 * Desktop Browser Authentication Screen - expect/actual pattern
 * This allows the common code to reference it, but only desktop provides implementation
 */
@Composable
expect fun DesktopBrowserAuthScreen(onAuthSuccess: () -> Unit)