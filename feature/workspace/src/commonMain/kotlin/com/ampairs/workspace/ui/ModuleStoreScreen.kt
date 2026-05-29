package com.ampairs.workspace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.workspace.generated.resources.Res
import ampairsapp.feature.workspace.generated.resources.workspace_modules
import ampairsapp.feature.workspace.generated.resources.workspace_modules_subtitle
import ampairsapp.feature.workspace.generated.resources.workspace_modules_fetching
import ampairsapp.feature.workspace.generated.resources.workspace_no_modules
import ampairsapp.feature.workspace.generated.resources.workspace_module_core
import ampairsapp.feature.workspace.generated.resources.workspace_module_dismiss_error

@Composable
fun ModuleStoreScreen(
    workspaceId: String = "",
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: WorkspaceModulesViewModel = assistedMetroViewModel<WorkspaceModulesViewModel, WorkspaceModulesViewModel.Factory> { create(workspaceId) },
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val availableModules by viewModel.availableModules.collectAsStateWithLifecycle()
    val installedModules by viewModel.installedModules.collectAsStateWithLifecycle()
    var togglingModules by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(workspaceId) {
        if (workspaceId.isNotEmpty()) {
            viewModel.loadInstalledModules()
            viewModel.loadAvailableModules(refresh = true)
        }
    }

    val activeCount = installedModules.count { it.status == "ACTIVE" && it.enabled }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(Res.string.workspace_modules),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (availableModules.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.workspace_modules_subtitle, activeCount, availableModules.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Error banner
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(Res.string.workspace_module_dismiss_error))
                    }
                }
            }
        }

        // Content
        when {
            isLoading && availableModules.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(Res.string.workspace_modules_fetching),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            availableModules.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.workspace_no_modules),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(availableModules, key = { it.moduleCode }) { module ->
                        val installed = installedModules.find { it.moduleCode == module.moduleCode }
                        val isEnabled = installed != null
                        val isToggling = module.moduleCode in togglingModules

                        ModuleToggleCard(
                            module = module,
                            isEnabled = isEnabled,
                            isToggling = isToggling,
                            onToggle = {
                                if (!isToggling) {
                                    togglingModules = togglingModules + module.moduleCode
                                    if (installed != null) {
                                        viewModel.uninstallModule(installed.id) { _ ->
                                            togglingModules = togglingModules - module.moduleCode
                                        }
                                    } else {
                                        viewModel.installModule(module.moduleCode) { _ ->
                                            togglingModules = togglingModules - module.moduleCode
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleToggleCard(
    module: AvailableModule,
    isEnabled: Boolean,
    isToggling: Boolean,
    onToggle: () -> Unit,
) {
    val borderColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isEnabled) 2.dp else 1.dp
    val containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    OutlinedCard(
        onClick = { if (!isToggling) onToggle() },
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Module initial icon
                Surface(
                    shape = CircleShape,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = module.name.first().uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Switch or per-card spinner
                if (isToggling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.scale(0.75f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Name + optional "core" badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false),
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                if (module.featured && module.requiredTier == "FREE") {
                    Text(
                        text = stringResource(Res.string.workspace_module_core),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            module.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
