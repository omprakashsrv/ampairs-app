package com.ampairs.printing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.printing.generated.resources.Res
import ampairsapp.feature.printing.generated.resources.printing_add_printer
import ampairsapp.feature.printing.generated.resources.printing_address
import ampairsapp.feature.printing.generated.resources.printing_cancel
import ampairsapp.feature.printing.generated.resources.printing_delete
import ampairsapp.feature.printing.generated.resources.printing_name
import ampairsapp.feature.printing.generated.resources.printing_no_printers
import ampairsapp.feature.printing.generated.resources.printing_paper_58
import ampairsapp.feature.printing.generated.resources.printing_paper_80
import ampairsapp.feature.printing.generated.resources.printing_paper_width
import ampairsapp.feature.printing.generated.resources.printing_printers_title
import ampairsapp.feature.printing.generated.resources.printing_save
import ampairsapp.feature.printing.generated.resources.printing_set_default
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterListScreen(
    viewModel: PrinterListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.printing_printers_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.printing_add_printer))
            }
        },
    ) { padding ->
        if (state.printers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.printing_no_printers), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.printers, key = { it.id }) { printer ->
                    PrinterRow(
                        name = printer.name,
                        subtitle = "${printer.connectionType.name} · ${printer.address ?: ""}",
                        onSetDefault = { viewModel.setAsDefault(printer.id) },
                        onDelete = { viewModel.delete(printer.id) },
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddPrinterDialog(
            onDismiss = { showAdd = false },
            onSave = { name, address, width ->
                viewModel.addNetworkPrinter(name, address, width)
                showAdd = false
            },
        )
    }
}

@Composable
private fun PrinterRow(
    name: String,
    subtitle: String,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSetDefault) {
                Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.printing_set_default))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.printing_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPrinterDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, paperWidthMm: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var width by remember { mutableStateOf(80) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.printing_add_printer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.printing_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(Res.string.printing_address)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(Res.string.printing_paper_width), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = width == 80,
                        onClick = { width = 80 },
                        label = { Text(stringResource(Res.string.printing_paper_80)) },
                    )
                    FilterChip(
                        selected = width == 58,
                        onClick = { width = 58 },
                        label = { Text(stringResource(Res.string.printing_paper_58)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, address, width) }) {
                Text(stringResource(Res.string.printing_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.printing_cancel)) }
        },
    )
}
