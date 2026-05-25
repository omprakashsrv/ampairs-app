package com.ampairs.common.ui

import Route
import WorkspaceRoute
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.theme.ThemePreference
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.navigation.PlatformNavigationDetector
import com.ampairs.workspace.ui.LanguageSettingsDialog
import ampairsapp.shared.generated.resources.Res
import ampairsapp.shared.generated.resources.edit
import ampairsapp.shared.generated.resources.settings_language
import ampairsapp.shared.generated.resources.settings_logout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ampairs.common.localization.LocalLocaleManager
import com.ampairs.common.theme.LocalThemeManager
import org.jetbrains.compose.resources.stringResource

/**
 * App header for Navigation 3.
 * Similar to AppHeader but uses back stack instead of NavController for navigation state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeaderNav3(
    backStack: MutableList<NavKey>,
    currentWorkspaceName: String?,
    currentWorkspaceId: String?,
    workspaceAvatarUrl: String?,
    userFullName: String,
    profilePictureThumbnailUrl: String?,
    isUserLoading: Boolean,
    isWorkspaceLoading: Boolean,
    onWorkspaceClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNavigationDrawerClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
            val globalNavManager = remember { GlobalNavigationManager.getInstance() }
            val shouldShowHamburger by globalNavManager.shouldShowHamburgerMenu.collectAsState()
            val canNavigateBack = backStack.size > 1
            val platformRequiresBackButton = remember { PlatformNavigationDetector.requiresBackButton() }

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
                    onClick = { onNavigationDrawerClick?.invoke() },
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
                    onClick = { backStack.removeLastOrNull() },
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
            WorkspaceSelectorNav3(
                backStack = backStack,
                workspaceName = currentWorkspaceName,
                workspaceId = currentWorkspaceId,
                workspaceAvatarUrl = workspaceAvatarUrl,
                isLoading = isWorkspaceLoading,
                onWorkspaceClick = onWorkspaceClick,
                modifier = Modifier.widthIn(min = 120.dp, max = 200.dp)
            )

            // Spacer to push right-side elements to the right
            Spacer(modifier = Modifier.weight(1f))

            // Right side - User profile menu
            UserProfileMenuNav3(
                userFullName = userFullName,
                isLoading = isUserLoading,
                profilePictureThumbnailUrl = profilePictureThumbnailUrl,
                onEditProfile = onEditProfile,
                onLogout = onLogout,
                onSwitchUser = onSwitchUser,
                onDeleteAccount = onDeleteAccount
            )
        }
    }
}

@Composable
private fun WorkspaceSelectorNav3(
    backStack: MutableList<NavKey>,
    workspaceName: String?,
    workspaceId: String?,
    workspaceAvatarUrl: String?,
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
                // Workspace avatar or default icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!workspaceAvatarUrl.isNullOrBlank() && workspaceId != null) {
                        AsyncImage(
                            model = ApiUrlBuilder.workspaceAvatarThumbnailUrl(workspaceId),
                            contentDescription = "Workspace avatar",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = "Workspace",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

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
            WorkspaceMenuItemNav3(
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

                WorkspaceMenuItemNav3(
                    icon = Icons.Default.Group,
                    text = "Team Members",
                    onClick = {
                        expanded = false
                        backStack.add(WorkspaceRoute.Members(workspaceId = workspaceId))
                    }
                )

                WorkspaceMenuItemNav3(
                    icon = Icons.Default.Apps,
                    text = "Manage Modules",
                    onClick = {
                        expanded = false
                        backStack.add(WorkspaceRoute.ModuleStore(workspaceId = workspaceId))
                    }
                )

                WorkspaceMenuItemNav3(
                    icon = Icons.Default.Mail,
                    text = "Invitations",
                    onClick = {
                        expanded = false
                        backStack.add(WorkspaceRoute.Invitations(workspaceId = workspaceId))
                    }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                WorkspaceMenuItemNav3(
                    icon = Icons.Default.CreditCard,
                    text = "Subscription",
                    onClick = {
                        expanded = false
                        backStack.add(Route.Subscription)
                    }
                )
            }
        }
    }
}

@Composable
private fun UserProfileMenuNav3(
    userFullName: String,
    isLoading: Boolean,
    profilePictureThumbnailUrl: String? = null,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // User Avatar
            UserAvatarNav3(
                userFullName = userFullName,
                isLoading = isLoading,
                size = 36.dp,
                profilePictureThumbnailUrl = profilePictureThumbnailUrl
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
            ProfileMenuItemNav3(
                icon = Icons.Default.Edit,
                text = stringResource(Res.string.edit),
                onClick = {
                    expanded = false
                    onEditProfile()
                }
            )

            ProfileMenuItemNav3(
                icon = Icons.Default.Language,
                text = stringResource(Res.string.settings_language),
                onClick = {
                    expanded = false
                    showLanguageDialog = true
                }
            )

            ProfileMenuItemNav3(
                icon = Icons.Default.Palette,
                text = "Theme",
                onClick = {
                    expanded = false
                    showThemeDialog = true
                }
            )

            ProfileMenuItemNav3(
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

            ProfileMenuItemNav3(
                icon = Icons.Default.Delete,
                text = "Delete Account",
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    expanded = false
                    onDeleteAccount()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            ProfileMenuItemNav3(
                icon = Icons.AutoMirrored.Filled.Logout,
                text = stringResource(Res.string.settings_logout),
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    expanded = false
                    showLogoutConfirmation = true
                }
            )
        }

        // Language Settings Dialog
        if (showLanguageDialog) {
            LanguageSettingsDialog(
                localeManager = LocalLocaleManager.current,
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Theme Settings Dialog
        if (showThemeDialog) {
            ThemeSettingsDialogNav3(
                onDismiss = { showThemeDialog = false }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutConfirmation) {
            LogoutConfirmationDialogNav3(
                onConfirm = {
                    showLogoutConfirmation = false
                    onLogout()
                },
                onDismiss = { showLogoutConfirmation = false }
            )
        }
    }
}

@Composable
private fun UserAvatarNav3(
    userFullName: String,
    isLoading: Boolean,
    size: Dp,
    profilePictureThumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.6f),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else if (!profilePictureThumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = profilePictureThumbnailUrl,
                contentDescription = "Profile picture",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
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
private fun ProfileMenuItemNav3(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
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
private fun WorkspaceMenuItemNav3(
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
private fun ThemeSettingsDialogNav3(
    onDismiss: () -> Unit
) {
    val themeManager = LocalThemeManager.current
    val currentTheme by themeManager.themePreference.collectAsState()
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Select Theme",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Theme Options
                ThemePreference.entries.forEach { theme ->
                    ThemeOptionNav3(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onSelect = {
                            scope.launch {
                                themeManager.setThemePreference(theme)
                                delay(100)
                                onDismiss()
                            }
                        }
                    )

                    if (theme != ThemePreference.entries.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionNav3(
    theme: ThemePreference,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (theme) {
                    ThemePreference.LIGHT -> Icons.Default.LightMode
                    ThemePreference.DARK -> Icons.Default.DarkMode
                    ThemePreference.SYSTEM -> Icons.Default.Settings
                },
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = when (theme) {
                        ThemePreference.LIGHT -> "Always use light theme"
                        ThemePreference.DARK -> "Always use dark theme"
                        ThemePreference.SYSTEM -> "Follow system settings"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LogoutConfirmationDialogNav3(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Logout",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to logout? You will need to sign in again to access your account.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
