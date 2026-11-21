package com.ampairs.sfa.ui.visit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ampairsapp.feature.sfa.generated.resources.Res
import ampairsapp.feature.sfa.generated.resources.sfa_save
import ampairsapp.feature.sfa.generated.resources.sfa_visit_notes
import ampairsapp.feature.sfa.generated.resources.sfa_visit_outcome
import ampairsapp.feature.sfa.generated.resources.sfa_visit_title
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VisitCaptureScreen(
    plannedVisitUid: String?,
    customerUid: String,
    repMemberUid: String,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: VisitCaptureViewModel = assistedMetroViewModel<VisitCaptureViewModel, VisitCaptureViewModel.Factory> {
        create(plannedVisitUid, customerUid, repMemberUid)
    },
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.sfa_visit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.sfa_visit_outcome), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VISIT_OUTCOMES.forEach { outcome ->
                    FilterChip(
                        selected = state.outcome == outcome,
                        onClick = { viewModel.selectOutcome(outcome) },
                        label = { Text(outcome) },
                    )
                }
            }
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text(stringResource(Res.string.sfa_visit_notes)) },
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { viewModel.save(onSaveSuccess) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.sfa_save))
            }
        }
    }
}
