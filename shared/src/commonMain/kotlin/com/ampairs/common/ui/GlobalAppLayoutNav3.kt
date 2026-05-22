package com.ampairs.common.ui

import AuthRoute
import Route
import WorkspaceRoute
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.navigation.MobileNavigationDrawer
import com.ampairs.workspace.navigation.NavigationPattern
import com.ampairs.workspace.navigation.PlatformNavigationDetector
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import navigateToMenuItemNav3

/**
 * Global App Layout for Navigation 3 that wraps NavDisplay.
 * Uses MutableList<NavKey> backStack instead of NavHostController.
 * Initializes header data ONCE and recomposes only on state changes.
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

    // Track current route to determine header visibility and behavior
    var currentRoute by remember { mutableStateOf<Any?>(null) }
    var isWorkspaceSelection by remember { mutableStateOf(false) }
    var shouldShowHeader by remember { mutableStateOf(false) }

    // Observe back stack changes to update header visibility
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

    // Global back navigation handler for Nav3
    BackNavigationHandlerNav3(
        backStack = backStack,
        enabled = shouldShowHeader,
        fallbackRoute = if (isWorkspaceSelection) Route.Login else Route.Workspace
    )

    // Only show layout with header for non-auth screens
    if (shouldShowHeader) {
        val currentWorkspaceName = if (isWorkspaceSelection) null else headerState.currentWorkspace?.name
        val currentWorkspaceId = if (isWorkspaceSelection) null else headerState.currentWorkspace?.id
        val workspaceAvatarUrl = if (isWorkspaceSelection) null else headerState.currentWorkspace?.avatarUrl
        val userFullName = "${headerState.currentUser?.firstName ?: ""} ${headerState.currentUser?.lastName ?: ""}".trim()
            .ifEmpty { "User" }
        val profilePictureThumbnailUrl = headerState.currentUser?.profilePictureThumbnailUrl

        val onWorkspaceClick: () -> Unit = {
            if (!isWorkspaceSelection) viewModel.clearWorkspace()
        }

        val onEditProfile: () -> Unit = {
            backStack.add(AuthRoute.UserUpdate)
        }

        val onLogout: () -> Unit = { viewModel.logout() }

        val onSwitchUser: () -> Unit = { viewModel.switchUser() }

        val onDeleteAccount: () -> Unit = {
            backStack.add(AuthRoute.AccountDeletion)
        }

        // Observe global navigation state
        val globalNavManager = GlobalNavigationManager.getInstance()
        val navigationService by globalNavManager.navigationService.collectAsState()
        val isNavigationAvailable by globalNavManager.isNavigationAvailable.collectAsState()
        val navigationPattern = PlatformNavigationDetector.getNavigationPattern()

        // Only show navigation drawer when platform supports it and navigation is available
        val hasActiveWorkspace = !currentWorkspaceId.isNullOrBlank()
        val shouldShowDrawer = navigationPattern == NavigationPattern.SIDE_DRAWER &&
                isNavigationAvailable &&
                navigationService != null &&
                hasActiveWorkspace

        if (shouldShowDrawer) {
            // Mobile: Use navigation drawer with MobileModuleSideNavigation
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            val showAgentFab = hasActiveWorkspace && currentRoute !is Route.Agent

            MobileNavigationDrawer(
                navigationService = navigationService!!,
                onNavigate = { route ->
                    navigateToMenuItemNav3(backStack, route)
                },
                onSwitchWorkspace = onWorkspaceClick,
                onManageMembers = {
                    backStack.add(WorkspaceRoute.Members(currentWorkspaceId!!))
                },
                onManageInvitations = {
                    backStack.add(WorkspaceRoute.Invitations(currentWorkspaceId!!))
                },
                onSettings = {},
                drawerState = drawerState
            ) {
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
                            onDeleteAccount = onDeleteAccount,
                            onNavigationDrawerClick = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    },
                    floatingActionButton = {
                        if (showAgentFab) {
                            FloatingActionButton(
                                onClick = { backStack.add(Route.Agent) },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat Assistant")
                            }
                        }
                    }
                ) { paddingValues ->
                    content(paddingValues)
                }
            }
        } else {
            // Desktop/Non-drawer platforms: Use regular scaffold
            val showAgentFab = hasActiveWorkspace && currentRoute !is Route.Agent

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
                floatingActionButton = {
                    if (showAgentFab) {
                        FloatingActionButton(
                            onClick = { backStack.add(Route.Agent) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat Assistant")
                        }
                    }
                }
            ) { paddingValues ->
                content(paddingValues)
            }
        }
    } else {
        // Auth screens - no header, just render content directly
        content(PaddingValues())
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
