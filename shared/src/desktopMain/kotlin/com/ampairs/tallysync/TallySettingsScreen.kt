package com.ampairs.tallysync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.common.config.AppPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TallySettingsScreen(
    workspaceSlug: String,
    scheduler: TallySyncScheduler,
    dataStore: AppPreferencesDataStore,
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9008") }
    var statusText by remember { mutableStateOf("Not synced yet") }
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(workspaceSlug) {
        host = dataStore.getTallyHost(workspaceSlug).first()
        val savedPort = dataStore.getTallyPort(workspaceSlug).first()
        port = savedPort.toString()
        scheduler.lastResult?.let { statusText = formatResult(it) }
    }

    Surface(modifier = Modifier.padding(24.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tally ERP Sync Settings", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Tally Host") },
                placeholder = { Text("e.g. 192.168.1.17") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Tally Port") },
                placeholder = { Text("9008") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        scope.launch {
                            dataStore.setTallyHost(workspaceSlug, host.trim())
                            dataStore.setTallyPort(workspaceSlug, port.toIntOrNull() ?: 9008)
                        }
                    }
                ) {
                    Text("Save")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    enabled = !isSyncing && host.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            statusText = "Syncing…"
                            val result = runCatching {
                                scheduler.syncService.sync(workspaceSlug)
                            }.onFailure { statusText = "Error: ${it.message}" }
                                .getOrNull()
                            if (result != null) statusText = formatResult(result)
                            isSyncing = false
                        }
                    }
                ) {
                    Text(if (isSyncing) "Syncing…" else "Sync Now")
                }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    enabled = !isSyncing,
                    onClick = {
                        scope.launch {
                            val entityTypes = listOf(
                                TallyProductMapper.ENTITY_STOCK_GROUP,
                                TallyProductMapper.ENTITY_STOCK_CATEGORY,
                                TallyProductMapper.ENTITY_STOCK_ITEM,
                                TallyProductMapper.ENTITY_UNIT,
                                TallyCustomerMapper.ENTITY_ACCOUNT_GROUP,
                                TallyCustomerMapper.ENTITY_LEDGER,
                            )
                            entityTypes.forEach { entity ->
                                dataStore.setTallyLastAlterId(workspaceSlug, entity, 0L)
                            }
                            statusText = "Sync reset — next sync will re-fetch all records"
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset Sync")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Status: $statusText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatResult(result: TallySyncResult): String =
    if (result.success)
        "OK — groups=${result.groupsSynced} categories=${result.categoriesSynced} products=${result.productsSynced} units=${result.unitsSynced}"
    else
        "Error: ${result.error}"
