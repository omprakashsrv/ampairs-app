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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.communication.generated.resources.Res
import ampairsapp.feature.communication.generated.resources.comm_campaign_pause
import ampairsapp.feature.communication.generated.resources.comm_campaign_progress
import ampairsapp.feature.communication.generated.resources.comm_campaign_resume
import ampairsapp.feature.communication.generated.resources.comm_campaign_start
import ampairsapp.feature.communication.generated.resources.comm_campaigns_title
import ampairsapp.feature.communication.generated.resources.comm_empty
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.communication.data.api.CampaignDto
import com.ampairs.communication.data.api.CommunicationActionApi
import com.ampairs.communication.data.repository.CampaignRepository
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

data class CampaignListUiState(
    val campaigns: List<CampaignDto> = emptyList(),
    val isRefreshing: Boolean = false,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class CampaignListViewModel(
    private val repository: CampaignRepository,
    private val actionApi: CommunicationActionApi,
    private val syncService: CentralSyncService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignListUiState())
    val uiState: StateFlow<CampaignListUiState> = _uiState.asStateFlow()

    init {
        repository.observeAll()
            .onEach { list -> _uiState.update { it.copy(campaigns = list) } }
            .launchIn(viewModelScope)
        syncService.observeEntity(SyncEntity.COMM_CAMPAIGN)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.COMM_CAMPAIGN))
    }

    fun start(uid: String) = act { actionApi.startCampaign(uid) }
    fun pause(uid: String) = act { actionApi.pauseCampaign(uid) }
    fun resume(uid: String) = act { actionApi.resumeCampaign(uid) }

    private fun act(block: suspend () -> com.ampairs.common.model.Response<CampaignDto>) {
        viewModelScope.launch {
            val resp = block()
            resp.data?.let { repository.applyServer(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignListScreen(
    viewModel: CampaignListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.comm_campaigns_title)) }) },
    ) { padding ->
        if (uiState.campaigns.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.comm_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.campaigns, key = { it.uid }) { campaign ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(campaign.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                AssistChip(onClick = {}, label = { Text(campaign.status) })
                            }
                            Text(
                                stringResource(Res.string.comm_campaign_progress, campaign.sentCount, campaign.deliveredCount, campaign.failedCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (campaign.status) {
                                    "RUNNING" -> TextButton(onClick = { viewModel.pause(campaign.uid) }) { Text(stringResource(Res.string.comm_campaign_pause)) }
                                    "PAUSED" -> TextButton(onClick = { viewModel.resume(campaign.uid) }) { Text(stringResource(Res.string.comm_campaign_resume)) }
                                    else -> TextButton(onClick = { viewModel.start(campaign.uid) }) { Text(stringResource(Res.string.comm_campaign_start)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
