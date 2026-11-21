package com.ampairs.connector.ui.config

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.connector.generated.resources.Res
import ampairsapp.feature.connector.generated.resources.connector_config_title
import ampairsapp.feature.connector.generated.resources.connector_no_config_fields
import ampairsapp.feature.connector.generated.resources.connector_required
import ampairsapp.feature.connector.generated.resources.connector_save
import ampairsapp.feature.connector.generated.resources.connector_saved
import ampairsapp.feature.connector.generated.resources.connector_secret_hint
import ampairsapp.feature.connector.generated.resources.connector_secret_set
import ampairsapp.feature.connector.generated.resources.connector_test_connection
import ampairsapp.feature.connector.generated.resources.connector_test_failed
import ampairsapp.feature.connector.generated.resources.connector_test_ok
import ampairsapp.feature.connector.generated.resources.connector_testing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectorConfigScreen(
    installationUid: String,
    connectorType: String,
    modifier: Modifier = Modifier,
    viewModel: ConnectorConfigViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(installationUid, connectorType) {
        viewModel.load(installationUid, connectorType)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.connector_config_title)) }) },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.fields.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.connector_no_config_fields), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.fields, key = { it.key }) { field ->
                    val support: String? = when {
                        field.secret && field.secretIsSet -> stringResource(Res.string.connector_secret_set)
                        field.secret -> stringResource(Res.string.connector_secret_hint)
                        field.required -> stringResource(Res.string.connector_required)
                        else -> null
                    }
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { viewModel.onFieldChange(field.key, it) },
                        label = { Text(field.label) },
                        singleLine = true,
                        visualTransformation = if (field.secret) PasswordVisualTransformation() else VisualTransformation.None,
                        supportingText = support?.let { msg -> { Text(msg) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.save() }, enabled = !state.saving) {
                            Text(stringResource(Res.string.connector_save))
                        }
                        OutlinedButton(onClick = { viewModel.testConnection() }, enabled = !state.testing) {
                            Text(stringResource(if (state.testing) Res.string.connector_testing else Res.string.connector_test_connection))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (state.saved) Text(stringResource(Res.string.connector_saved), color = MaterialTheme.colorScheme.primary)
                    state.testOk?.let { ok ->
                        val label = if (ok) stringResource(Res.string.connector_test_ok) else stringResource(Res.string.connector_test_failed)
                        val detail = state.testMessage?.let { m -> ": $m" } ?: ""
                        Text("$label$detail", color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
