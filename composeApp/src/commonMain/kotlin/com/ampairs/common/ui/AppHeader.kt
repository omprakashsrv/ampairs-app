package com.ampairs.common.ui

import WorkspaceRoute
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ampairs.common.theme.ThemeManager
import com.ampairs.common.theme.ThemePreference
import com.ampairs.workspace.navigation.PlatformNavigationDetector
import com.ampairs.workspace.navigation.NavigationPattern
import com.ampairs.workspace.navigation.GlobalNavigationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    navController: NavController,
    currentWorkspaceName: String?,
    currentWorkspaceId: String?,
    userFullName: String,
    isUserLoading: Boolean = false,
    isWorkspaceLoading: Boolean = false,
    onWorkspaceClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    onNavigationDrawerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation button logic - platform-aware
            val globalNavManager = GlobalNavigationManager.getInstance()
            val shouldShowHamburger by globalNavManager.shouldShowHamburgerMenu.collectAsState()
            val canNavigateBack = navController.previousBackStackEntry != null
            val platformRequiresBackButton = PlatformNavigationDetector.requiresBackButton()

            // iOS Navigation Pattern:
            // - Show hamburger at root level (no back stack)
            // - Show back button when in navigation hierarchy
            // - Prioritize back button over hamburger when both could be shown
            //
            // Android Navigation Pattern:
            // - Show hamburger (uses hardware back for navigation)
            // - Hide back button when hamburger is shown
            val showHamburgerButton = shouldShowHamburger && onNavigationDrawerClick != null && (
                if (platformRequiresBackButton) !canNavigateBack  // iOS: hide hamburger when back is available
                else true  // Android: always show hamburger
            )

            val showBackButton = canNavigateBack && (
                platformRequiresBackButton || (!shouldShowHamburger || onNavigationDrawerClick == null)
            )

            // Hamburger menu button
            if (showHamburgerButton) {
                IconButton(
                    onClick = onNavigationDrawerClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Open navigation menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Back button

            if (showBackButton) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Center - Workspace selector with management menu
            WorkspaceSelector(
                navController = navController,
                workspaceName = currentWorkspaceName,
                workspaceId = currentWorkspaceId,
                isLoading = isWorkspaceLoading,
                onWorkspaceClick = onWorkspaceClick,
                modifier = Modifier.widthIn(min = 120.dp, max = 200.dp)
            )

            // Spacer to push right-side elements to the right
            Spacer(modifier = Modifier.weight(1f))

            // Right side - Theme toggle and user profile menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme toggle button
                ThemeToggleButton()

                // User profile menu
                UserProfileMenu(
                    userFullName = userFullName,
                    isLoading = isUserLoading,
                    onEditProfile = onEditProfile,
                    onLogout = onLogout,
                    onSwitchUser = onSwitchUser
                )
            }
        }
    }
}

@Composable
private fun WorkspaceSelector(
    navController: NavController,
    workspaceName: String?,
    workspaceId: String?,
    isLoading: Boolean,
    onWorkspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = "Workspace",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Workspace",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (isLoading) {
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = workspaceName ?: "Select Workspace",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (workspaceName != null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }

                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Workspace menu",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Workspace Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
        ) {
            // Switch Workspace
            WorkspaceMenuItem(
                icon = Icons.Default.SwapHoriz,
                text = "Switch Workspace",
                onClick = {
                    expanded = false
                    onWorkspaceClick()
                }
            )

            // Only show management options when a workspace is selected
            if (workspaceName != null && workspaceId != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                WorkspaceMenuItem(
                    icon = Icons.Default.Group,
                    text = "Team Members",
                    onClick = {
                        expanded = false
                        navController.navigate(WorkspaceRoute.Members(workspaceId = workspaceId))
                    }
                )

                WorkspaceMenuItem(
                    icon = Icons.Default.Apps,
                    text = "Manage Modules",
                    onClick = {
                        expanded = false
                        navController.navigate(WorkspaceRoute.ModuleStore(workspaceId = workspaceId))
                    }
                )

                WorkspaceMenuItem(
                    icon = Icons.Default.Mail,
                    text = "Invitations",
                    onClick = {
                        expanded = false
                        navController.navigate(WorkspaceRoute.Invitations(workspaceId = workspaceId))
                    }
                )
            }
        }
    }
}

@Composable
private fun UserProfileMenu(
    userFullName: String,
    isLoading: Boolean,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // User Avatar
            UserAvatar(
                userFullName = userFullName,
                isLoading = isLoading,
                size = 36.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // User Name
            Column {
                Text(
                    text = if (isLoading) "Loading..." else userFullName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Menu Button
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Profile menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Edit,
                text = "Edit Profile",
                onClick = {
                    expanded = false
                    onEditProfile()
                }
            )

            ProfileMenuItem(
                icon = Icons.Default.SwapHoriz,
                text = "Switch User",
                onClick = {
                    expanded = false
                    onSwitchUser()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                text = "Logout",
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}

@Composable
private fun UserAvatar(
    userFullName: String,
    isLoading: Boolean,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.6f),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            val initials = userFullName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")

            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (textColor == MaterialTheme.colorScheme.error) {
                        textColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun WorkspaceMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun ThemeToggleButton(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val themeManager: ThemeManager = org.koin.compose.koinInject()
    val currentTheme by themeManager.themePreference.collectAsState()

    Box(modifier = modifier) {
        // Theme toggle button
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                when (currentTheme) {
                    ThemePreference.LIGHT -> Icons.Default.LightMode
                    ThemePreference.DARK -> Icons.Default.DarkMode
                    ThemePreference.SYSTEM -> Icons.Default.Settings
                },
                contentDescription = "Theme: ${currentTheme.displayName}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        // Theme selection dropdown
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
        ) {
            ThemePreference.entries.forEach { theme ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (theme) {
                                    ThemePreference.LIGHT -> Icons.Default.LightMode
                                    ThemePreference.DARK -> Icons.Default.DarkMode
                                    ThemePreference.SYSTEM -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (theme == currentTheme) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = theme.displayName,
                                color = if (theme == currentTheme) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (theme == currentTheme) {
                                    FontWeight.Medium
                                } else {
                                    FontWeight.Normal
                                }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (theme == currentTheme) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        themeManager.setThemePreference(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}