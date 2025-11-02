package com.ampairs.workspace.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.ModuleDetailResponse
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Module Store Screen
 * Displays marketplace with search and installed modules in two separate tabs
 */
@Composable
fun ModuleStoreScreen(
    navController: NavController,
    workspaceId: String = "",
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: WorkspaceModulesViewModel = koinViewModel { parametersOf(workspaceId.takeIf { it.isNotEmpty() }) }
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showModuleDetails by remember { mutableStateOf(false) }
    var selectedModuleId by remember { mutableStateOf<String?>(null) }
    var installingModules by remember { mutableStateOf(setOf<String>()) }

    // Observe ViewModel state
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val availableModules by viewModel.availableModules.collectAsState()
    val installedModules by viewModel.installedModules.collectAsState()

    // Filter modules based on search query
    val filteredAvailableModules = remember(availableModules, searchQuery, installedModules) {
        if (searchQuery.isBlank()) {
            availableModules
        } else {
            availableModules.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description?.contains(searchQuery, ignoreCase = true) == true ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }.filter { available ->
            // Exclude already installed modules from marketplace
            installedModules.none { it.moduleCode == available.moduleCode }
        }
    }

    val filteredInstalledModules = remember(installedModules, searchQuery) {
        if (searchQuery.isBlank()) {
            installedModules
        } else {
            installedModules.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description?.contains(searchQuery, ignoreCase = true) == true ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Load data on first composition
    LaunchedEffect(workspaceId) {
        println("ModuleStoreScreen: LaunchedEffect triggered with workspaceId: $workspaceId")
        if (workspaceId.isNotEmpty()) {
            println("ModuleStoreScreen: Loading installed modules...")
            viewModel.loadInstalledModules()
            println("ModuleStoreScreen: Loading available modules...")
            viewModel.loadAvailableModules(refresh = true)
        } else {
            println("ModuleStoreScreen: ERROR - workspaceId is empty!")
        }
    }

    // Debug: Log state changes
    LaunchedEffect(isLoading) {
        println("ModuleStoreScreen: isLoading = $isLoading")
    }

    LaunchedEffect(availableModules.size, installedModules.size) {
        println("ModuleStoreScreen: availableModules.size = ${availableModules.size}, installedModules.size = ${installedModules.size}")
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            println("ModuleStoreScreen: Error = $it")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClearQuery = { searchQuery = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Marketplace (${filteredAvailableModules.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Installed (${filteredInstalledModules.size})") }
                )
            }

            // Error display
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Tab content
                when (selectedTab) {
                    0 -> MarketplaceTab(
                        modules = filteredAvailableModules,
                        installingModules = installingModules,
                        onModuleClick = { module ->
                            selectedModuleId = module.moduleCode
                            showModuleDetails = true
                        },
                        onInstall = { moduleCode ->
                            viewModel.clearError()
                            installingModules = installingModules + moduleCode
                            viewModel.installModule(moduleCode) { response ->
                                installingModules = installingModules - moduleCode
                                if (response != null && response.success) {
                                    // Refresh lists after installation
                                    viewModel.loadInstalledModules()
                                    viewModel.loadAvailableModules(refresh = true)
                                }
                            }
                        }
                    )
                    1 -> InstalledTab(
                        modules = filteredInstalledModules,
                        onModuleClick = { module ->
                            selectedModuleId = module.moduleCode
                            showModuleDetails = true
                        },
                        onUninstall = { moduleCode ->
                            viewModel.clearError()
                            viewModel.uninstallModule(moduleCode) { response ->
                                if (response != null && response.success) {
                                    // Refresh lists after uninstallation
                                    viewModel.loadInstalledModules()
                                    viewModel.loadAvailableModules(refresh = true)
                                }
                            }
                        },
                        onNavigate = { moduleCode ->
                            // Navigate to module and close store
                            val route = when (moduleCode) {
                                "customer-management" -> Route.Customer
                                "product-management" -> Route.Product
                                "order-management" -> Route.Order
                                "invoice-management" -> Route.Invoice
                                "inventory-management" -> Route.Inventory
                                "tax-code-management" -> Route.Tax
                                else -> null
                            }
                            route?.let {
                                navController.navigate(it) {
                                    // Clear the module store from back stack
                                    popUpTo(WorkspaceRoute.ModuleStore(workspaceId)) {
                                        inclusive = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

    // Module details dialog
    if (showModuleDetails && selectedModuleId != null) {
        ModuleDetailsDialog(
            moduleId = selectedModuleId!!,
            workspaceId = workspaceId,
            onDismiss = {
                showModuleDetails = false
                selectedModuleId = null
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search modules...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun MarketplaceTab(
    modules: List<AvailableModule>,
    installingModules: Set<String>,
    onModuleClick: (AvailableModule) -> Unit,
    onInstall: (String) -> Unit
) {
    if (modules.isEmpty()) {
        EmptyState(
            message = "No modules found in marketplace",
            description = "Try adjusting your search criteria"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(modules) { module ->
                MarketplaceModuleCard(
                    module = module,
                    isInstalling = installingModules.contains(module.moduleCode),
                    onClick = { onModuleClick(module) },
                    onInstall = { onInstall(module.moduleCode) }
                )
            }
        }
    }
}

@Composable
private fun InstalledTab(
    modules: List<InstalledModule>,
    onModuleClick: (InstalledModule) -> Unit,
    onUninstall: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    if (modules.isEmpty()) {
        EmptyState(
            message = "No modules installed",
            description = "Browse the marketplace to install modules"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(modules) { module ->
                InstalledModuleCard(
                    module = module,
                    onClick = { onModuleClick(module) },
                    onUninstall = { onUninstall(module.moduleCode) },
                    onNavigate = { onNavigate(module.moduleCode) }
                )
            }
        }
    }
}

@Composable
private fun MarketplaceModuleCard(
    module: AvailableModule,
    isInstalling: Boolean,
    onClick: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Module icon
            Card(
                modifier = Modifier.size(56.dp),
                colors = CardDefaults.cardColors(
                    containerColor = parseHexColor(module.primaryColor)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = module.name.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Module info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (module.featured) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Featured",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${module.category} • ${module.requiredTier}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⭐ ${module.rating}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${module.sizeMb}MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = module.complexity,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (module.complexity) {
                            "Simple" -> MaterialTheme.colorScheme.primary
                            "Medium" -> MaterialTheme.colorScheme.tertiary
                            "Advanced" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                module.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Install button
            Button(
                onClick = onInstall,
                enabled = !isInstalling,
                modifier = Modifier.width(100.dp)
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Install")
                }
            }
        }
    }
}

@Composable
private fun InstalledModuleCard(
    module: InstalledModule,
    onClick: () -> Unit,
    onUninstall: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Module icon
            Card(
                modifier = Modifier.size(56.dp),
                colors = CardDefaults.cardColors(
                    containerColor = parseHexColor(module.primaryColor)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = module.name.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Module info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (module.status) {
                                "ACTIVE" -> MaterialTheme.colorScheme.primary
                                "INSTALLED" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    ) {
                        Text(
                            text = module.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${module.category} • v${module.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                module.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onNavigate,
                    enabled = module.status == "ACTIVE",
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("Open")
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onUninstall,
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("Uninstall", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ModuleDetailsDialog(
    moduleId: String,
    workspaceId: String,
    onDismiss: () -> Unit,
    viewModel: WorkspaceModulesViewModel = koinViewModel { parametersOf(workspaceId.takeIf { it.isNotEmpty() }) }
) {
    var moduleDetails by remember { mutableStateOf<ModuleDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(moduleId) {
        isLoading = true
        viewModel.getModuleDetails(moduleId) { result ->
            isLoading = false
            when {
                result != null -> moduleDetails = result
                else -> errorMessage = "Failed to load module details"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Module Details") },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    moduleDetails != null -> {
                        ModuleDetailsContent(moduleDetails!!)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ModuleDetailsContent(details: ModuleDetailResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Module Info Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Module Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Name", details.moduleInfo.name)
                    DetailRow("Category", details.moduleInfo.category)
                    DetailRow("Version", details.moduleInfo.version)
                    DetailRow("Status", details.moduleInfo.status)
                    DetailRow("Description", details.moduleInfo.description)
                }
            }
        }

        item {
            // Analytics Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Daily Active Users", "${details.analytics.dailyActiveUsers}")
                    DetailRow("Monthly Access", "${details.analytics.monthlyAccess}")
                    DetailRow("Avg Session", details.analytics.averageSessionDuration)
                }
            }
        }

        item {
            // Configuration Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Auto Sync", if (details.configuration.autoSync) "Enabled" else "Disabled")
                    DetailRow("Notifications", if (details.configuration.notificationsEnabled) "Enabled" else "Disabled")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(
    message: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
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
