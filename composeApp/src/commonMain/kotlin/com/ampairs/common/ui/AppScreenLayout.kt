package com.ampairs.common.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.ampairs.workspace.navigation.MobileNavigationDrawer
import com.ampairs.workspace.navigation.NavigationPattern
import com.ampairs.workspace.navigation.PlatformNavigationDetector
import com.ampairs.workspace.navigation.GlobalNavigationManager
import navigateToMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreenLayout(
    navController: NavHostController,
    currentWorkspaceName: String?,
    currentWorkspaceId: String?,
    userFullName: String,
    isUserLoading: Boolean = false,
    isWorkspaceLoading: Boolean = false,
    onWorkspaceClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    // Observe global navigation state
    val globalNavManager = GlobalNavigationManager.getInstance()
    val navigationService by globalNavManager.navigationService.collectAsState()
    val isNavigationAvailable by globalNavManager.isNavigationAvailable.collectAsState()
    val navigationPattern = PlatformNavigationDetector.getNavigationPattern()

    // Only show navigation drawer when:
    // 1. Platform supports side drawer (mobile)
    // 2. Navigation service is available and initialized
    // 3. A workspace is actually selected (not on workspace selection screen)
    val hasActiveWorkspace = !currentWorkspaceId.isNullOrBlank()
    val shouldShowDrawer = navigationPattern == NavigationPattern.SIDE_DRAWER &&
            isNavigationAvailable &&
            navigationService != null &&
            hasActiveWorkspace
    if (shouldShowDrawer) {
        // Mobile: Use navigation drawer with MobileModuleSideNavigation
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        MobileNavigationDrawer(
            navigationService = navigationService!!,
            onNavigate = { route ->
                // Use the navigateToMenuItem function like desktop implementation
                navigateToMenuItem(navController, route)
            },
            onSwitchWorkspace = onWorkspaceClick,
            onManageMembers = {
                // Safe to use !! since shouldShowDrawer ensures currentWorkspaceId is not null
                navController.navigate("workspace/${currentWorkspaceId!!}/members")
            },
            onManageInvitations = {
                navController.navigate("workspace/${currentWorkspaceId!!}/invitations")
            },
            onSettings = {
                // Navigate to settings screen if available
            },
            drawerState = drawerState
        ) {
            Scaffold(
                modifier = modifier.imePadding(),
                topBar = {
                    AppHeader(
                        navController = navController,
                        currentWorkspaceName = currentWorkspaceName,
                        currentWorkspaceId = currentWorkspaceId,
                        userFullName = userFullName,
                        isUserLoading = isUserLoading,
                        isWorkspaceLoading = isWorkspaceLoading,
                        onWorkspaceClick = onWorkspaceClick,
                        onEditProfile = onEditProfile,
                        onLogout = onLogout,
                        onSwitchUser = onSwitchUser,
                        onDeleteAccount = onDeleteAccount,
                        onNavigationDrawerClick = {
                            // Open the drawer when hamburger menu is clicked
                            scope.launch {
                                drawerState.open()
                            }
                        },
                    )
                },
            ) { paddingValues ->
                content(paddingValues)
            }
        }
    } else {
        // Desktop/Non-drawer platforms: Use regular scaffold
        Scaffold(
            modifier = modifier.imePadding(),
            topBar = {
                AppHeader(
                    navController = navController,
                    currentWorkspaceName = currentWorkspaceName,
                    currentWorkspaceId = currentWorkspaceId,
                    userFullName = userFullName,
                    isUserLoading = isUserLoading,
                    isWorkspaceLoading = isWorkspaceLoading,
                    onWorkspaceClick = onWorkspaceClick,
                    onEditProfile = onEditProfile,
                    onLogout = onLogout,
                    onSwitchUser = onSwitchUser,
                    onDeleteAccount = onDeleteAccount,
                )
            },
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}