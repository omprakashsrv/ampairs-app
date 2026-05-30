package com.ampairs.common.ui

import Route
import WorkspaceRoute
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavKey
import ampairsapp.shared.generated.resources.Res
import ampairsapp.shared.generated.resources.nav_home
import ampairsapp.shared.generated.resources.nav_more
import com.ampairs.workspace.navigation.DynamicModuleRoute
import com.ampairs.workspace.navigation.GlobalNavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppNavigationRail(
    backStack: MutableList<NavKey>,
    currentRoute: NavKey?
) {
    val globalNavManager = remember { GlobalNavigationManager.getInstance() }
    val navigationService by globalNavManager.navigationService.collectAsState()
    val navigationRoutes by remember(navigationService) {
        navigationService?.navigationRoutes ?: MutableStateFlow(emptyList<DynamicModuleRoute>())
    }.collectAsState()

    val activeModuleCode = resolveActiveModuleCode(currentRoute)

    NavigationRail {
        // Home — always first
        NavigationRailItem(
            selected = activeModuleCode == null,
            onClick = {
                val idx = backStack.indexOfLast { it is WorkspaceRoute.Modules }
                if (idx >= 0) while (backStack.size > idx + 1) backStack.removeLastOrNull()
            },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_home)) }
        )
        // All installed modules in scrollable section
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            navigationRoutes.forEach { module ->
                NavigationRailItem(
                    selected = activeModuleCode == module.moduleCode,
                    onClick = { navigateToModule(backStack, module.moduleCode) },
                    icon = { Icon(moduleCodeToIcon(module.moduleCode), contentDescription = null) },
                    label = { Text(module.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        // More — always last
        NavigationRailItem(
            selected = activeModuleCode == "more",
            onClick = {
                val idx = backStack.indexOfLast { it is WorkspaceRoute.Modules }
                if (idx >= 0) {
                    while (backStack.size > idx + 1) backStack.removeLastOrNull()
                    backStack.add(Route.More)
                }
            },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_more)) }
        )
    }
}
