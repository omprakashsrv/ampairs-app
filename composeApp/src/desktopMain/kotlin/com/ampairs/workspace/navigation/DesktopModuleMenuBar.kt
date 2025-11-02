package com.ampairs.workspace.navigation

import androidx.compose.runtime.*

/**
 * Desktop implementation - No-op since MenuBar is handled in main.kt Window scope
 * Use WorkspaceModuleMenuBar extension function in main.kt MenuBar scope instead
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
    // No-op for desktop - MenuBar is handled in main.kt Window scope
    // Use MenuBarScope.WorkspaceModuleMenuBar() in main.kt instead
}