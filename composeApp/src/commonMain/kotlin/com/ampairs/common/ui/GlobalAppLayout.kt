package com.ampairs.common.ui

import AuthRoute
import Route
import WorkspaceRoute
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.navigation.BackNavigationHandler
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.domain.WorkspaceList
import com.ampairs.workspace.integration.WorkspaceContextIntegration
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import org.koin.compose.koinInject

/**
 * Global App Layout that wraps the entire NavHost.
 * Initializes header data ONCE and recomposes only on state changes.
 * This is more efficient than wrapping each screen individually.
 */
@Composable
fun GlobalAppLayout(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val userRepository: UserRepository = koinInject()
    val workspaceRepository: WorkspaceRepository = koinInject()
    val userWorkspaceRepository: UserWorkspaceRepository = koinInject()
    val tokenRepository: TokenRepository = koinInject()
    val analytics: FirebaseAnalytics = koinInject()
    val appPreferences: AppPreferencesDataStore = koinInject()
    val headerStateManager = remember { AppHeaderStateManager.instance }
    val headerState by headerStateManager.headerState.collectAsState()

    // Track current route to determine header visibility and behavior
    var currentRoute by remember { mutableStateOf<String?>(null) }
    var isWorkspaceSelection by remember { mutableStateOf(false) }
    var shouldShowHeader by remember { mutableStateOf(false) }

    // Observe navigation changes to update header visibility
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { backStackEntry ->
            currentRoute = backStackEntry.destination.route

            // Determine if we're in workspace selection mode
            isWorkspaceSelection = currentRoute?.contains("WorkspaceRoute.Root") == true ||
                    currentRoute?.contains("WorkspaceRoute.Create") == true ||
                    currentRoute?.contains("WorkspaceRoute.Edit") == true

            // Show header for all screens except auth screens
            shouldShowHeader = currentRoute != null &&
                    !currentRoute!!.contains("AuthRoute") &&
                    !currentRoute!!.contains("Route.Login")
        }
    }

    // Initialize header data ONCE on first composition
    LaunchedEffect(Unit) {
        try {
            // Load current user
            userRepository.getUser()?.let { userEntity ->
                val userInfo = UserInfo(
                    id = userEntity.id,
                    firstName = userEntity.first_name,
                    lastName = userEntity.last_name,
                    userName = userEntity.user_name,
                    countryCode = userEntity.country_code,
                    phone = userEntity.phone,
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
            println("GlobalAppLayout: Error loading header data: ${e.message}")
        }
    }

    // Global back navigation handler
    BackNavigationHandler(
        navController = navController,
        enabled = shouldShowHeader,
        fallbackRoute = if (isWorkspaceSelection) Route.Login else Route.Workspace
    )

    // Only show layout with header for non-auth screens
    if (shouldShowHeader) {
        AppScreenLayout(
            navController = navController,
            currentWorkspaceName = if (isWorkspaceSelection) null else headerState.currentWorkspace?.name,
            currentWorkspaceId = if (isWorkspaceSelection) null else headerState.currentWorkspace?.id,
            userFullName = "${headerState.currentUser?.firstName ?: ""} ${headerState.currentUser?.lastName ?: ""}".trim()
                .ifEmpty { "User" },
            isUserLoading = headerState.isUserLoading,
            isWorkspaceLoading = headerState.isWorkspaceLoading,
            onWorkspaceClick = {
                if (!isWorkspaceSelection) {
                    WorkspaceContextIntegration.clearWorkspaceContext()
                    kotlinx.coroutines.runBlocking {
                        appPreferences.clearLastWorkspaceId()
                    }
                    navController.navigate(Route.Workspace) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            onEditProfile = {
                navController.navigate(AuthRoute.UserUpdate)
            },
            onLogout = {
                analytics.logEvent(AnalyticsEvents.LOGOUT)
                analytics.setUserId(null)
                WorkspaceContextIntegration.clearWorkspaceContext()
                kotlinx.coroutines.runBlocking {
                    // Get current user ID before clearing
                    val currentUserId = tokenRepository.getCurrentUserId()

                    currentUserId?.let { userId ->
                        try {
                            // 1. Logout user - clears tokens and user session from database
                            tokenRepository.logoutUser(userId)

                            // 2. Delete user entity from local database
                            userRepository.deleteUserById(userId)

                            println("✅ Successfully logged out and deleted user: $userId")
                        } catch (e: Exception) {
                            println("⚠️ Failed to logout user: ${e.message}")
                        }
                    }

                    // 3. Clear app preferences to prevent auto-resume
                    appPreferences.clearLastWorkspaceId()
                    appPreferences.clearLastUserId()
                }
                headerStateManager.reset()
                navController.navigate(Route.Login) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            onSwitchUser = {
                analytics.setUserId(null)
                kotlinx.coroutines.runBlocking {
                    tokenRepository.clearCurrentUser()
                    appPreferences.clearLastUserId()
                    appPreferences.clearLastWorkspaceId()
                }
                WorkspaceContextIntegration.clearWorkspaceContext()
                headerStateManager.reset()
                navController.navigate(AuthRoute.UserSelection) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onDeleteAccount = {
                navController.navigate(AuthRoute.AccountDeletion)
            },
            modifier = modifier,
            content = content
        )
    } else {
        // Auth screens - no header, just render content directly
        content(PaddingValues())
    }
}
