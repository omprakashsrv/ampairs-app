package com.ampairs.pricing.ui.geozone

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_cd_remove_tier
import ampairsapp.feature.pricing.generated.resources.pricing_form_active
import ampairsapp.feature.pricing.generated.resources.pricing_form_name
import ampairsapp.feature.pricing.generated.resources.pricing_form_save
import ampairsapp.feature.pricing.generated.resources.pricing_geo_add_range
import ampairsapp.feature.pricing.generated.resources.pricing_geo_edit_title
import ampairsapp.feature.pricing.generated.resources.pricing_geo_new_title
import ampairsapp.feature.pricing.generated.resources.pricing_geo_pincodes
import ampairsapp.feature.pricing.generated.resources.pricing_geo_range_from
import ampairsapp.feature.pricing.generated.resources.pricing_geo_range_to
import ampairsapp.feature.pricing.generated.resources.pricing_geo_ranges_label
import ampairsapp.feature.pricing.generated.resources.pricing_geo_states

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoZoneFormScreen(
    geoZoneId: String?,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeoZoneFormViewModel = assistedMetroViewModel<GeoZoneFormViewModel, GeoZoneFormViewModel.Factory>(
        key = geoZoneId ?: "new",
    ) { create(geoZoneId) },
) {
    val state by viewModel.formState.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
                text = if (geoZoneId == null) stringResource(Res.string.pricing_geo_new_title) else stringResource(Res.string.pricing_geo_edit_title),
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
            OutlinedTextField(
                value = state.pincodesText,
                onValueChange = viewModel::updatePincodes,
                label = { Text(stringResource(Res.string.pricing_geo_pincodes)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.statesText,
                onValueChange = viewModel::updateStates,
                label = { Text(stringResource(Res.string.pricing_geo_states)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(Res.string.pricing_geo_ranges_label), style = MaterialTheme.typography.labelLarge)
            state.ranges.forEachIndexed { index, range ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = range.from,
                        onValueChange = { viewModel.updateRangeFrom(index, it) },
                        label = { Text(stringResource(Res.string.pricing_geo_range_from)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = range.to,
                        onValueChange = { viewModel.updateRangeTo(index, it) },
                        label = { Text(stringResource(Res.string.pricing_geo_range_to)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.removeRange(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.pricing_cd_remove_tier))
                    }
                }
            }
            TextButton(onClick = viewModel::addRange) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.pricing_geo_add_range))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.pricing_form_active), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.active, onCheckedChange = viewModel::updateActive)
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
