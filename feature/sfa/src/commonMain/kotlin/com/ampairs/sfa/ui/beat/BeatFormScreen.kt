package com.ampairs.sfa.ui.beat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ampairsapp.feature.sfa.generated.resources.Res
import ampairsapp.feature.sfa.generated.resources.sfa_beat_description
import ampairsapp.feature.sfa.generated.resources.sfa_beat_form_title_edit
import ampairsapp.feature.sfa.generated.resources.sfa_beat_form_title_new
import ampairsapp.feature.sfa.generated.resources.sfa_beat_name
import ampairsapp.feature.sfa.generated.resources.sfa_beat_rep
import ampairsapp.feature.sfa.generated.resources.sfa_beat_scheduled_days
import ampairsapp.feature.sfa.generated.resources.sfa_save
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatFormScreen(
    beatId: String?,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: BeatFormViewModel = assistedMetroViewModel<BeatFormViewModel, BeatFormViewModel.Factory> { create(beatId) },
    modifier: Modifier = Modifier,
) {
    val state by viewModel.formState.collectAsState()
    val title = if (beatId == null) stringResource(Res.string.sfa_beat_form_title_new)
    else stringResource(Res.string.sfa_beat_form_title_edit)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(Res.string.sfa_beat_name)) },
                singleLine = true,
                isError = state.error != null && state.name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                label = { Text(stringResource(Res.string.sfa_beat_description)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.repMemberUid,
                onValueChange = viewModel::updateRepMemberUid,
                label = { Text(stringResource(Res.string.sfa_beat_rep)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.scheduledDays,
                onValueChange = viewModel::updateScheduledDays,
                label = { Text(stringResource(Res.string.sfa_beat_scheduled_days)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let {
                Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { viewModel.save(onSaveSuccess) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.sfa_save), textAlign = TextAlign.Center)
            }
        }
    }
}
