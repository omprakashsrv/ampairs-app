package com.ampairs.printing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import ampairsapp.feature.printing.generated.resources.printing_add
import ampairsapp.feature.printing.generated.resources.printing_add_printer
import ampairsapp.feature.printing.generated.resources.printing_address
import ampairsapp.feature.printing.generated.resources.printing_cancel
import ampairsapp.feature.printing.generated.resources.printing_delete
import ampairsapp.feature.printing.generated.resources.printing_discover
import ampairsapp.feature.printing.generated.resources.printing_discovered_title
import ampairsapp.feature.printing.generated.resources.printing_discovering
import ampairsapp.feature.printing.generated.resources.printing_done
import ampairsapp.feature.printing.generated.resources.printing_name
import ampairsapp.feature.printing.generated.resources.printing_no_printers
import ampairsapp.feature.printing.generated.resources.printing_ok
import ampairsapp.feature.printing.generated.resources.printing_paper_58
import ampairsapp.feature.printing.generated.resources.printing_paper_80
import ampairsapp.feature.printing.generated.resources.printing_paper_width
import ampairsapp.feature.printing.generated.resources.printing_printers_title
import ampairsapp.feature.printing.generated.resources.printing_queue
import ampairsapp.feature.printing.generated.resources.printing_save
import ampairsapp.feature.printing.generated.resources.printing_set_default
import ampairsapp.feature.printing.generated.resources.printing_setup_guide
import ampairsapp.feature.printing.generated.resources.printing_test
import ampairsapp.feature.printing.generated.resources.printing_type
import ampairsapp.feature.printing.generated.resources.printing_type_label
import ampairsapp.feature.printing.generated.resources.printing_type_page
import ampairsapp.feature.printing.generated.resources.printing_type_thermal
import ampairsapp.feature.printing.generated.resources.printing_label_height
import ampairsapp.feature.printing.generated.resources.printing_label_width
import ampairsapp.feature.printing.generated.resources.printing_page_note
import com.ampairs.printing.core.model.PrinterClass
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterListScreen(
    onOpenQueue: () -> Unit = {},
    viewModel: PrinterListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.printing_printers_title)) },
                actions = {
                    TextButton(onClick = onOpenQueue) { Text(stringResource(Res.string.printing_queue)) }
                    IconButton(onClick = { viewModel.discover() }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.printing_discover))
                    }
                },
            )
        },
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
                        subtitle = "${printer.printerClass.name} · ${printer.connectionType.name} · ${printer.address ?: ""}",
                        onTestPrint = { viewModel.testPrint(printer.id) },
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
            onScan = {
                showAdd = false
                viewModel.discover()
            },
            onSave = { name, printerClass, address, widthMm, labelW, labelH ->
                viewModel.addPrinter(name, printerClass, address, widthMm, labelW, labelH)
                showAdd = false
            },
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) { Text(stringResource(Res.string.printing_ok)) }
            },
            text = { Text(message) },
        )
    }

    if (state.discovering || state.discovered.isNotEmpty()) {
        DiscoveredPrintersDialog(
            discovering = state.discovering,
            discovered = state.discovered.map { Triple(it.id, it.name, "${it.connectionType.name} · ${it.address ?: ""}") },
            onAdd = { id -> state.discovered.firstOrNull { it.id == id }?.let { viewModel.addDiscoveredPrinter(it) } },
            onDismiss = { viewModel.clearDiscovered() },
        )
    }
}

@Composable
private fun DiscoveredPrintersDialog(
    discovering: Boolean,
    discovered: List<Triple<String, String, String>>,
    onAdd: (id: String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.printing_discovered_title)) },
        text = {
            when {
                discovering -> Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(Res.string.printing_discovering))
                }

                discovered.isEmpty() -> Column(
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        stringResource(Res.string.printing_setup_guide),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(discovered, key = { it.first }) { (id, name, subtitle) ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { onAdd(id) }) {
                                    Text(stringResource(Res.string.printing_add))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.printing_done)) }
        },
    )
}

@Composable
private fun PrinterRow(
    name: String,
    subtitle: String,
    onTestPrint: () -> Unit,
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
            IconButton(onClick = onTestPrint) {
                Icon(Icons.Default.Print, contentDescription = stringResource(Res.string.printing_test))
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
    onScan: () -> Unit,
    onSave: (name: String, printerClass: PrinterClass, address: String, paperWidthMm: Int, labelWidthMm: Double, labelHeightMm: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var printerClass by remember { mutableStateOf(PrinterClass.THERMAL) }
    var address by remember { mutableStateOf("") }
    var width by remember { mutableStateOf(80) }
    var labelW by remember { mutableStateOf("50") }
    var labelH by remember { mutableStateOf("25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.printing_add_printer)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.printing_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(Res.string.printing_type), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(printerClass == PrinterClass.THERMAL, { printerClass = PrinterClass.THERMAL }, { Text(stringResource(Res.string.printing_type_thermal)) })
                    FilterChip(printerClass == PrinterClass.PAGE, { printerClass = PrinterClass.PAGE }, { Text(stringResource(Res.string.printing_type_page)) })
                    FilterChip(printerClass == PrinterClass.LABEL, { printerClass = PrinterClass.LABEL }, { Text(stringResource(Res.string.printing_type_label)) })
                }

                when (printerClass) {
                    PrinterClass.PAGE -> Text(
                        stringResource(Res.string.printing_page_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(stringResource(Res.string.printing_address)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (printerClass == PrinterClass.THERMAL) {
                            Text(stringResource(Res.string.printing_paper_width), style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(width == 80, { width = 80 }, { Text(stringResource(Res.string.printing_paper_80)) })
                                FilterChip(width == 58, { width = 58 }, { Text(stringResource(Res.string.printing_paper_58)) })
                            }
                        } else {
                            OutlinedTextField(
                                value = labelW,
                                onValueChange = { labelW = it },
                                label = { Text(stringResource(Res.string.printing_label_width)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = labelH,
                                onValueChange = { labelH = it },
                                label = { Text(stringResource(Res.string.printing_label_height)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (printerClass == PrinterClass.PAGE) {
                TextButton(onClick = onScan) { Text(stringResource(Res.string.printing_discover)) }
            } else {
                TextButton(onClick = {
                    onSave(name, printerClass, address, width, labelW.toDoubleOrNull() ?: 50.0, labelH.toDoubleOrNull() ?: 25.0)
                }) { Text(stringResource(Res.string.printing_save)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.printing_cancel)) }
        },
    )
}
