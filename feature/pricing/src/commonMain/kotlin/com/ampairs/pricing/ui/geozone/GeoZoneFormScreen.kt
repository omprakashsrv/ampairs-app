package com.ampairs.pricing.ui.geozone

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_form_active
import ampairsapp.feature.pricing.generated.resources.pricing_form_save
import ampairsapp.feature.pricing.generated.resources.pricing_geo_add_range
import ampairsapp.feature.pricing.generated.resources.pricing_geo_edit_title
import ampairsapp.feature.pricing.generated.resources.pricing_geo_new_title
import ampairsapp.feature.pricing.generated.resources.pricing_geo_range_from
import ampairsapp.feature.pricing.generated.resources.pricing_geo_range_to
import ampairsapp.feature.pricing.generated.resources.pricing_geo_ranges_label
import ampairsapp.feature.pricing.generated.resources.pricing_geob_add
import ampairsapp.feature.pricing.generated.resources.pricing_geob_add_pincode
import ampairsapp.feature.pricing.generated.resources.pricing_geob_add_state
import ampairsapp.feature.pricing.generated.resources.pricing_geob_cancel
import ampairsapp.feature.pricing.generated.resources.pricing_geob_cd_remove_pincode
import ampairsapp.feature.pricing.generated.resources.pricing_geob_cd_remove_range
import ampairsapp.feature.pricing.generated.resources.pricing_geob_cd_remove_state
import ampairsapp.feature.pricing.generated.resources.pricing_geob_in_zone
import ampairsapp.feature.pricing.generated.resources.pricing_geob_members_count
import ampairsapp.feature.pricing.generated.resources.pricing_geob_name_required
import ampairsapp.feature.pricing.generated.resources.pricing_geob_not_found
import ampairsapp.feature.pricing.generated.resources.pricing_geob_not_in_zone
import ampairsapp.feature.pricing.generated.resources.pricing_geob_pincodes
import ampairsapp.feature.pricing.generated.resources.pricing_geob_pincodes_hint
import ampairsapp.feature.pricing.generated.resources.pricing_geob_save_failed
import ampairsapp.feature.pricing.generated.resources.pricing_geob_save_zone
import ampairsapp.feature.pricing.generated.resources.pricing_geob_state_hint
import ampairsapp.feature.pricing.generated.resources.pricing_geob_states
import ampairsapp.feature.pricing.generated.resources.pricing_geob_test_hint
import ampairsapp.feature.pricing.generated.resources.pricing_geob_test_in_msg
import ampairsapp.feature.pricing.generated.resources.pricing_geob_test_out_msg
import ampairsapp.feature.pricing.generated.resources.pricing_geob_test_title
import ampairsapp.feature.pricing.generated.resources.pricing_geob_zone_name

private val mono = FontFamily.Monospace

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (geoZoneId == null) stringResource(Res.string.pricing_geo_new_title) else stringResource(Res.string.pricing_geo_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                // Zone name (primary-outlined card, mirrors design header field)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::updateName,
                            label = { Text(stringResource(Res.string.pricing_geob_zone_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                PincodesSection(state, viewModel)
                RangesSection(state, viewModel)
                StatesSection(state, viewModel)
                TestSection(state, viewModel)

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
                        Text(
                            text = localizedError(err),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Footer: Cancel + Save zone
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(onClick = onSaveSuccess) {
                        Text(stringResource(Res.string.pricing_geob_cancel))
                    }
                    Button(onClick = { viewModel.save(onSaveSuccess) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.pricing_geob_save_zone))
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedError(marker: String): String = when (marker) {
    "not_found" -> stringResource(Res.string.pricing_geob_not_found)
    "name_required" -> stringResource(Res.string.pricing_geob_name_required)
    "save_failed" -> stringResource(Res.string.pricing_geob_save_failed)
    else -> marker
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (trailing != null) {
                    Text(trailing, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PincodesSection(state: GeoZoneFormState, viewModel: GeoZoneFormViewModel) {
    SectionCard(
        icon = Icons.Filled.PinDrop,
        title = stringResource(Res.string.pricing_geob_pincodes),
        trailing = stringResource(Res.string.pricing_geob_pincodes_hint),
    ) {
        if (state.pincodes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                state.pincodes.forEach { pin ->
                    RemovableChip(
                        label = pin,
                        monospace = true,
                        bg = MaterialTheme.colorScheme.surfaceContainerHighest,
                        fg = MaterialTheme.colorScheme.onSurface,
                        removeCd = stringResource(Res.string.pricing_geob_cd_remove_pincode),
                        onRemove = { viewModel.removePincode(pin) },
                    )
                }
            }
        }
        AddInlineRow(
            value = state.pincodeDraft,
            onValueChange = viewModel::updatePincodeDraft,
            label = stringResource(Res.string.pricing_geob_add_pincode),
            keyboardType = KeyboardType.Number,
            onAdd = viewModel::addPincode,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StatesSection(state: GeoZoneFormState, viewModel: GeoZoneFormViewModel) {
    SectionCard(icon = Icons.Filled.Public, title = stringResource(Res.string.pricing_geob_states)) {
        if (state.states.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                state.states.forEach { st ->
                    RemovableChip(
                        label = st,
                        monospace = false,
                        leadingIcon = Icons.Filled.CheckCircle,
                        bg = MaterialTheme.colorScheme.tertiaryContainer,
                        fg = MaterialTheme.colorScheme.onTertiaryContainer,
                        removeCd = stringResource(Res.string.pricing_geob_cd_remove_state),
                        onRemove = { viewModel.removeState(st) },
                    )
                }
            }
        }
        AddInlineRow(
            value = state.stateDraft,
            onValueChange = viewModel::updateStateDraft,
            label = stringResource(Res.string.pricing_geob_state_hint),
            keyboardType = KeyboardType.Text,
            addLabel = stringResource(Res.string.pricing_geob_add_state),
            onAdd = viewModel::addState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangesSection(state: GeoZoneFormState, viewModel: GeoZoneFormViewModel) {
    SectionCard(icon = Icons.Filled.LinearScale, title = stringResource(Res.string.pricing_geo_ranges_label)) {
        state.ranges.forEachIndexed { index, range ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = range.from,
                    onValueChange = { viewModel.updateRangeFrom(index, it) },
                    label = { Text(stringResource(Res.string.pricing_geo_range_from)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = range.to,
                    onValueChange = { viewModel.updateRangeTo(index, it) },
                    label = { Text(stringResource(Res.string.pricing_geo_range_to)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.removeRange(index) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.pricing_geob_cd_remove_range), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        DashedAddButton(label = stringResource(Res.string.pricing_geo_add_range), onClick = viewModel::addRange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestSection(state: GeoZoneFormState, viewModel: GeoZoneFormViewModel) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.pricing_geob_test_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(Res.string.pricing_geob_members_count, state.memberCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = state.testPincode,
                    onValueChange = viewModel::updateTestPincode,
                    label = { Text(stringResource(Res.string.pricing_geob_test_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                val inZone = state.testInZone
                val bg = if (inZone) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                val fg = if (inZone) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(color = bg, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            if (inZone) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            if (inZone) stringResource(Res.string.pricing_geob_in_zone) else stringResource(Res.string.pricing_geob_not_in_zone),
                            style = MaterialTheme.typography.labelMedium,
                            color = fg,
                        )
                    }
                }
            }
            if (state.testPincode.isNotBlank()) {
                Text(
                    text = if (state.testInZone) stringResource(Res.string.pricing_geob_test_in_msg) else stringResource(Res.string.pricing_geob_test_out_msg),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.testInZone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// --- Small reusable pieces ----------------------------------------------------------------

@Composable
private fun RemovableChip(
    label: String,
    monospace: Boolean,
    bg: Color,
    fg: Color,
    removeCd: String,
    onRemove: () -> Unit,
    leadingIcon: ImageVector? = null,
) {
    Surface(color = bg, shape = MaterialTheme.shapes.extraLarge) {
        Row(
            Modifier.padding(start = if (leadingIcon != null) 8.dp else 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontFamily = if (monospace) mono else null,
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = removeCd,
                tint = fg,
                modifier = Modifier.size(16.dp).clickable(onClick = onRemove),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInlineRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    onAdd: () -> Unit,
    addLabel: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            modifier = Modifier.weight(1f),
        )
        DashedAddButton(label = addLabel ?: stringResource(Res.string.pricing_geob_add), onClick = onAdd)
    }
}

@Composable
private fun DashedAddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
