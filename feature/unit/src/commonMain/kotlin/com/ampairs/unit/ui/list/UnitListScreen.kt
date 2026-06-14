package com.ampairs.unit.ui.list

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.unit.domain.model.Unit
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.unit.generated.resources.Res
import ampairsapp.feature.unit.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitListScreen(
    onUnitClick: (String) -> kotlin.Unit,
    onAddUnit: () -> kotlin.Unit,
    viewModel: UnitListViewModel = metroViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    LaunchedEffect(kotlin.Unit) {
        viewModel.syncUnits()
    }

    Scaffold(
        floatingActionButton = {
            if (!isExpanded) {
                ExtendedFloatingActionButton(
                    onClick = onAddUnit,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.unit_list_add_unit)) }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Inline header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.unit_list_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.units.isNotEmpty()) {
                                Text(
                                    text = stringResource(Res.string.unit_list_count, uiState.units.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isExpanded) {
                            Button(onClick = onAddUnit) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.unit_list_add_unit))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.unit_list_search_placeholder), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.unit_list_cd_search_clear), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            HorizontalDivider()

            // Content
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(Res.string.unit_error_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.error ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                uiState.units.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) stringResource(Res.string.unit_list_empty_search) else stringResource(Res.string.unit_list_empty_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.searchQuery.isBlank()) {
                                Button(onClick = onAddUnit) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(Res.string.unit_list_create_unit))
                                }
                            }
                        }
                    }
                }
                isExpanded -> {
                    UnitsTable(
                        units = uiState.units,
                        onUnitClick = onUnitClick,
                        onDelete = { viewModel.deleteUnit(it) }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.units, key = { it.uid }) { unit ->
                            UnitCard(
                                unit = unit,
                                onClick = { onUnitClick(unit.uid) },
                                onDelete = { viewModel.deleteUnit(unit.uid) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitsTable(
    units: List<Unit>,
    onUnitClick: (String) -> kotlin.Unit,
    onDelete: (String) -> kotlin.Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(Res.string.unit_list_col_name), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.5f))
                Text(text = stringResource(Res.string.unit_list_col_short_name), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(text = stringResource(Res.string.unit_list_col_category), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(text = stringResource(Res.string.unit_list_col_decimals), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f))
                Text(text = stringResource(Res.string.unit_list_col_status), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f))
                Spacer(Modifier.width(48.dp))
            }
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(units, key = { it.uid }) { unit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUnitClick(unit.uid) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(text = unit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        unit.description?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    Text(text = unit.shortName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = unit.category ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(text = unit.decimalPlaces.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f))
                    Text(
                        text = if (unit.active) stringResource(Res.string.unit_active) else stringResource(Res.string.unit_inactive),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unit.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(0.8f)
                    )
                    IconButton(onClick = { onDelete(unit.uid) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.unit_list_cd_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun UnitCard(
    unit: Unit,
    onClick: () -> kotlin.Unit,
    onDelete: () -> kotlin.Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = unit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        AssistChip(
                            onClick = {},
                            label = { Text(text = unit.shortName, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    Text(
                        text = stringResource(Res.string.unit_list_decimal_places_format, unit.decimalPlaces),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    unit.category?.let { category ->
                        Text(
                            text = stringResource(Res.string.unit_list_category_format, category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    unit.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.unit_list_cd_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = if (unit.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (unit.active) stringResource(Res.string.unit_active) else stringResource(Res.string.unit_inactive),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unit.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
