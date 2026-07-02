package com.ampairs.inventory.ui.count

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_back
import ampairsapp.feature.inventory.generated.resources.inv_count_confirm
import ampairsapp.feature.inventory.generated.resources.inv_count_confirm_dialog_body
import ampairsapp.feature.inventory.generated.resources.inv_count_confirm_dialog_title
import ampairsapp.feature.inventory.generated.resources.inv_count_counted
import ampairsapp.feature.inventory.generated.resources.inv_count_no_variances
import ampairsapp.feature.inventory.generated.resources.inv_count_review
import ampairsapp.feature.inventory.generated.resources.inv_count_save_draft
import ampairsapp.feature.inventory.generated.resources.inv_count_subtitle
import ampairsapp.feature.inventory.generated.resources.inv_count_system
import ampairsapp.feature.inventory.generated.resources.inv_count_title
import ampairsapp.feature.inventory.generated.resources.inv_count_variance
import ampairsapp.feature.inventory.generated.resources.cancel
import com.ampairs.inventory.ui.components.InventoryEmptyState
import com.ampairs.inventory.ui.components.MonoFamily
import com.ampairs.inventory.ui.components.formatQty
import com.ampairs.inventory.ui.components.signedQty
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalCountScreen(
    onNavigateBack: () -> Unit,
    viewModel: PhysicalCountViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.posted.collect { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inv_count_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.inv_back))
                    }
                },
                actions = {
                    FilterChip(
                        selected = state.reviewMode,
                        onClick = { viewModel.toggleReview(!state.reviewMode) },
                        label = { Text(stringResource(Res.string.inv_count_review)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.inv_count_save_draft))
                    }
                    Button(
                        onClick = { showConfirm = true },
                        enabled = state.varianceRows.isNotEmpty() && !state.posting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.width(18.dp))
                        Text(stringResource(Res.string.inv_count_confirm), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                stringResource(Res.string.inv_count_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // column header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("", Modifier.weight(1f))
                HeaderLabel(stringResource(Res.string.inv_count_system))
                HeaderLabel(stringResource(Res.string.inv_count_counted))
                HeaderLabel(stringResource(Res.string.inv_count_variance))
            }
            HorizontalDivider()

            if (state.reviewMode && state.varianceRows.isEmpty()) {
                InventoryEmptyState(icon = Icons.Filled.FactCheck, title = stringResource(Res.string.inv_count_no_variances))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.visibleRows, key = { it.item.uid }) { row ->
                        CountRowView(row, onCount = { viewModel.setCount(row.item.uid, it) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(Res.string.inv_count_confirm_dialog_title)) },
            text = { Text(stringResource(Res.string.inv_count_confirm_dialog_body)) },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; viewModel.confirm() }) {
                    Text(stringResource(Res.string.inv_count_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun HeaderLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(70.dp),
    )
}

@Composable
private fun CountRowView(row: CountRow, onCount: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(row.item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
        Text(formatQty(row.system), Modifier.width(70.dp), style = MaterialTheme.typography.bodyMedium, fontFamily = MonoFamily)
        OutlinedTextField(
            value = row.countedText,
            onValueChange = onCount,
            singleLine = true,
            placeholder = { Text(formatQty(row.system)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp),
        )
        Text(
            if (row.hasVariance) signedQty(row.variance) else "—",
            Modifier.width(60.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = MonoFamily,
            color = when {
                row.variance > 0 -> MaterialTheme.colorScheme.primary
                row.variance < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
