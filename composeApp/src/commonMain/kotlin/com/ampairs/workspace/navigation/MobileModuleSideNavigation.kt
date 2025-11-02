package com.ampairs.workspace.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mobile-style side navigation for module navigation
 * Displays modules as expandable groups with menu items
 */
@Composable
fun MobileModuleSideNavigation(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navigationRoutes by navigationService.navigationRoutes.collectAsState()
    val isLoading by navigationService.isLoading.collectAsState()
    val error by navigationService.error.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Error state
        error?.let { errorMessage ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Show content based on actual data state, not loading state
        if (navigationRoutes.isEmpty()) {
            if (isLoading) {
                // Loading state
                item {
                    repeat(3) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            // Skeleton loading
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Empty state (only when not loading)
                item {
                    EmptyModulesCard()
                }
            }
        } else {
            // Create grouped navigation with module names as groups
            navigationRoutes.forEach { moduleRoute ->
                // Module group header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(parseHexColor(moduleRoute.primaryColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = moduleRoute.displayName.first().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = moduleRoute.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Module menu items
                items(moduleRoute.menuItems) { menuItem ->
                    NavigationDrawerItem(
                        icon = {
                            Spacer(modifier = Modifier.width(32.dp)) // Indent for group hierarchy
                        },
                        label = {
                            Text(
                                text = menuItem.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        selected = false, // TODO: Add selection state management
                        onClick = {
                            onNavigate(menuItem.routePath)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp)
                    )
                }

                // Add spacing between module groups
                if (moduleRoute != navigationRoutes.last()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Statistics
//        if (navigationRoutes.isNotEmpty()) {
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//                ModuleStatistics(
//                    moduleCount = navigationRoutes.size,
//                    totalMenuItems = navigationRoutes.sumOf { it.menuItems.size }
//                )
//            }
//        }
    }
}


@Composable
private fun EmptyModulesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Modules Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Install modules from the module store to see navigation options.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModuleStatistics(
    moduleCount: Int,
    totalMenuItems: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = moduleCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Modules",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalMenuItems.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Menu Items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Parse hex color string to Compose Color (cross-platform)
 */
private fun parseHexColor(hexColor: String): Color {
    val cleanHex = hexColor.removePrefix("#")
    return try {
        val colorInt = cleanHex.toLong(16)
        Color(
            red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f,
            alpha = 1f
        )
    } catch (_: Exception) {
        Color.Gray // Fallback color
    }
}

