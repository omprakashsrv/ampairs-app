package com.ampairs.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_model_cancel
import ampairsapp.feature.agent.generated.resources.agent_model_delete
import ampairsapp.feature.agent.generated.resources.agent_model_download
import ampairsapp.feature.agent.generated.resources.agent_model_retry
import ampairsapp.feature.agent.generated.resources.agent_model_status_downloading
import ampairsapp.feature.agent.generated.resources.agent_model_status_installed
import ampairsapp.feature.agent.generated.resources.agent_model_status_not_installed
import ampairsapp.feature.agent.generated.resources.agent_models_back_cd
import ampairsapp.feature.agent.generated.resources.agent_models_title
import com.ampairs.agent.llm.ModelInstallStatus
import com.ampairs.agent.llm.formatModelSize
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ModelManagerViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.agent_models_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.agent_models_back_cd),
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.rows, key = { it.model.id }) { row ->
                ModelCard(
                    name = row.model.displayName,
                    sizeLabel = formatModelSize(row.model.sizeBytes),
                    status = row.status,
                    onDownload = { viewModel.onDownload(row.model) },
                    onCancel = { viewModel.onCancel(row.model.id) },
                    onDelete = { viewModel.onDelete(row.model) },
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    name: String,
    sizeLabel: String,
    status: ModelInstallStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (status) {
                is ModelInstallStatus.Downloading -> {
                    Text(
                        text = stringResource(Res.string.agent_model_status_downloading),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (status.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { status.fraction },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = onCancel) { Text(stringResource(Res.string.agent_model_cancel)) }
                    }
                }

                is ModelInstallStatus.Installed -> {
                    StatusLine(stringResource(Res.string.agent_model_status_installed))
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        TextButton(onClick = onDelete) { Text(stringResource(Res.string.agent_model_delete)) }
                    }
                }

                is ModelInstallStatus.Failed -> {
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = onDownload) { Text(stringResource(Res.string.agent_model_retry)) }
                    }
                }

                ModelInstallStatus.NotInstalled -> {
                    StatusLine(stringResource(Res.string.agent_model_status_not_installed))
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = onDownload) { Text(stringResource(Res.string.agent_model_download)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
