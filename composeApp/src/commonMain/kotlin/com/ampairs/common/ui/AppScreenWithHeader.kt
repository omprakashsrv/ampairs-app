package com.ampairs.common.ui

import AuthRoute
import Route
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.navigation.BackNavigationHandler
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.domain.WorkspaceList
import com.ampairs.workspace.integration.WorkspaceContextIntegration
import kotlinx.coroutines.flow.firstOrNull
import org.koin.compose.koinInject

@Composable
fun AppScreenWithHeader(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isWorkspaceSelection: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val userRepository: UserRepository = koinInject()
    val workspaceRepository: WorkspaceRepository = koinInject()
    val userWorkspaceRepository: UserWorkspaceRepository = koinInject()
    val tokenRepository: TokenRepository = koinInject()
    val analytics: FirebaseAnalytics = koinInject()
    val headerStateManager = remember { AppHeaderStateManager.instance }
    val headerState by headerStateManager.headerState.collectAsState()

    // Initialize header data on first composition and when workspace changes
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
                    lastLogin = 0L, // Not available in current UserEntity
                    loginCount = 0, // Not available in current UserEntity
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
                // Load the selected workspace by ID
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
            // Handle error gracefully
            println("Error loading header data: ${e.message}")
        }
    }

    // Global back navigation handler
    BackNavigationHandler(
        navController = navController,
        enabled = true,
        fallbackRoute = if (isWorkspaceSelection) Route.Login else Route.Workspace
    )

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
                // Clear workspace context and modules before switching
                WorkspaceContextIntegration.clearWorkspaceContext()
                navController.navigate(Route.Workspace) {
                    // Clear back stack up to workspace selection screen
                    // This removes any deep navigation within the current workspace
                    popUpTo(Route.Workspace) {
                        inclusive = true  // Include the workspace route itself
                    }
                    launchSingleTop = true
                }
            }
        },
        onEditProfile = {
            navController.navigate(AuthRoute.UserUpdate)
        },
        onLogout = {
            // Log analytics event
            analytics.logEvent(AnalyticsEvents.LOGOUT)

            // Clear Firebase Analytics user ID
            analytics.setUserId(null)

            // Clear workspace context and modules before logout
            WorkspaceContextIntegration.clearWorkspaceContext()
            // Clear user data and navigate to login
            headerStateManager.reset()
            navController.navigate(Route.Login) {
                popUpTo(0)
            }
        },
        onSwitchUser = {
            // Clear Firebase Analytics user ID before switching
            analytics.setUserId(null)

            // Use a coroutine to clear current user before navigation
            kotlinx.coroutines.runBlocking {
                // Clear the current user so they stay on user selection screen
                tokenRepository.clearCurrentUser()
            }
            // Clear workspace context and modules before switching users
            WorkspaceContextIntegration.clearWorkspaceContext()
            // Clear header state before navigation to prevent scope issues
            headerStateManager.reset()
            navController.navigate(AuthRoute.UserSelection) {
                // Clear the ENTIRE back stack for user switching - complete reset
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        },
        modifier = modifier,
        content = content
    )
}