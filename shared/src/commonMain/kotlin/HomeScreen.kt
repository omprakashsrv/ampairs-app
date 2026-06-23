import ampairsapp.shared.generated.resources.Res
import ampairsapp.shared.generated.resources.home_action_add_customer
import ampairsapp.shared.generated.resources.home_action_add_product
import ampairsapp.shared.generated.resources.home_action_new_invoice
import ampairsapp.shared.generated.resources.home_greeting
import ampairsapp.shared.generated.resources.home_placeholder_value
import ampairsapp.shared.generated.resources.home_recent_activity_empty
import ampairsapp.shared.generated.resources.home_section_overview
import ampairsapp.shared.generated.resources.home_section_quick_actions
import ampairsapp.shared.generated.resources.home_section_recent_activity
import ampairsapp.shared.generated.resources.home_stat_customers
import ampairsapp.shared.generated.resources.home_stat_invoices
import ampairsapp.shared.generated.resources.home_stat_today_sales
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.notification.ui.NotificationBadgeViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import Route
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.common.ui.navigateToModule
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.navigation.ModuleCodes
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    workspaceId: String,
    backStack: MutableList<NavKey>? = null,
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)? = null,
    viewModel: WorkspaceModulesViewModel = assistedMetroViewModel<WorkspaceModulesViewModel, WorkspaceModulesViewModel.Factory>(
        key = workspaceId
    ) { create(workspaceId) },
    badgeViewModel: NotificationBadgeViewModel = metroViewModel()
) {
    val globalNavManager = remember { GlobalNavigationManager.getInstance() }
    val navigationService by globalNavManager.navigationService.collectAsState()
    val headerStateManager = remember { AppHeaderStateManager.instance }
    val headerState by headerStateManager.headerState.collectAsState()
    val unreadCount by badgeViewModel.unreadCount.collectAsStateWithLifecycle()

    val userName = headerState.currentUser?.firstName ?: ""
    val workspaceName = headerState.currentWorkspace?.name ?: ""

    LaunchedEffect(workspaceId) {
        if (workspaceId.isNotEmpty()) viewModel.loadInstalledModules()
    }

    LaunchedEffect(navigationService) {
        onNavigationServiceReady?.invoke(navigationService)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Greeting header
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userName.isNotEmpty())
                                "${stringResource(Res.string.home_greeting)}, $userName"
                            else
                                stringResource(Res.string.home_greeting),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (workspaceName.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = workspaceName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                    if (backStack != null) {
                        IconButton(onClick = { backStack.add(Route.Notifications) }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge {
                                            Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = stringResource(Res.string.home_cd_notifications),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overview section label
        item {
            Text(
                text = stringResource(Res.string.home_section_overview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        // KPI cards row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    label = stringResource(Res.string.home_stat_today_sales),
                    value = stringResource(Res.string.home_placeholder_value),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = stringResource(Res.string.home_stat_invoices),
                    value = stringResource(Res.string.home_placeholder_value),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = stringResource(Res.string.home_stat_customers),
                    value = stringResource(Res.string.home_placeholder_value),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick actions section
        item {
            Text(
                text = stringResource(Res.string.home_section_quick_actions),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (backStack != null) {
                    QuickActionChip(
                        label = stringResource(Res.string.home_action_new_invoice),
                        icon = Icons.Default.Receipt,
                        onClick = { navigateToModule(backStack, ModuleCodes.INVOICE_BILLING) }
                    )
                    QuickActionChip(
                        label = stringResource(Res.string.home_action_add_customer),
                        icon = Icons.Default.Group,
                        onClick = { navigateToModule(backStack, ModuleCodes.CUSTOMER_MANAGEMENT) }
                    )
                    QuickActionChip(
                        label = stringResource(Res.string.home_action_add_product),
                        icon = Icons.Default.Inventory,
                        onClick = { navigateToModule(backStack, ModuleCodes.PRODUCT_MANAGEMENT) }
                    )
                }
            }
        }

        // Recent activity section
        item {
            Text(
                text = stringResource(Res.string.home_section_recent_activity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.home_recent_activity_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        icon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = null
    )
}
