package com.ampairs.printing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import ampairsapp.feature.printing.generated.resources.printing_restore_defaults
import ampairsapp.feature.printing.generated.resources.printing_set_default
import ampairsapp.feature.printing.generated.resources.printing_templates_title
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import com.ampairs.printing.core.model.DocumentType
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
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.printing_import_html_template))
                        }
                        DropdownMenu(expanded = importMenu, onDismissRequest = { importMenu = false }) {
                            listOf(DocumentType.INVOICE, DocumentType.ORDER, DocumentType.RECEIPT).forEach { dt ->
                                DropdownMenuItem(
                                    text = { Text("${stringResource(Res.string.printing_import_html_template)} · ${dt.name}") },
                                    onClick = {
                                        importMenu = false
                                        viewModel.importStaticTemplate(dt)
                                    },
                                )
                            }
                        }
                    }
                    TextButton(onClick = { viewModel.restoreDefaults() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(stringResource(Res.string.printing_restore_defaults))
                    }
                },
            )
        },
    ) { padding ->
        if (state.templates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.printing_no_templates), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.templates, key = { it.id }) { template ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onEditTemplate(template.id) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${template.documentType.name} · ${template.printerClass.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (template.isDefault) {
                                AssistChip(onClick = {}, label = { Text(stringResource(Res.string.printing_default)) })
                            } else {
                                TextButton(onClick = { viewModel.setDefault(template.id) }) {
                                    Text(stringResource(Res.string.printing_set_default))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
