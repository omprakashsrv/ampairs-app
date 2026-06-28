package com.ampairs.communication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import ampairsapp.feature.communication.generated.resources.Res
import ampairsapp.feature.communication.generated.resources.comm_empty
import ampairsapp.feature.communication.generated.resources.comm_usage_billing_client
import ampairsapp.feature.communication.generated.resources.comm_usage_billing_platform
import ampairsapp.feature.communication.generated.resources.comm_usage_title
import ampairsapp.feature.communication.generated.resources.comm_usage_total
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.communication.data.api.CommunicationActionApi
import com.ampairs.communication.data.api.UsageReportResponse
import com.ampairs.communication.data.api.UsageRowDto
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class UsageViewModel(
    private val actionApi: CommunicationActionApi,
) : ViewModel() {
    private val _report = MutableStateFlow<UsageReportResponse?>(null)
    val report: StateFlow<UsageReportResponse?> = _report.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val resp = actionApi.usageReport(from = null, to = null)
            resp.data?.let { data -> _report.update { data } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    viewModel: UsageViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val report by viewModel.report.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.comm_usage_title)) }) },
    ) { padding ->
        val rows = report?.rows.orEmpty()
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.comm_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(Res.string.comm_usage_total, report?.totalMessages ?: 0L),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(rows) { row -> UsageRow(row) }
            }
        }
    }
}

@Composable
private fun UsageRow(row: UsageRowDto) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(row.channel, fontWeight = FontWeight.SemiBold)
                Text(
                    if (row.billingMode == "PLATFORM") stringResource(Res.string.comm_usage_billing_platform)
                    else stringResource(Res.string.comm_usage_billing_client),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${row.messageCount}", style = MaterialTheme.typography.titleMedium)
        }
    }
}
