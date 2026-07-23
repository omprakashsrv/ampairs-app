package com.ampairs.pricing.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.pricing.domain.model.PriceListStatus
import com.ampairs.pricing.domain.model.SalesChannel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_add_item
import ampairsapp.feature.pricing.generated.resources.pricing_add_tier
import ampairsapp.feature.pricing.generated.resources.pricing_cd_remove_tier
import ampairsapp.feature.pricing.generated.resources.pricing_form_active
import ampairsapp.feature.pricing.generated.resources.pricing_form_channel
import ampairsapp.feature.pricing.generated.resources.pricing_form_currency
import ampairsapp.feature.pricing.generated.resources.pricing_form_customer_group
import ampairsapp.feature.pricing.generated.resources.pricing_form_edit_title
import ampairsapp.feature.pricing.generated.resources.pricing_form_name
import ampairsapp.feature.pricing.generated.resources.pricing_form_new_title
import ampairsapp.feature.pricing.generated.resources.pricing_form_priority
import ampairsapp.feature.pricing.generated.resources.pricing_form_save
import ampairsapp.feature.pricing.generated.resources.pricing_form_status
import ampairsapp.feature.pricing.generated.resources.pricing_item_moq
import ampairsapp.feature.pricing.generated.resources.pricing_item_product_id
import ampairsapp.feature.pricing.generated.resources.pricing_item_unit_price
import ampairsapp.feature.pricing.generated.resources.pricing_item_variant
import ampairsapp.feature.pricing.generated.resources.pricing_items_section
import ampairsapp.feature.pricing.generated.resources.pricing_remove_item
import ampairsapp.feature.pricing.generated.resources.pricing_tier_min_qty
import ampairsapp.feature.pricing.generated.resources.pricing_tier_price
import ampairsapp.feature.pricing.generated.resources.pricing_tiers_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListFormScreen(
    priceListId: String?,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PriceListFormViewModel = assistedMetroViewModel<PriceListFormViewModel, PriceListFormViewModel.Factory>(
        key = priceListId ?: "new",
    ) { create(priceListId) },
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (priceListId == null) stringResource(Res.string.pricing_form_new_title) else stringResource(Res.string.pricing_form_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(Res.string.pricing_form_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(Res.string.pricing_form_channel), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SalesChannel.entries.forEach { channel ->
                    FilterChip(
                        selected = state.channel == channel,
                        onClick = { viewModel.updateChannel(channel) },
                        label = { Text(channel.name) },
                    )
                }
            }

            Text(stringResource(Res.string.pricing_form_status), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriceListStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.status == status,
                        onClick = { viewModel.updateStatus(status) },
                        label = { Text(status.name) },
                    )
                }
            }

            OutlinedTextField(
                value = state.currency,
                onValueChange = viewModel::updateCurrency,
                label = { Text(stringResource(Res.string.pricing_form_currency)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.priority,
                onValueChange = viewModel::updatePriority,
                label = { Text(stringResource(Res.string.pricing_form_priority)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.customerGroupId,
                onValueChange = viewModel::updateCustomerGroupId,
                label = { Text(stringResource(Res.string.pricing_form_customer_group)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.pricing_form_active), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.active, onCheckedChange = viewModel::updateActive)
            }

            HorizontalDivider()
            Text(stringResource(Res.string.pricing_items_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            state.items.forEachIndexed { index, item ->
                ItemEditorCard(
                    item = item,
                    onProductId = { viewModel.updateItemProductId(index, it) },
                    onVariant = { viewModel.updateItemVariant(index, it) },
                    onUnitPrice = { viewModel.updateItemUnitPrice(index, it) },
                    onMoq = { viewModel.updateItemMoq(index, it) },
                    onAddTier = { viewModel.addTier(index) },
                    onRemoveTier = { tierIndex -> viewModel.removeTier(index, tierIndex) },
                    onTierMinQty = { tierIndex, v -> viewModel.updateTierMinQty(index, tierIndex, v) },
                    onTierPrice = { tierIndex, v -> viewModel.updateTierPrice(index, tierIndex, v) },
                    onRemoveItem = { viewModel.removeItem(index) },
                )
            }

            OutlinedButton(onClick = viewModel::addItem, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.pricing_add_item))
            }

            state.error?.let { err ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(onClick = { viewModel.save(onSaveSuccess) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.pricing_form_save))
            }
        }
    }
}

@Composable
private fun ItemEditorCard(
    item: ItemFormState,
    onProductId: (String) -> Unit,
    onVariant: (String) -> Unit,
    onUnitPrice: (String) -> Unit,
    onMoq: (String) -> Unit,
    onAddTier: () -> Unit,
    onRemoveTier: (Int) -> Unit,
    onTierMinQty: (Int, String) -> Unit,
    onTierPrice: (Int, String) -> Unit,
    onRemoveItem: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = item.productId,
                onValueChange = onProductId,
                label = { Text(stringResource(Res.string.pricing_item_product_id)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = item.variantSku,
                onValueChange = onVariant,
                label = { Text(stringResource(Res.string.pricing_item_variant)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = item.unitPrice,
                    onValueChange = onUnitPrice,
                    label = { Text(stringResource(Res.string.pricing_item_unit_price)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = item.moq,
                    onValueChange = onMoq,
                    label = { Text(stringResource(Res.string.pricing_item_moq)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(stringResource(Res.string.pricing_tiers_label), style = MaterialTheme.typography.labelLarge)
            item.tiers.forEachIndexed { tierIndex, tier ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tier.minQty,
                        onValueChange = { onTierMinQty(tierIndex, it) },
                        label = { Text(stringResource(Res.string.pricing_tier_min_qty)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = tier.unitPrice,
                        onValueChange = { onTierPrice(tierIndex, it) },
                        label = { Text(stringResource(Res.string.pricing_tier_price)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemoveTier(tierIndex) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.pricing_cd_remove_tier))
                    }
                }
            }
            TextButton(onClick = onAddTier) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.pricing_add_tier))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRemoveItem) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.pricing_remove_item), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
