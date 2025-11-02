package com.ampairs.workspace.navigation

import androidx.compose.runtime.*

/**
 * iOS implementation - no-op since mobile uses navigation drawer instead
 */
@Composable
actual fun DesktopModuleNavigation(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit,
    onSwitchWorkspace: () -> Unit,
    onManageMembers: () -> Unit,
    onManageInvitations: () -> Unit,
    onSettings: () -> Unit,
    onThemeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onExit: () -> Unit
) {
    // No-op for iOS - mobile uses navigation drawer instead of desktop menu bar
}