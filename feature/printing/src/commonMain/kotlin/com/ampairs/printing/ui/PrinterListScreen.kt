package com.ampairs.printing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import ampairsapp.feature.printing.generated.resources.printing_discover
import ampairsapp.feature.printing.generated.resources.printing_discover_printers
import ampairsapp.feature.printing.generated.resources.printing_discovered_title
import ampairsapp.feature.printing.generated.resources.printing_discovering
import ampairsapp.feature.printing.generated.resources.printing_label_height
import ampairsapp.feature.printing.generated.resources.printing_label_width
import ampairsapp.feature.printing.generated.resources.printing_name
import ampairsapp.feature.printing.generated.resources.printing_no_printers
import ampairsapp.feature.printing.generated.resources.printing_no_printers_body
import ampairsapp.feature.printing.generated.resources.printing_ok
import ampairsapp.feature.printing.generated.resources.printing_open
import ampairsapp.feature.printing.generated.resources.printing_page_note
import ampairsapp.feature.printing.generated.resources.printing_paper_58
import ampairsapp.feature.printing.generated.resources.printing_paper_80
import ampairsapp.feature.printing.generated.resources.printing_paper_width
import ampairsapp.feature.printing.generated.resources.printing_printers_subtitle
import ampairsapp.feature.printing.generated.resources.printing_printers_title
import ampairsapp.feature.printing.generated.resources.printing_queue
import ampairsapp.feature.printing.generated.resources.printing_save
import ampairsapp.feature.printing.generated.resources.printing_setup_guide
import ampairsapp.feature.printing.generated.resources.printing_templates
import ampairsapp.feature.printing.generated.resources.printing_type
import ampairsapp.feature.printing.generated.resources.printing_type_label
import ampairsapp.feature.printing.generated.resources.printing_type_page
import ampairsapp.feature.printing.generated.resources.printing_type_thermal
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.transport.DiscoveredPrinter
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterListScreen(
    onOpenQueue: () -> Unit = {},
    onOpenTemplates: () -> Unit = {},
    onOpenPrinter: (String) -> Unit = {},
    viewModel: PrinterListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showDiscover by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.printing_printers_title)) },
                actions = {
                    TextButton(onClick = onOpenTemplates) { Text(stringResource(Res.string.printing_templates)) }
                    TextButton(onClick = onOpenQueue) { Text(stringResource(Res.string.printing_queue)) }
                    IconButton(onClick = {
                        showDiscover = true
                        viewModel.discover()
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(Res.string.printing_discover))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.printers.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.printing_add_printer)) },
                )
            }
        },
    ) { padding ->
        if (state.printers.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.PrintDisabled,
                title = stringResource(Res.string.printing_no_printers),
                body = stringResource(Res.string.printing_no_printers_body),
                iconContainer = MaterialTheme.colorScheme.primaryContainer,
                iconContent = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = {
                    showDiscover = true
                    viewModel.discover()
                }) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.printing_discover_printers))
                }
                TextButton(onClick = { showAdd = true }) {
                    Text(stringResource(Res.string.printing_add_printer))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                item {
                    Text(
                        stringResource(Res.string.printing_printers_subtitle, state.printers.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 8.dp),
                    )
                }
                items(state.printers, key = { it.id }) { printer ->
                    PrinterRow(printer = printer, onClick = { onOpenPrinter(printer.id) })
                }
            }
        }
    }

    if (showAdd) {
        AddPrinterDialog(
            onDismiss = { showAdd = false },
            onScan = {
                showAdd = false
                showDiscover = true
                viewModel.discover()
            },
            onSave = { name, printerClass, address, widthMm, labelW, labelH ->
                viewModel.addPrinter(name, printerClass, address, widthMm, labelW, labelH)
                showAdd = false
            },
        )
    }

    if (showDiscover) {
        DiscoverSheet(
            discovering = state.discovering,
            discovered = state.discovered,
            onConnect = { viewModel.addDiscoveredPrinter(it) },
            onDismiss = {
                showDiscover = false
                viewModel.clearDiscovered()
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
}

@Composable
private fun PrinterRow(printer: PrinterProfile, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            icon = printer.connectionType.icon,
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(printer.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(
                "${printer.connectionType.label} · ${printer.address ?: printer.printerClass.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                PillChip(
                    label = printer.connectionType.label,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = printer.connectionType.icon,
                )
                val cls = printer.printerClass.chipColors
                PillChip(printer.printerClass.label, cls.container, cls.content)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(Res.string.printing_open),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverSheet(
    discovering: Boolean,
    discovered: List<DiscoveredPrinter>,
    onConnect: (DiscoveredPrinter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(Res.string.printing_discovered_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when {
                discovering -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp))
                    Text(stringResource(Res.string.printing_discovering), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                discovered.isEmpty() -> Text(
                    stringResource(Res.string.printing_setup_guide),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp),
                )

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(discovered, key = { it.id }) { device ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                        ) {
                            IconTile(
                                icon = device.connectionType.icon,
                                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                content = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 44,
                                cornerRadius = 12,
                                iconSize = 22,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${device.connectionType.label} · ${device.address ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(onClick = { onConnect(device) }) {
                                Text(stringResource(Res.string.printing_add))
                            }
                        }
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                        ) {
                            IconTile(
                                icon = Icons.Filled.DesktopWindows,
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                content = MaterialTheme.colorScheme.onSecondaryContainer,
                                size = 44,
                                cornerRadius = 12,
                                iconSize = 22,
                            )
                            Text(
                                stringResource(Res.string.printing_page_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
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
