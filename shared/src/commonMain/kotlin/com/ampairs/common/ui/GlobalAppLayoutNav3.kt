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
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.di.LocalAppGraph
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.domain.WorkspaceList
import com.ampairs.workspace.integration.WorkspaceContextIntegration
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.navigation.MobileNavigationDrawer
import com.ampairs.workspace.navigation.NavigationPattern
import com.ampairs.workspace.navigation.PlatformNavigationDetector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
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
    val graph = LocalAppGraph.current
    val userRepository = graph.userRepository
    val workspaceRepository = graph.workspaceRepository
    val userWorkspaceRepository = graph.userWorkspaceRepository
    val tokenRepository = graph.tokenRepository
    val analytics = graph.analytics
    val appPreferences = graph.appPreferences
    val headerStateManager = remember { AppHeaderStateManager.instance }
    val headerState by headerStateManager.headerState.collectAsState()

    // Track current route to determine header visibility and behavior
    var currentRoute by remember { mutableStateOf<Any?>(null) }
    var isWorkspaceSelection by remember { mutableStateOf(false) }
    var shouldShowHeader by remember { mutableStateOf(false) }

    // Observe back stack changes to update header visibility
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collectLatest { route ->
                currentRoute = route

                // Determine if we're in workspace selection mode
                isWorkspaceSelection = route is WorkspaceRoute.Root ||
                        route is WorkspaceRoute.Create ||
                        route is WorkspaceRoute.Edit

                // Show header for all screens except auth screens
                shouldShowHeader = route != null &&
                        route !is AuthRoute &&
                        route !is Route.Login
            }
    }

    // Initialize header data ONCE on first composition
    LaunchedEffect(Unit) {
        try {
            // Load current user
            userRepository.getUser()?.let { userEntity ->
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
                headerStateManager.updateUser(userInfo)

                // Set Firebase Analytics user ID
                analytics.setUserId(userEntity.id)
            }

            // Load currently selected workspace
            val selectedWorkspaceId = userWorkspaceRepository.getWorkspaceIdForUser(
                userId = headerState.currentUser?.id ?: ""
            )
            if (selectedWorkspaceId.isNotEmpty()) {
                workspaceRepository.getWorkspaceById(selectedWorkspaceId)?.let { workspace ->
                    val workspaceList = WorkspaceList(
                        id = workspace.id,
                        name = workspace.name,
                        slug = workspace.slug,
                        description = workspace.description,
                        workspaceType = workspace.workspaceType,
                        avatarUrl = workspace.avatarUrl,
                        subscriptionPlan = workspace.subscriptionPlan,
                        memberCount = workspace.memberCount ?: 1,
                        lastActivityAt = workspace.lastActivityAt,
                        createdAt = workspace.createdAt
                    )
                    headerStateManager.updateWorkspace(workspaceList)
                }
            } else {
                // Fallback: load first workspace if no workspace is selected
                workspaceRepository.getLocalWorkspaces().firstOrNull()?.let { workspaces ->
                    workspaces.firstOrNull()?.let { workspace ->
                        val workspaceList = WorkspaceList(
                            id = workspace.id,
                            name = workspace.name,
                            slug = workspace.slug,
                            description = workspace.description,
                            workspaceType = workspace.workspaceType,
                            avatarUrl = workspace.avatarUrl,
                            subscriptionPlan = workspace.subscriptionPlan,
                            memberCount = workspace.memberCount ?: 1,
                            lastActivityAt = workspace.lastActivityAt,
                            createdAt = workspace.createdAt
                        )
                        headerStateManager.updateWorkspace(workspaceList)
                    }
                }
            }
        } catch (e: Exception) {
            println("GlobalAppLayoutNav3: Error loading header data: ${e.message}")
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
            if (!isWorkspaceSelection) {
                WorkspaceContextIntegration.clearWorkspaceContext()
                kotlinx.coroutines.runBlocking {
                    appPreferences.clearLastWorkspaceId()
                }
                backStack.clear()
                backStack.add(Route.Workspace)
            }
        }

        val onEditProfile: () -> Unit = {
            backStack.add(AuthRoute.UserUpdate)
        }

        val onLogout: () -> Unit = {
            analytics.logEvent(AnalyticsEvents.LOGOUT)
            analytics.setUserId(null)
            WorkspaceContextIntegration.clearWorkspaceContext()
            kotlinx.coroutines.runBlocking {
                val currentUserId = tokenRepository.getCurrentUserId()

                currentUserId?.let { userId ->
                    try {
                        tokenRepository.logoutUser(userId)
                        userRepository.deleteUserById(userId)
                        println("Successfully logged out and deleted user: $userId")
                    } catch (e: Exception) {
                        println("Failed to logout user: ${e.message}")
                    }
                }

                appPreferences.clearLastWorkspaceId()
                appPreferences.clearLastUserId()
            }
            headerStateManager.reset()
            backStack.clear()
            backStack.add(Route.Login)
        }

        val onSwitchUser: () -> Unit = {
            analytics.setUserId(null)
            kotlinx.coroutines.runBlocking {
                tokenRepository.clearCurrentUser()
                appPreferences.clearLastUserId()
                appPreferences.clearLastWorkspaceId()
            }
            WorkspaceContextIntegration.clearWorkspaceContext()
            headerStateManager.reset()
            backStack.clear()
            backStack.add(AuthRoute.UserSelection)
        }

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
