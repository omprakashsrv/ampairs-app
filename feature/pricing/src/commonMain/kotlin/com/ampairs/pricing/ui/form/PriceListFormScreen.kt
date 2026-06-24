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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import ampairsapp.feature.pricing.generated.resources.pricing_form_active
import ampairsapp.feature.pricing.generated.resources.pricing_form_channel
import ampairsapp.feature.pricing.generated.resources.pricing_form_currency
import ampairsapp.feature.pricing.generated.resources.pricing_form_customer_group
import ampairsapp.feature.pricing.generated.resources.pricing_form_edit_title
import ampairsapp.feature.pricing.generated.resources.pricing_form_items_count
import ampairsapp.feature.pricing.generated.resources.pricing_form_name
import ampairsapp.feature.pricing.generated.resources.pricing_form_new_title
import ampairsapp.feature.pricing.generated.resources.pricing_form_priority
import ampairsapp.feature.pricing.generated.resources.pricing_form_save
import ampairsapp.feature.pricing.generated.resources.pricing_form_status

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
    val state by viewModel.formState.collectAsState()

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

            if (priceListId != null) {
                Text(
                    text = stringResource(Res.string.pricing_form_items_count, state.itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
