package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.business.domain.BusinessOverview
import com.ampairs.business.ui.components.BusinessChecklistItem
import com.ampairs.business.ui.components.BusinessCompletenessCard
import com.ampairs.business.ui.components.BusinessIdentityHeader
import com.ampairs.business.ui.components.BusinessRowTone
import com.ampairs.business.ui.components.BusinessScreenContent
import com.ampairs.business.ui.components.BusinessSectionTitle
import com.ampairs.business.ui.components.BusinessSettingsEntry
import com.ampairs.business.ui.components.BusinessSettingsList
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Business Overview — the identity-first hub of the Business module.
 *
 * Implements the Ampairs "Business Module" redesign: a gradient identity header,
 * a setup-completeness checklist, and settings rows that preview their current
 * value and drill into the individual editors.
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasCustomAttributes by viewModel.hasCustomAttributes.collectAsStateWithLifecycle()

    BusinessScreenContent(modifier = modifier) {
            when {
                uiState.isLoading && uiState.overview == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.overview == null -> {
                    val errorText = uiState.error ?: "Unknown error"
                    if (errorText.contains("not found", ignoreCase = true)) {
                        CreateProfilePrompt(onCreate = onNavigateToProfile)
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
                    val logoUrl = uiState.logoThumbnailUrl?.let { "$it?v=${uiState.logoCacheBuster}" }
                    val hasLogo = uiState.logoThumbnailUrl != null
                    val typeLabel = overview.businessType.ifBlank { "Business" }

                    BusinessIdentityHeader(
                        name = overview.name.ifBlank { "Your business" },
                        initials = initialsFrom(overview.name),
                        logoUrl = logoUrl,
                        chips = listOf(Icons.Filled.Storefront to typeLabel),
                        contacts = buildList<Pair<ImageVector, String>> {
                            overview.phone?.takeIf { it.isNotBlank() }?.let { add(Icons.Filled.Phone to it) }
                            overview.email?.takeIf { it.isNotBlank() }?.let { add(Icons.Filled.Mail to it) }
                            overview.address.takeIf { it.isNotBlank() }?.let { add(Icons.Filled.LocationOn to it) }
                        },
                    )

                    val checklist = buildChecklist(
                        overview = overview,
                        hasLogo = hasLogo,
                        hasCustomAttributes = hasCustomAttributes,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToImages = onNavigateToImages,
                        onNavigateToCustomAttributes = onNavigateToCustomAttributes,
                    )
                    if (checklist.any { !it.done }) {
                        BusinessCompletenessCard(items = checklist)
                    }

                    BusinessSectionTitle(icon = Icons.Filled.Settings, text = "Settings")

                    val entries = buildList {
                        add(
                            BusinessSettingsEntry(
                                icon = Icons.Filled.Business,
                                tone = BusinessRowTone.Primary,
                                title = "Profile & Registration",
                                value = listOf(overview.name, typeLabel).filter { it.isNotBlank() }.joinToString(" · "),
                                onClick = onNavigateToProfile,
                            )
                        )
                        add(
                            BusinessSettingsEntry(
                                icon = Icons.Filled.Tune,
                                tone = BusinessRowTone.Indigo,
                                title = "Operations",
                                value = listOf(overview.currency, overview.timezone)
                                    .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Currency, timezone & hours" },
                                onClick = onNavigateToOperations,
                            )
                        )
                        add(
                            BusinessSettingsEntry(
                                icon = Icons.Filled.Image,
                                tone = BusinessRowTone.Amber,
                                title = "Logo & Gallery",
                                value = if (hasLogo) "Logo set" else "No logo yet — appears on invoices",
                                missing = !hasLogo,
                                onClick = onNavigateToImages,
                            )
                        )
                        if (hasCustomAttributes) {
                            add(
                                BusinessSettingsEntry(
                                    icon = Icons.Filled.Code,
                                    tone = BusinessRowTone.Primary,
                                    title = "Custom Attributes",
                                    value = "Additional business information",
                                    onClick = onNavigateToCustomAttributes,
                                )
                            )
                        }
                    }
                    BusinessSettingsList(entries = entries)
                }
            }
    }
}

@Composable
private fun CreateProfilePrompt(onCreate: () -> Unit) {
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
                Icons.Filled.Business,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Business Profile Found",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create your business profile to get started with managing your business operations, settings, and compliance.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Business Profile")
            }
        }
    }
}

private fun buildChecklist(
    overview: BusinessOverview,
    hasLogo: Boolean,
    hasCustomAttributes: Boolean,
    onNavigateToProfile: () -> Unit,
    onNavigateToImages: () -> Unit,
    onNavigateToCustomAttributes: () -> Unit,
): List<BusinessChecklistItem> = buildList {
    add(
        BusinessChecklistItem(
            label = "Business name & type",
            hint = overview.businessType.ifBlank { "Add your business name and type" },
            done = overview.name.isNotBlank(),
            onClick = onNavigateToProfile,
        )
    )
    val hasContact = !overview.phone.isNullOrBlank() || !overview.email.isNullOrBlank()
    add(
        BusinessChecklistItem(
            label = "Contact details",
            hint = overview.phone?.takeIf { it.isNotBlank() }
                ?: overview.email?.takeIf { it.isNotBlank() }
                ?: "Add a phone number or email",
            done = hasContact,
            onClick = onNavigateToProfile,
        )
    )
    add(
        BusinessChecklistItem(
            label = "Registered address",
            hint = overview.address.ifBlank { "Printed on GST invoices" },
            done = overview.address.isNotBlank(),
            onClick = onNavigateToProfile,
        )
    )
    add(
        BusinessChecklistItem(
            label = "Upload a logo",
            hint = if (hasLogo) "Logo set" else "Appears on invoices & orders",
            done = hasLogo,
            onClick = onNavigateToImages,
        )
    )
    add(
        BusinessChecklistItem(
            label = "Custom attributes",
            hint = if (hasCustomAttributes) "Configured" else "Capture extra business information",
            done = hasCustomAttributes,
            onClick = onNavigateToCustomAttributes,
        )
    )
}

private fun initialsFrom(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
