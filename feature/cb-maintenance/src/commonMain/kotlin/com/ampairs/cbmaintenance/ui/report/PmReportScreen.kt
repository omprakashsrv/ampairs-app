package com.ampairs.cbmaintenance.ui.report

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel

private val MONTHS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
private val OUTLET_W = 150.dp
private val CITY_W = 56.dp
private val ASSET_W = 130.dp
private val TASK_W = 190.dp
private val FREQ_W = 76.dp
private val PERSON_W = 130.dp
private val MONTH_W = 26.dp

@Composable
fun PmReportScreen(
    viewModel: PmReportViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "PM compliance ${uiState.year}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onSearch,
                    label = { Text("Search outlet / asset / task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Header (scrolls horizontally in lockstep with the rows).
        Row(modifier = Modifier.horizontalScroll(hScroll).padding(horizontal = 12.dp, vertical = 6.dp)) {
            HeaderCell("Outlet", OUTLET_W)
            HeaderCell("City", CITY_W)
            HeaderCell("Asset", ASSET_W)
            HeaderCell("Task", TASK_W)
            HeaderCell("Freq", FREQ_W)
            HeaderCell("Done by", PERSON_W)
            HeaderCell("Assisted by", PERSON_W)
            MONTHS.forEach { HeaderCell(it, MONTH_W, TextAlign.Center) }
        }
        HorizontalDivider()

        when {
            uiState.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            uiState.filteredRows.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No completed PM work this year", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(uiState.filteredRows) { row ->
                    Row(modifier = Modifier.horizontalScroll(hScroll).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        BodyCell(row.outlet, OUTLET_W)
                        BodyCell(row.city, CITY_W)
                        BodyCell(row.asset, ASSET_W)
                        BodyCell(row.task, TASK_W)
                        BodyCell(row.freq, FREQ_W)
                        BodyCell(row.doneBy, PERSON_W)
                        BodyCell(row.assistedBy, PERSON_W)
                        row.months.forEach { done ->
                            Text(
                                if (done) "✓" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(MONTH_W),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp, align: TextAlign = TextAlign.Start) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        textAlign = align,
        modifier = Modifier.width(width),
    )
}

@Composable
private fun BodyCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        modifier = Modifier.width(width),
    )
}
