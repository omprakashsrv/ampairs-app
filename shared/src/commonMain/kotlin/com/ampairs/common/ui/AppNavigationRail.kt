package com.ampairs.common.ui

import Route
import WorkspaceRoute
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavKey
import ampairsapp.shared.generated.resources.Res
import ampairsapp.shared.generated.resources.nav_home
import ampairsapp.shared.generated.resources.nav_more
import com.ampairs.workspace.navigation.GlobalNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppNavigationRail(
    backStack: MutableList<NavKey>,
    currentRoute: NavKey?
) {
    val globalNavManager = remember { GlobalNavigationManager.getInstance() }
    @OptIn(ExperimentalCoroutinesApi::class)
    val navigationRoutes by remember {
        globalNavManager.navigationService.flatMapLatest { service ->
            service?.allActiveRoutes ?: flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

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

        // All installed modules
        navigationRoutes.forEach { module ->
            NavigationRailItem(
                selected = activeModuleCode == module.moduleCode,
                onClick = { navigateToModule(backStack, module.moduleCode) },
                icon = { Icon(moduleCodeToIcon(module.moduleCode), contentDescription = null) },
                label = { Text(moduleCodeToDisplayName(module.moduleCode), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }

        // Push More to bottom
        Spacer(Modifier.weight(1f))

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
