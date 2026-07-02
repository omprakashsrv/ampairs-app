package com.ampairs.pricing.ui.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.pricing.domain.model.SalesChannel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_advanced_note
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_attribute_rules_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_attribute_rules_note
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_back
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_brand_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_category_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_channel_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_channel_retail
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_channel_retail_sub
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_channel_wholesale
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_channel_wholesale_sub
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_continue
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_create_zone
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_currency_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_currency_value
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_error_name_required
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_geo_zone_all
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_geo_zone_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_name_hint
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_name_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_optional_id_hint
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_preview
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_preview_all_products
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_preview_everyone
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_preview_everywhere
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_priority_hint
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_priority_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_product_group_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_products_hint
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_review_audience
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_review_channel
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_review_name
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_review_priority
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_review_products
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_review_sync_note
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_review_where
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_save_activate
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_where_hint
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_status_draft
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_status_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_step0_title
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_step1_title
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_step2_title
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_step3_title
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_step4_title
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_step5_title
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_step_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_time_window_label
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_time_window_value
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_title
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_customer
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_customer_sub
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_customer_value
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_everyone
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_everyone_sub
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_group
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_group_sub
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_group_value
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_hint
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_type
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_type_sub
import ampairsapp.feature.pricing.generated.resources.pricing_wizard_who_type_value

@Composable
fun PriceListWizardScreen(
    onSaved: () -> Unit,
    onCreateZone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PriceListWizardViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isLast = state.step == WIZARD_STEP_COUNT - 1

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.pricing_wizard_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                StepIndicator(step = state.step)
            }
        }
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.pricing_wizard_step_label, state.step + 1, WIZARD_STEP_COUNT),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { Text(stepTitle(state.step), style = MaterialTheme.typography.headlineSmall) }
            item { PreviewCard(state) }
            item {
                when (state.step) {
                    0 -> StepBasics(state, viewModel)
                    1 -> StepWho(state, viewModel)
                    2 -> StepProducts(state, viewModel)
                    3 -> StepWhere(state, viewModel, onCreateZone)
                    4 -> StepAdvanced(state, viewModel)
                    else -> StepReview(state)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        HorizontalDivider()
        // Footer
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = viewModel::back, enabled = state.step > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.pricing_wizard_back))
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f).clickable(enabled = !state.isSaving) {
                    if (isLast) viewModel.save(onSaved) else viewModel.next()
                },
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isLast) stringResource(Res.string.pricing_wizard_save_activate) else stringResource(Res.string.pricing_wizard_continue),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun stepTitle(step: Int): String = when (step) {
    0 -> stringResource(Res.string.pricing_wizard_step0_title)
    1 -> stringResource(Res.string.pricing_wizard_step1_title)
    2 -> stringResource(Res.string.pricing_wizard_step2_title)
    3 -> stringResource(Res.string.pricing_wizard_step3_title)
    4 -> stringResource(Res.string.pricing_wizard_step4_title)
    else -> stringResource(Res.string.pricing_wizard_step5_title)
}

@Composable
private fun StepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(WIZARD_STEP_COUNT) { index ->
            val color = if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(if (index == step) 10.dp else 8.dp)) {}
        }
    }
}

// ── live preview sentence ─────────────────────────────────────────────────────────

@Composable
private fun PreviewCard(state: WizardUiState) {
    val scope = when (state.scopeMode) {
        WizardScopeMode.EVERYONE -> stringResource(Res.string.pricing_wizard_preview_everyone)
        WizardScopeMode.CUSTOMER_GROUP -> state.scopeValue.ifBlank { stringResource(Res.string.pricing_wizard_who_group) }
        WizardScopeMode.CUSTOMER_TYPE -> state.scopeValue.ifBlank { stringResource(Res.string.pricing_wizard_who_type) }
        WizardScopeMode.CUSTOMER -> state.scopeValue.ifBlank { stringResource(Res.string.pricing_wizard_who_customer) }
    }
    val channel = channelLabel(state.channel)
    val products = state.brandId.ifBlank { state.categoryId }.ifBlank { state.productGroupId }
        .ifBlank { stringResource(Res.string.pricing_wizard_preview_all_products) }
    val zone = state.geoZones.firstOrNull { it.uid == state.geoZoneId }?.name
        ?: stringResource(Res.string.pricing_wizard_preview_everywhere)

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(Res.string.pricing_wizard_preview, scope, channel, products, zone),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun channelLabel(channel: SalesChannel): String = when (channel) {
    SalesChannel.WHOLESALE -> stringResource(Res.string.pricing_wizard_channel_wholesale)
    SalesChannel.RETAIL -> stringResource(Res.string.pricing_wizard_channel_retail)
}

// ── STEP 0 basics ─────────────────────────────────────────────────────────────────

@Composable
private fun StepBasics(state: WizardUiState, viewModel: PriceListWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(Res.string.pricing_wizard_name_label)) },
            placeholder = { Text(stringResource(Res.string.pricing_wizard_name_hint)) },
            isError = state.error == "name_required",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.error == "name_required") {
            Text(
                stringResource(Res.string.pricing_wizard_error_name_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(stringResource(Res.string.pricing_wizard_channel_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChannelCard(
                icon = Icons.Filled.Storefront,
                title = stringResource(Res.string.pricing_wizard_channel_wholesale),
                subtitle = stringResource(Res.string.pricing_wizard_channel_wholesale_sub),
                selected = state.channel == SalesChannel.WHOLESALE,
                onClick = { viewModel.setChannel(SalesChannel.WHOLESALE) },
                modifier = Modifier.weight(1f),
            )
            ChannelCard(
                icon = Icons.Filled.ShoppingBag,
                title = stringResource(Res.string.pricing_wizard_channel_retail),
                subtitle = stringResource(Res.string.pricing_wizard_channel_retail_sub),
                selected = state.channel == SalesChannel.RETAIL,
                onClick = { viewModel.setChannel(SalesChannel.RETAIL) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadOnlyField(
                label = stringResource(Res.string.pricing_wizard_currency_label),
                value = stringResource(Res.string.pricing_wizard_currency_value),
                modifier = Modifier.weight(1f),
            )
            ReadOnlyField(
                label = stringResource(Res.string.pricing_wizard_status_label),
                value = stringResource(Res.string.pricing_wizard_status_draft),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChannelCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = content)
            Text(title, style = MaterialTheme.typography.titleSmall, color = content)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = content)
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ── STEP 1 who ──────────────────────────────────────────────────────────────────────

@Composable
private fun StepWho(state: WizardUiState, viewModel: PriceListWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(stringResource(Res.string.pricing_wizard_who_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        ScopeOption(
            icon = Icons.Filled.Public,
            label = stringResource(Res.string.pricing_wizard_who_everyone),
            subtitle = stringResource(Res.string.pricing_wizard_who_everyone_sub),
            selected = state.scopeMode == WizardScopeMode.EVERYONE,
            onClick = { viewModel.setScopeMode(WizardScopeMode.EVERYONE) },
        )
        ScopeOption(
            icon = Icons.Filled.Groups,
            label = stringResource(Res.string.pricing_wizard_who_group),
            subtitle = stringResource(Res.string.pricing_wizard_who_group_sub),
            selected = state.scopeMode == WizardScopeMode.CUSTOMER_GROUP,
            onClick = { viewModel.setScopeMode(WizardScopeMode.CUSTOMER_GROUP) },
        )
        ScopeOption(
            icon = Icons.Filled.Category,
            label = stringResource(Res.string.pricing_wizard_who_type),
            subtitle = stringResource(Res.string.pricing_wizard_who_type_sub),
            selected = state.scopeMode == WizardScopeMode.CUSTOMER_TYPE,
            onClick = { viewModel.setScopeMode(WizardScopeMode.CUSTOMER_TYPE) },
        )
        ScopeOption(
            icon = Icons.Filled.Person,
            label = stringResource(Res.string.pricing_wizard_who_customer),
            subtitle = stringResource(Res.string.pricing_wizard_who_customer_sub),
            selected = state.scopeMode == WizardScopeMode.CUSTOMER,
            onClick = { viewModel.setScopeMode(WizardScopeMode.CUSTOMER) },
        )

        if (state.scopeMode != WizardScopeMode.EVERYONE) {
            val label = when (state.scopeMode) {
                WizardScopeMode.CUSTOMER_GROUP -> stringResource(Res.string.pricing_wizard_who_group_value)
                WizardScopeMode.CUSTOMER_TYPE -> stringResource(Res.string.pricing_wizard_who_type_value)
                WizardScopeMode.CUSTOMER -> stringResource(Res.string.pricing_wizard_who_customer_value)
                WizardScopeMode.EVERYONE -> ""
            }
            OutlinedTextField(
                value = state.scopeValue,
                onValueChange = viewModel::setScopeValue,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScopeOption(icon: ImageVector, label: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Icon(icon, contentDescription = null, tint = content)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = content)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = content)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
        }
    }
}

// ── STEP 2 products ─────────────────────────────────────────────────────────────────

@Composable
private fun StepProducts(state: WizardUiState, viewModel: PriceListWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(stringResource(Res.string.pricing_wizard_products_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconTextField(
            icon = Icons.Filled.BrandingWatermark,
            label = stringResource(Res.string.pricing_wizard_brand_label),
            value = state.brandId,
            onValueChange = viewModel::setBrandId,
        )
        IconTextField(
            icon = Icons.Filled.Category,
            label = stringResource(Res.string.pricing_wizard_category_label),
            value = state.categoryId,
            onValueChange = viewModel::setCategoryId,
        )
        IconTextField(
            icon = Icons.Filled.Inventory2,
            label = stringResource(Res.string.pricing_wizard_product_group_label),
            value = state.productGroupId,
            onValueChange = viewModel::setProductGroupId,
        )
    }
}

@Composable
private fun IconTextField(icon: ImageVector, label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(stringResource(Res.string.pricing_wizard_optional_id_hint)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── STEP 3 where ────────────────────────────────────────────────────────────────────

@Composable
private fun StepWhere(state: WizardUiState, viewModel: PriceListWizardViewModel, onCreateZone: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(stringResource(Res.string.pricing_wizard_where_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(stringResource(Res.string.pricing_wizard_geo_zone_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // "All zones" (clears selection)
        ZoneRow(
            name = stringResource(Res.string.pricing_wizard_geo_zone_all),
            selected = state.geoZoneId.isBlank(),
            onClick = { viewModel.setGeoZoneId("") },
        )
        state.geoZones.forEach { zone ->
            ZoneRow(
                name = zone.name,
                selected = state.geoZoneId == zone.uid,
                onClick = { viewModel.setGeoZoneId(zone.uid) },
            )
        }

        OutlinedButton(onClick = onCreateZone, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(7.dp))
            Text(stringResource(Res.string.pricing_wizard_create_zone))
        }
    }
}

@Composable
private fun ZoneRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = content)
            Text(name, style = MaterialTheme.typography.bodyLarge, color = content, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
        }
    }
}

// ── STEP 4 advanced ─────────────────────────────────────────────────────────────────

@Composable
private fun StepAdvanced(state: WizardUiState, viewModel: PriceListWizardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.pricing_wizard_advanced_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = state.priority,
            onValueChange = viewModel::setPriority,
            label = { Text(stringResource(Res.string.pricing_wizard_priority_label)) },
            supportingText = { Text(stringResource(Res.string.pricing_wizard_priority_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        InfoRow(
            icon = Icons.Filled.Category,
            label = stringResource(Res.string.pricing_wizard_attribute_rules_label),
            value = stringResource(Res.string.pricing_wizard_attribute_rules_note),
        )
        InfoRow(
            icon = Icons.Filled.Info,
            label = stringResource(Res.string.pricing_wizard_time_window_label),
            value = stringResource(Res.string.pricing_wizard_time_window_value),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── STEP 5 review ───────────────────────────────────────────────────────────────────

@Composable
private fun StepReview(state: WizardUiState) {
    val scope = when (state.scopeMode) {
        WizardScopeMode.EVERYONE -> stringResource(Res.string.pricing_wizard_who_everyone)
        WizardScopeMode.CUSTOMER_GROUP -> state.scopeValue.ifBlank { stringResource(Res.string.pricing_wizard_who_group) }
        WizardScopeMode.CUSTOMER_TYPE -> state.scopeValue.ifBlank { stringResource(Res.string.pricing_wizard_who_type) }
        WizardScopeMode.CUSTOMER -> state.scopeValue.ifBlank { stringResource(Res.string.pricing_wizard_who_customer) }
    }
    val products = state.brandId.ifBlank { state.categoryId }.ifBlank { state.productGroupId }
        .ifBlank { stringResource(Res.string.pricing_wizard_preview_all_products) }
    val zone = state.geoZones.firstOrNull { it.uid == state.geoZoneId }?.name
        ?: stringResource(Res.string.pricing_wizard_preview_everywhere)

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column {
                ReviewRow(stringResource(Res.string.pricing_wizard_review_name), state.name, divider = true)
                ReviewRow(stringResource(Res.string.pricing_wizard_review_channel), channelLabel(state.channel), divider = true)
                ReviewRow(stringResource(Res.string.pricing_wizard_review_audience), scope, divider = true)
                ReviewRow(stringResource(Res.string.pricing_wizard_review_products), products, divider = true)
                ReviewRow(stringResource(Res.string.pricing_wizard_review_where), zone, divider = true)
                ReviewRow(stringResource(Res.string.pricing_wizard_review_priority), state.priority.ifBlank { "0" }, divider = false)
            }
        }
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.pricing_wizard_review_sync_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(13.dp),
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, divider: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
