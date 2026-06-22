package com.ampairs.printing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import ampairsapp.feature.printing.generated.resources.printing_default
import ampairsapp.feature.printing.generated.resources.printing_import_html_template
import ampairsapp.feature.printing.generated.resources.printing_no_templates
import ampairsapp.feature.printing.generated.resources.printing_no_templates_body
import ampairsapp.feature.printing.generated.resources.printing_restore_defaults
import ampairsapp.feature.printing.generated.resources.printing_set_default
import ampairsapp.feature.printing.generated.resources.printing_template_blocks
import ampairsapp.feature.printing.generated.resources.printing_template_html
import ampairsapp.feature.printing.generated.resources.printing_templates_title
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateKind
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onEditTemplate: (String) -> Unit = {},
    viewModel: TemplateListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.printing_templates_title)) },
                actions = {
                    var importMenu by remember { mutableStateOf(false) }
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { importMenu = true }) {
                            Icon(Icons.Filled.UploadFile, contentDescription = stringResource(Res.string.printing_import_html_template))
                        }
                        DropdownMenu(expanded = importMenu, onDismissRequest = { importMenu = false }) {
                            listOf(DocumentType.INVOICE, DocumentType.ORDER, DocumentType.RECEIPT).forEach { dt ->
                                DropdownMenuItem(
                                    text = { Text("${dt.displayName}") },
                                    leadingIcon = { Icon(dt.icon, contentDescription = null) },
                                    onClick = {
                                        importMenu = false
                                        viewModel.importStaticTemplate(dt)
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.restoreDefaults() }) {
                        Icon(Icons.Filled.SettingsBackupRestore, contentDescription = stringResource(Res.string.printing_restore_defaults))
                    }
                },
            )
        },
    ) { padding ->
        if (state.templates.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Description,
                title = stringResource(Res.string.printing_no_templates),
                body = stringResource(Res.string.printing_no_templates_body),
                iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                iconContent = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = { viewModel.restoreDefaults() }) {
                    Icon(Icons.Filled.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.printing_restore_defaults))
                }
            }
        } else {
            val groups = remember(state.templates) {
                state.templates.groupBy { it.documentType }
                    .toList()
                    .sortedBy { (type, _) -> type.ordinal }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
            ) {
                groups.forEach { (type, templates) ->
                    item(key = "header-${type.name}") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Text(
                                type.displayName.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onOpen = { onEditTemplate(template.id) },
                            onSetDefault = { viewModel.setDefault(template.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    onOpen: () -> Unit,
    onSetDefault: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(
                    icon = if (template.printerClass == PrinterClass.THERMAL) Icons.Filled.Receipt else Icons.Filled.Description,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 42,
                    cornerRadius = 11,
                    iconSize = 22,
                )
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            template.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                        )
                        if (template.isDefault) {
                            PillChip(
                                label = stringResource(Res.string.printing_default),
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                                icon = Icons.Filled.Check,
                            )
                        }
                    }
                    Row(
                        Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val cls = template.printerClass.chipColors
                        PillChip(template.printerClass.label, cls.container, cls.content)
                        Text(
                            if (template.kind == TemplateKind.STATIC) {
                                stringResource(Res.string.printing_template_html)
                            } else {
                                stringResource(Res.string.printing_template_blocks)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!template.isDefault) {
                HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSetDefault) {
                        Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                        Text(stringResource(Res.string.printing_set_default))
                    }
                }
            }
        }
    }
}
