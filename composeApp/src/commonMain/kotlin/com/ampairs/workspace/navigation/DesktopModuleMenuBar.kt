package com.ampairs.workspace.navigation

import androidx.compose.runtime.*

/**
 * Cross-platform desktop module navigation interface
 * For desktop platforms, delegates to platform-specific MenuBar implementation
 * This file provides the common interface that mobile platforms can ignore
 */
@Composable
expect fun DesktopModuleNavigation(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit,
    onSwitchWorkspace: () -> Unit = {},
    onManageMembers: () -> Unit = {},
    onManageInvitations: () -> Unit = {},
    onSettings: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onExit: () -> Unit = {}
)