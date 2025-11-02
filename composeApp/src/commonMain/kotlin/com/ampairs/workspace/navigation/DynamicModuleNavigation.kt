package com.ampairs.workspace.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Unified dynamic module navigation component
 * Automatically chooses the appropriate navigation pattern based on platform:
 * - Desktop: Native MenuBar integration
 * - Mobile (Android/iOS): Side navigation drawer style
 */
@Composable
fun DynamicModuleNavigation(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit,
    onSwitchWorkspace: () -> Unit = {},
    onManageMembers: () -> Unit = {},
    onManageInvitations: () -> Unit = {},
    onSettings: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onExit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (PlatformNavigationDetector.getNavigationPattern()) {
        NavigationPattern.MENU_BAR -> {
            DesktopModuleNavigation(
                navigationService = navigationService,
                onNavigate = onNavigate,
                onSwitchWorkspace = onSwitchWorkspace,
                onManageMembers = onManageMembers,
                onManageInvitations = onManageInvitations,
                onSettings = onSettings,
                onThemeChange = onThemeChange,
                onRefresh = onRefresh,
                onExit = onExit
            )
        }
        NavigationPattern.SIDE_DRAWER -> {
            MobileModuleSideNavigation(
                navigationService = navigationService,
                onNavigate = onNavigate,
                modifier = modifier
            )
        }
    }
}