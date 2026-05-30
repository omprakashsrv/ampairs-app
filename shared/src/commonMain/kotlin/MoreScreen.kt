import Route
import SubscriptionRoute
import WorkspaceRoute
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.common.ui.navigateToModule
import com.ampairs.common.ui.moduleCodeToIcon
import com.ampairs.workspace.navigation.ModuleCodes
import ampairsapp.shared.generated.resources.Res
import ampairsapp.shared.generated.resources.nav_more_account
import ampairsapp.shared.generated.resources.nav_more_all_modules
import ampairsapp.shared.generated.resources.nav_more_business
import ampairsapp.shared.generated.resources.nav_more_edit_profile
import ampairsapp.shared.generated.resources.nav_more_screen_title
import ampairsapp.shared.generated.resources.nav_more_subscription
import ampairsapp.shared.generated.resources.nav_more_switch_workspace
import ampairsapp.shared.generated.resources.nav_more_tax_gst
import ampairsapp.shared.generated.resources.nav_more_workspace_settings
import org.jetbrains.compose.resources.stringResource

private data class ModuleItem(
    val label: String,
    val moduleCode: String? = null
)

@Composable
fun MoreScreen(
    backStack: MutableList<NavKey>,
    onSwitchWorkspace: () -> Unit,
    onEditProfile: () -> Unit
) {
    val headerStateManager = remember { AppHeaderStateManager.instance }
    val headerState by headerStateManager.headerState.collectAsState()
    val workspaceName = headerState.currentWorkspace?.name ?: ""
    val workspaceId = headerState.currentWorkspace?.id ?: ""
    val userName = "${headerState.currentUser?.firstName ?: ""} ${headerState.currentUser?.lastName ?: ""}".trim()
    val userPhone = headerState.currentUser?.phone ?: ""
    val userInitials = run {
        val first = headerState.currentUser?.firstName?.firstOrNull()?.uppercase() ?: ""
        val last = headerState.currentUser?.lastName?.firstOrNull()?.uppercase() ?: ""
        "$first$last".ifEmpty { "?" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.nav_more_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }

        // Profile card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onEditProfile() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = userInitials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifEmpty { "User" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (userPhone.isNotEmpty()) {
                            Text(
                                text = userPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Workspace card
        if (workspaceName.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onSwitchWorkspace() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = workspaceName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workspaceName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Switch workspace",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // All modules section
        item {
            Text(
                text = stringResource(Res.string.nav_more_all_modules),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
            )
        }

        item {
            val modules = listOf(
                ModuleItem("Home"),
                ModuleItem("Sales", ModuleCodes.INVOICE_BILLING),
                ModuleItem("Parties", ModuleCodes.CUSTOMER_MANAGEMENT),
                ModuleItem("Stock", ModuleCodes.PRODUCT_MANAGEMENT),
                ModuleItem(stringResource(Res.string.nav_more_tax_gst), ModuleCodes.TAX_CODE_MANAGEMENT),
                ModuleItem(stringResource(Res.string.nav_more_business), ModuleCodes.BUSINESS_PROFILE),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height((modules.size / 3 * 96 + 96).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(modules) { module ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (module.moduleCode == null) {
                                    val idx = backStack.indexOfLast { it is WorkspaceRoute.Modules }
                                    if (idx >= 0) while (backStack.size > idx + 1) backStack.removeLastOrNull()
                                } else {
                                    navigateToModule(backStack, module.moduleCode)
                                }
                            },
                        colors = CardDefaults.outlinedCardColors(),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (module.moduleCode == null) Icons.Default.Home
                                              else moduleCodeToIcon(module.moduleCode),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = module.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Account section
        item {
            Text(
                text = stringResource(Res.string.nav_more_account),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
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
                val items = listOf(
                    Triple(
                        stringResource(Res.string.nav_more_edit_profile),
                        Icons.Default.Person
                    ) { onEditProfile() },
                    Triple(
                        stringResource(Res.string.nav_more_workspace_settings),
                        Icons.Default.Settings
                    ) {
                        if (workspaceId.isNotEmpty()) {
                            backStack.add(WorkspaceRoute.Detail(workspaceId))
                        }
                    },
                    Triple(
                        stringResource(Res.string.nav_more_subscription),
                        Icons.Default.CreditCard
                    ) {
                        backStack.add(SubscriptionRoute.Root)
                    },
                    Triple(
                        stringResource(Res.string.nav_more_switch_workspace),
                        Icons.Default.Bolt
                    ) { onSwitchWorkspace() },
                )
                items.forEachIndexed { index, (label, icon, action) ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    ListItem(
                        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable { action() }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
