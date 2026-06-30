package com.ampairs.sfa.ui.beat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.sfa.generated.resources.Res
import ampairsapp.feature.sfa.generated.resources.sfa_add_beat
import ampairsapp.feature.sfa.generated.resources.sfa_beats_empty
import ampairsapp.feature.sfa.generated.resources.sfa_beats_title
import com.ampairs.sfa.domain.model.Beat
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatListScreen(
    onBeatClick: (String) -> Unit,
    onAddBeat: () -> Unit,
    viewModel: BeatListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.sfa_beats_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBeat,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.sfa_add_beat)) },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                uiState.isRefreshing && uiState.beats.isEmpty() -> CircularProgressIndicator()
                uiState.beats.isEmpty() -> Text(
                    text = stringResource(Res.string.sfa_beats_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.beats, key = { it.uid }) { beat ->
                        BeatRow(beat = beat, onClick = { onBeatClick(beat.uid) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BeatRow(beat: Beat, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = beat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        beat.description?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
