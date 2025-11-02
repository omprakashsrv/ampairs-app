package com.ampairs.workspace.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Mobile navigation drawer wrapper for workspace module navigation
 * Uses Material 3 ModalNavigationDrawer for mobile platforms
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileNavigationDrawer(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit,
    onSwitchWorkspace: () -> Unit = {},
    onManageMembers: () -> Unit = {},
    onManageInvitations: () -> Unit = {},
    onSettings: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
//                    Text(
//                        text = "Navigation",
//                        style = MaterialTheme.typography.titleLarge,
//                        modifier = Modifier.padding(bottom = 16.dp)
//                    )
//
//                    HorizontalDivider()

//                    Spacer(modifier = Modifier.height(16.dp))

                    // Module navigation using existing MobileModuleSideNavigation
                    MobileModuleSideNavigation(
                        navigationService = navigationService,
                        onNavigate = { route ->
                            onNavigate(route)
                            // Close drawer after navigation using proper coroutine scope
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Workspace actions
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.SwapHoriz, null) },
                        label = { Text("Switch Workspace") },
                        selected = false,
                        onClick = {
                            onSwitchWorkspace()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Group, null) },
                        label = { Text("Team Members") },
                        selected = false,
                        onClick = {
                            onManageMembers()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Mail, null) },
                        label = { Text("Invitations") },
                        selected = false,
                        onClick = {
                            onManageInvitations()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            onSettings()
                        }
                    )
                }
            }
        },
        content = content
    )
}