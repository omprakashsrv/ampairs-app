package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.form.render.ConfigAttributesSection
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Business Custom Attributes screen (spec 011, US5) — fields render from the unified business
 * `FormSchema` via the shared [ConfigAttributesSection]; values persist through the existing
 * business-profile update path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCustomAttributesScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessCustomAttributesViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formSchema by viewModel.formSchema.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Custom attributes saved successfully")
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.hasCustomAttributes && !uiState.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.saveCustomAttributes() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Custom Attributes")
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isRefreshing = true
                    viewModel.refresh()
                }
            }
        ) {
            when {
                uiState.isLoading && formSchema == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                !uiState.hasCustomAttributes -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "No Custom Attributes",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Custom attributes can be configured in Form Configuration to capture additional business information.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Custom Attributes",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            if (uiState.businessName.isNotBlank()) {
                                Text(
                                    text = uiState.businessName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Config-driven custom attributes from the unified business FormSchema.
                        ConfigAttributesSection(
                            schema = formSchema,
                            optionRegistry = viewModel.optionRegistry,
                            widgetRegistry = viewModel.widgetRegistry,
                            attributes = uiState.customAttributeValues,
                            onAttributesChange = viewModel::updateAttributes,
                        )

                        // Bottom spacing for FAB
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
