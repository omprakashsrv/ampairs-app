package com.ampairs.common.ui

import AuthRoute
import Route
import WorkspaceRoute
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.navigation.NavigationPattern
import com.ampairs.workspace.navigation.PlatformNavigationDetector
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Global App Layout for Navigation 3 that wraps NavDisplay.
 * Uses MutableList<NavKey> backStack instead of NavHostController.
 * Initializes header data ONCE and recomposes only on state changes.
 *
 * Navigation pattern:
 * - Mobile (SIDE_DRAWER): NavigationBar at bottom — no hamburger drawer.
 * - Desktop (MENU_BAR): NavigationRail on left + native system MenuBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAppLayoutNav3(
    backStack: MutableList<NavKey>,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val viewModel: GlobalAppLayoutViewModel = metroViewModel()
    val headerStateManager = remember { AppHeaderStateManager.instance }
    val headerState by headerStateManager.headerState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navEvent.collectLatest { event ->
            when (event) {
                GlobalAppLayoutViewModel.NavEvent.NavigateToLogin -> {
                    backStack.clear()
                    backStack.add(Route.Login)
                }
                GlobalAppLayoutViewModel.NavEvent.NavigateToWorkspace -> {
                    backStack.clear()
                    backStack.add(Route.Workspace)
                }
                GlobalAppLayoutViewModel.NavEvent.NavigateToUserSelection -> {
                    backStack.clear()
                    backStack.add(AuthRoute.UserSelection)
                }
            }
        }
    }

    var currentRoute by remember { mutableStateOf<NavKey?>(null) }
    var isWorkspaceSelection by remember { mutableStateOf(false) }
    var shouldShowHeader by remember { mutableStateOf(false) }

    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collectLatest { route ->
                currentRoute = route
                isWorkspaceSelection = route is WorkspaceRoute.Root ||
                        route is WorkspaceRoute.Create ||
                        route is WorkspaceRoute.Edit
                shouldShowHeader = route != null &&
                        route !is AuthRoute &&
                        route !is Route.Login
            }
    }

    BackNavigationHandlerNav3(
        backStack = backStack,
        enabled = shouldShowHeader,
        fallbackRoute = if (isWorkspaceSelection) Route.Login else Route.Workspace
    )

    if (!shouldShowHeader) {
        content(PaddingValues())
        return
    }

    val currentWorkspaceName = if (isWorkspaceSelection) null else headerState.currentWorkspace?.name
    val currentWorkspaceId = if (isWorkspaceSelection) null else headerState.currentWorkspace?.id
    val workspaceAvatarUrl = if (isWorkspaceSelection) null else headerState.currentWorkspace?.avatarUrl
    val userFullName = "${headerState.currentUser?.firstName ?: ""} ${headerState.currentUser?.lastName ?: ""}".trim()
        .ifEmpty { "User" }
    val profilePictureThumbnailUrl = headerState.currentUser?.profilePictureThumbnailUrl

    val onWorkspaceClick: () -> Unit = remember(isWorkspaceSelection) {
        { if (!isWorkspaceSelection) viewModel.clearWorkspace() }
    }
    val onEditProfile: () -> Unit = remember { { backStack.add(AuthRoute.UserUpdate) } }
    val onLogout: () -> Unit = remember { { viewModel.logout() } }
    val onSwitchUser: () -> Unit = remember { { viewModel.switchUser() } }
    val onDeleteAccount: () -> Unit = remember { { backStack.add(AuthRoute.AccountDeletion) } }

    val hasActiveWorkspace = !currentWorkspaceId.isNullOrBlank()
    val navigationPattern = remember { PlatformNavigationDetector.getNavigationPattern() }

    // Keep global navigation manager updated for the desktop system MenuBar.
    val globalNavManager = remember { GlobalNavigationManager.getInstance() }
    val navigationService by globalNavManager.navigationService.collectAsState()

    when {
        // ── Mobile: NavigationBar at bottom, no top bar ─────────────────────
        navigationPattern == NavigationPattern.SIDE_DRAWER && hasActiveWorkspace -> {
            Scaffold(
                modifier = modifier.imePadding(),
                bottomBar = {
                    AppBottomNavigation(
                        backStack = backStack,
                        currentRoute = currentRoute
                    )
                },
            ) { paddingValues ->
                content(paddingValues)
            }
        }

        // ── Desktop: NavigationRail on left + system MenuBar ─────────────────
        navigationPattern == NavigationPattern.MENU_BAR && hasActiveWorkspace -> {
            Row(modifier = modifier.fillMaxSize().imePadding()) {
                AppNavigationRail(
                    backStack = backStack,
                    currentRoute = currentRoute
                )
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        AppHeaderNav3(
                            backStack = backStack,
                            currentWorkspaceName = currentWorkspaceName,
                            currentWorkspaceId = currentWorkspaceId,
                            workspaceAvatarUrl = workspaceAvatarUrl,
                            userFullName = userFullName,
                            profilePictureThumbnailUrl = profilePictureThumbnailUrl,
                            isUserLoading = headerState.isUserLoading,
                            isWorkspaceLoading = headerState.isWorkspaceLoading,
                            onWorkspaceClick = onWorkspaceClick,
                            onEditProfile = onEditProfile,
                            onLogout = onLogout,
                            onSwitchUser = onSwitchUser,
                            onDeleteAccount = onDeleteAccount
                        )
                    },
                ) { paddingValues ->
                    content(paddingValues)
                }
            }
        }

        // ── Workspace selection / no active workspace ────────────────────────
        else -> {
            Scaffold(
                modifier = modifier.imePadding(),
                topBar = {
                    AppHeaderNav3(
                        backStack = backStack,
                        currentWorkspaceName = currentWorkspaceName,
                        currentWorkspaceId = currentWorkspaceId,
                        workspaceAvatarUrl = workspaceAvatarUrl,
                        userFullName = userFullName,
                        profilePictureThumbnailUrl = profilePictureThumbnailUrl,
                        isUserLoading = headerState.isUserLoading,
                        isWorkspaceLoading = headerState.isWorkspaceLoading,
                        onWorkspaceClick = onWorkspaceClick,
                        onEditProfile = onEditProfile,
                        onLogout = onLogout,
                        onSwitchUser = onSwitchUser,
                        onDeleteAccount = onDeleteAccount
                    )
                },
            ) { paddingValues ->
                content(paddingValues)
            }
        }
    }
}

/**
 * Back navigation handler for Navigation 3.
 * Uses platform-specific back button handling with Nav3 back stack.
 */
@Composable
expect fun BackNavigationHandlerNav3(
    backStack: MutableList<NavKey>,
    enabled: Boolean,
    fallbackRoute: NavKey
)
