package com.ampairs.communication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.communication.generated.resources.Res
import ampairsapp.feature.communication.generated.resources.comm_card_campaigns
import ampairsapp.feature.communication.generated.resources.comm_card_campaigns_desc
import ampairsapp.feature.communication.generated.resources.comm_card_credentials
import ampairsapp.feature.communication.generated.resources.comm_card_credentials_desc
import ampairsapp.feature.communication.generated.resources.comm_card_logs
import ampairsapp.feature.communication.generated.resources.comm_card_logs_desc
import ampairsapp.feature.communication.generated.resources.comm_card_schedules
import ampairsapp.feature.communication.generated.resources.comm_card_schedules_desc
import ampairsapp.feature.communication.generated.resources.comm_card_templates
import ampairsapp.feature.communication.generated.resources.comm_card_templates_desc
import ampairsapp.feature.communication.generated.resources.comm_card_usage
import ampairsapp.feature.communication.generated.resources.comm_card_usage_desc
import ampairsapp.feature.communication.generated.resources.comm_home_subtitle
import ampairsapp.feature.communication.generated.resources.comm_home_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationHomeScreen(
    onOpenTemplates: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenCampaigns: () -> Unit,
    onOpenCredentials: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.comm_home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.comm_home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                HomeCard(Icons.Default.Description, Res.string.comm_card_templates, Res.string.comm_card_templates_desc, onOpenTemplates)
            }
            item {
                HomeCard(Icons.Default.Schedule, Res.string.comm_card_schedules, Res.string.comm_card_schedules_desc, onOpenSchedules)
            }
            item {
                HomeCard(Icons.Default.Campaign, Res.string.comm_card_campaigns, Res.string.comm_card_campaigns_desc, onOpenCampaigns)
            }
            item {
                HomeCard(Icons.Default.Key, Res.string.comm_card_credentials, Res.string.comm_card_credentials_desc, onOpenCredentials)
            }
            item {
                HomeCard(Icons.Default.Receipt, Res.string.comm_card_usage, Res.string.comm_card_usage_desc, onOpenUsage)
            }
            item {
                HomeCard(Icons.AutoMirrored.Filled.Send, Res.string.comm_card_logs, Res.string.comm_card_logs_desc, onOpenLogs)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCard(
    icon: ImageVector,
    title: StringResource,
    desc: StringResource,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
