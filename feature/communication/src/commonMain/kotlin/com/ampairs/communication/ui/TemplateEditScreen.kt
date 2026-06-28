package com.ampairs.communication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.communication.generated.resources.Res
import ampairsapp.feature.communication.generated.resources.comm_back
import ampairsapp.feature.communication.generated.resources.comm_delete
import ampairsapp.feature.communication.generated.resources.comm_save
import ampairsapp.feature.communication.generated.resources.comm_template_add_variant
import ampairsapp.feature.communication.generated.resources.comm_template_category
import ampairsapp.feature.communication.generated.resources.comm_template_channel
import ampairsapp.feature.communication.generated.resources.comm_template_code
import ampairsapp.feature.communication.generated.resources.comm_template_description
import ampairsapp.feature.communication.generated.resources.comm_template_html
import ampairsapp.feature.communication.generated.resources.comm_template_locale
import ampairsapp.feature.communication.generated.resources.comm_template_missing_vars
import ampairsapp.feature.communication.generated.resources.comm_template_name
import ampairsapp.feature.communication.generated.resources.comm_template_new
import ampairsapp.feature.communication.generated.resources.comm_template_preview
import ampairsapp.feature.communication.generated.resources.comm_template_preview_title
import ampairsapp.feature.communication.generated.resources.comm_template_subject
import ampairsapp.feature.communication.generated.resources.comm_template_text
import ampairsapp.feature.communication.generated.resources.comm_template_variant
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    templateUid: String,
    onBack: () -> Unit,
    viewModel: TemplateEditViewModel = assistedMetroViewModel<TemplateEditViewModel, TemplateEditViewModel.Factory>(
        key = templateUid,
    ) { create(templateUid) },
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.comm_template_new)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.comm_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            CircularProgressIndicator(Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.code, onValueChange = viewModel::onCode, singleLine = true,
                label = { Text(stringResource(Res.string.comm_template_code)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onName, singleLine = true,
                label = { Text(stringResource(Res.string.comm_template_name)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.category, onValueChange = viewModel::onCategory, singleLine = true,
                label = { Text(stringResource(Res.string.comm_template_category)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.defaultLocale, onValueChange = viewModel::onLocale, singleLine = true,
                label = { Text(stringResource(Res.string.comm_template_locale)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description, onValueChange = viewModel::onDescription,
                label = { Text(stringResource(Res.string.comm_template_description)) }, modifier = Modifier.fillMaxWidth(),
            )

            state.variants.forEachIndexed { index, variant ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${stringResource(Res.string.comm_template_variant)} ${index + 1}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = { viewModel.removeVariant(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.comm_delete))
                            }
                        }
                        OutlinedTextField(
                            value = variant.channel, onValueChange = { viewModel.updateVariant(index, variant.copy(channel = it)) },
                            singleLine = true, label = { Text(stringResource(Res.string.comm_template_channel)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = variant.subject.orEmpty(),
                            onValueChange = { viewModel.updateVariant(index, variant.copy(subject = it)) },
                            singleLine = true, label = { Text(stringResource(Res.string.comm_template_subject)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = variant.htmlBody.orEmpty(),
                            onValueChange = { viewModel.updateVariant(index, variant.copy(htmlBody = it)) },
                            label = { Text(stringResource(Res.string.comm_template_html)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = variant.textBody.orEmpty(),
                            onValueChange = { viewModel.updateVariant(index, variant.copy(textBody = it)) },
                            label = { Text(stringResource(Res.string.comm_template_text)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            OutlinedButton(onClick = viewModel::addVariant, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.comm_template_add_variant))
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::preview, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.comm_template_preview))
                }
                Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.comm_save))
                }
            }

            if (state.previewHtml != null || state.previewText != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(Res.string.comm_template_preview_title), fontWeight = FontWeight.SemiBold)
                        state.previewSubject?.let { Text(it, fontWeight = FontWeight.Medium) }
                        Text(state.previewHtml ?: state.previewText.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        if (state.missingVariables.isNotEmpty()) {
                            Text(
                                "${stringResource(Res.string.comm_template_missing_vars)}: ${state.missingVariables.joinToString()}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
