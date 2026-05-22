package com.ampairs.navigation.providers

import AuthRoute
import Route
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.auth.DesktopBrowserAuthScreen
import com.ampairs.auth.ui.AccountDeletionScreen
import com.ampairs.auth.ui.AccountRestoreScreen
import com.ampairs.auth.ui.LoginScreen
import com.ampairs.auth.ui.OtpScreen
import com.ampairs.auth.ui.PhoneScreen
import com.ampairs.auth.ui.UserSelectionScreen
import com.ampairs.auth.ui.UserUpdateScreen
import com.ampairs.auth.viewmodel.LoginNavEvent
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.auth.viewmodel.UserSelectionNavEvent
import com.ampairs.auth.viewmodel.UserSelectionViewModel
import com.ampairs.auth.viewmodel.UserUpdateNavEvent
import com.ampairs.auth.viewmodel.UserUpdateViewModel
import com.ampairs.common.localization.LocalLocaleManager
import com.ampairs.common.state.AppHeaderStateManager
import dev.zacsweers.metrox.viewmodel.metroViewModel
import getPlatformName
import kotlinx.coroutines.flow.collectLatest

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
        val viewModel: UserSelectionViewModel = metroViewModel()
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                val userInfo = when (event) {
                    is UserSelectionNavEvent.LoginSuccess -> event.userInfo
                    is UserSelectionNavEvent.NavigateToWorkspace -> event.userInfo
                }
                userInfo?.let { AppHeaderStateManager.instance.updateUser(it) }
                when (event) {
                    is UserSelectionNavEvent.LoginSuccess -> onLoginSuccess()
                    is UserSelectionNavEvent.NavigateToWorkspace -> {
                        backStack.clear()
                        backStack.add(Route.Workspace)
                    }
                }
            }
        }
        UserSelectionScreen(
            onAddNewUser = {
                backStack.add(getAuthRouteForPlatform())
            },
            onNoUsers = {
                backStack.clear()
                backStack.add(AuthRoute.LoginRoot)
            }
        )
    }

    is AuthRoute.LoginRoot -> NavEntry(key) {
        LoginScreen(
            localeManager = LocalLocaleManager.current,
            onLoginSuccess = onLoginSuccess,
            onNavigateToWorkspace = {
                backStack.clear()
                backStack.add(Route.Workspace)
            },
            onNavigateToUserUpdate = { backStack.add(AuthRoute.UserUpdate) },
            onNavigateToAuthRoute = { backStack.add(getAuthRouteForPlatform()) }
        )
    }

    is AuthRoute.DesktopBrowserAuth -> NavEntry(key) {
        val viewModel: LoginViewModel = metroViewModel()
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                when (event) {
                    LoginNavEvent.LoginSuccess -> onLoginSuccess()
                    LoginNavEvent.NavigateToWorkspace -> {
                        backStack.clear()
                        backStack.add(Route.Workspace)
                    }
                    else -> {}
                }
            }
        }
        DesktopBrowserAuthScreen {
            viewModel.handleExistingUserWorkspaceCheck()
        }
    }

    is AuthRoute.Phone -> NavEntry(key) {
        val viewModel: LoginViewModel = metroViewModel()
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                when (event) {
                    LoginNavEvent.LoginSuccess -> onLoginSuccess()
                    LoginNavEvent.NavigateToWorkspace -> {
                        backStack.clear()
                        backStack.add(Route.Workspace)
                    }
                    else -> {}
                }
            }
        }
        PhoneScreen(
            viewModel = viewModel,
            onAuthSuccess = { sessionId, verificationId ->
                backStack.add(
                    AuthRoute.Otp(
                        sessionId = sessionId,
                        verificationId = verificationId
                    )
                )
            }
        )
    }

    is AuthRoute.Otp -> NavEntry(key) {
        val viewModel: LoginViewModel = metroViewModel()
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                when (event) {
                    LoginNavEvent.NavigateToUserUpdate -> backStack.add(AuthRoute.UserUpdate)
                    LoginNavEvent.NavigateToAccountRestore -> backStack.add(AuthRoute.AccountRestore)
                    else -> {}
                }
            }
        }
        OtpScreen(
            viewModel = viewModel,
            sessionId = key.sessionId,
            verificationId = key.verificationId,
            onAuthSuccess = { viewModel.handleOtpSuccess() }
        )
    }

    is AuthRoute.UserUpdate -> NavEntry(key) {
        val viewModel: UserUpdateViewModel = metroViewModel()
        LaunchedEffect(Unit) {
            viewModel.navEvent.collectLatest { event ->
                val userInfo = when (event) {
                    is UserUpdateNavEvent.LoginSuccess -> event.userInfo
                    is UserUpdateNavEvent.NavigateToWorkspace -> event.userInfo
                }
                userInfo?.let { AppHeaderStateManager.instance.updateUser(it) }
                when (event) {
                    is UserUpdateNavEvent.LoginSuccess -> onLoginSuccess()
                    is UserUpdateNavEvent.NavigateToWorkspace -> {
                        backStack.clear()
                        backStack.add(Route.Workspace)
                    }
                }
            }
        }
        UserUpdateScreen(
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
        AccountDeletionScreen(
            onDeletionSuccess = {
                backStack.clear()
                backStack.add(Route.Login)
            },
            onNavigateBack = {
                backStack.removeLastOrNull()
            }
        )
    }

    is AuthRoute.AccountRestore -> NavEntry(key) {
        AccountRestoreScreen(
            onRestoreSuccess = {
                backStack.add(AuthRoute.UserUpdate)
            },
            onLogout = {
                backStack.clear()
                backStack.add(Route.Login)
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
