package com.ampairs.business.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ampairs.business.ui.BusinessOverviewViewModel
import com.ampairs.business.ui.components.BusinessAction
import com.ampairs.business.ui.components.BusinessActionGrid
import com.ampairs.business.ui.components.BusinessScreenContent
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Business Overview Screen - Dashboard with key business information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessOverviewScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToOperations: () -> Unit = {},
    onNavigateToCustomAttributes: () -> Unit = {},
    onNavigateToImages: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BusinessOverviewViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasCustomAttributes by viewModel.hasCustomAttributes.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                viewModel.refresh()
            }
        }
    ) {
        BusinessScreenContent {
            Text(
                text = "Business Overview",
                style = MaterialTheme.typography.headlineMedium
            )

            when {
                uiState.isLoading && uiState.overview == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    val errorText = uiState.error ?: "Unknown error"

                    // Show create profile option if business doesn't exist
                    if (errorText.contains("not found", ignoreCase = true)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Business,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "No Business Profile Found",
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = "Create your business profile to get started with managing your business operations, settings, and compliance.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onNavigateToProfile,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create Business Profile")
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                uiState.overview != null -> {
                    val overview = uiState.overview!!

                    // Business Info Card
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Business details on the left
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(overview.name, style = MaterialTheme.typography.headlineSmall)
                                Text("Type: ${overview.businessType}", style = MaterialTheme.typography.bodyMedium)
                                if (overview.email != null) {
                                    Text("Email: ${overview.email}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (overview.phone != null) {
                                    Text("Phone: ${overview.phone}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (overview.address.isNotBlank()) {
                                    Text("Address: ${overview.address}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("Currency: ${overview.currency} | Timezone: ${overview.timezone}", style = MaterialTheme.typography.bodySmall)
                            }

                            // Logo on the right (if available)
                            if (uiState.logoThumbnailUrl != null) {
                                val logoUrl = "${uiState.logoThumbnailUrl}?v=${uiState.logoCacheBuster}"
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = "Business Logo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Quick Actions — one column on phones, two on wider (tablet/desktop) windows.
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium)

                    val actions = buildList {
                        add(
                            BusinessAction(
                                icon = Icons.Default.Business,
                                title = "Profile & Registration",
                                subtitle = "Company details and registration info",
                                onClick = onNavigateToProfile,
                            )
                        )
                        add(
                            BusinessAction(
                                icon = Icons.Default.Settings,
                                title = "Operations",
                                subtitle = "Timezone, currency, business hours",
                                onClick = onNavigateToOperations,
                            )
                        )
                        add(
                            BusinessAction(
                                icon = Icons.Default.Image,
                                title = "Logo & Gallery",
                                subtitle = "Business logo and gallery images",
                                onClick = onNavigateToImages,
                            )
                        )
                        // Custom Attributes — only when some are defined.
                        if (hasCustomAttributes) {
                            add(
                                BusinessAction(
                                    icon = Icons.Default.Extension,
                                    title = "Custom Attributes",
                                    subtitle = "Additional business information and custom fields",
                                    onClick = onNavigateToCustomAttributes,
                                )
                            )
                        }
                    }
                    BusinessActionGrid(actions = actions)
                }
            }
        }
    }
}
