package com.ampairs.tax.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TaxModuleScreen(
    onNavigateToCalculator: () -> Unit,
    onNavigateToMyTaxCodes: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToConfiguration: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tax Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            // Tax Calculator Card
            FeatureCard(
                title = "Tax Calculator",
                description = "Calculate taxes with country-specific rules and live breakdown",
                icon = Icons.Default.Calculate,
                onClick = onNavigateToCalculator
            )
        }

        item {
            // My Tax Codes Card
            FeatureCard(
                title = "My Tax Codes",
                description = "Manage your subscribed tax codes with favorites and usage tracking",
                icon = Icons.Default.List,
                onClick = onNavigateToMyTaxCodes
            )
        }

        item {
            // Browse Tax Codes Card
            FeatureCard(
                title = "Browse Tax Codes",
                description = "Search and import codes from global master registry",
                icon = Icons.Default.Search,
                onClick = onNavigateToSearch
            )
        }

        // Configuration option (if provided)
        onNavigateToConfiguration?.let { navigate ->
            item {
                FeatureCard(
                    title = "Tax Configuration",
                    description = "Configure country, tax strategy, and default settings",
                    icon = Icons.Default.Code,
                    onClick = navigate
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

