package com.ampairs.connector.ui.mapping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.connector.generated.resources.Res
import ampairsapp.feature.connector.generated.resources.connector_mapping_add_rule
import ampairsapp.feature.connector.generated.resources.connector_mapping_ampairs
import ampairsapp.feature.connector.generated.resources.connector_mapping_delete_rule
import ampairsapp.feature.connector.generated.resources.connector_mapping_external
import ampairsapp.feature.connector.generated.resources.connector_mapping_none
import ampairsapp.feature.connector.generated.resources.connector_mapping_title
import ampairsapp.feature.connector.generated.resources.connector_mapping_transform
import ampairsapp.feature.connector.generated.resources.connector_save
import ampairsapp.feature.connector.generated.resources.connector_saved

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectorMappingScreen(
    installationUid: String,
    modifier: Modifier = Modifier,
    viewModel: ConnectorMappingViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(installationUid) { viewModel.load(installationUid) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.connector_mapping_title)) }) },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.entities.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.connector_mapping_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                state.error?.let { err -> item { Text(err, color = MaterialTheme.colorScheme.error) } }

                items(state.entities, key = { it.entityType }) { entity ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            entity.entityType,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        entity.rules.forEachIndexed { index, rule ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = rule.externalField,
                                    onValueChange = { viewModel.onRuleChange(entity.entityType, index, rule.copy(externalField = it)) },
                                    label = { Text(stringResource(Res.string.connector_mapping_external)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = rule.ampairsField,
                                    onValueChange = { viewModel.onRuleChange(entity.entityType, index, rule.copy(ampairsField = it)) },
                                    label = { Text(stringResource(Res.string.connector_mapping_ampairs)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = rule.transform ?: "",
                                    onValueChange = {
                                        viewModel.onRuleChange(entity.entityType, index, rule.copy(transform = it.ifBlank { null }))
                                    },
                                    label = { Text(stringResource(Res.string.connector_mapping_transform)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TextButton(onClick = { viewModel.removeRule(entity.entityType, index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.connector_mapping_delete_rule))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(Res.string.connector_mapping_delete_rule))
                                }
                            }
                            HorizontalDivider()
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { viewModel.addRule(entity.entityType) }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.connector_mapping_add_rule))
                                Text(stringResource(Res.string.connector_mapping_add_rule))
                            }
                            Button(onClick = { viewModel.saveEntity(entity.entityType) }, enabled = !entity.saving) {
                                Text(stringResource(Res.string.connector_save))
                            }
                            if (entity.saved) Text(
                                stringResource(Res.string.connector_saved),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
    }
}
