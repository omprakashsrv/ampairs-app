package com.ampairs.tallysync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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

    val logLines by scheduler.syncService.logLines.collectAsState()
    val taxCodeCandidates by scheduler.syncService.taxCodeCandidates.collectAsState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(workspaceSlug) {
        host = dataStore.getTallyHost(workspaceSlug).first()
        val savedPort = dataStore.getTallyPort(workspaceSlug).first()
        port = savedPort.toString()
        scheduler.lastResult?.let { statusText = formatResult(it) }
    }

    Surface(modifier = Modifier.padding(24.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
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
                                scheduler.runOnce(workspaceSlug)
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

            // ----- Sync log -----
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync Log", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                TextButton(
                    enabled = logLines.isNotEmpty(),
                    onClick = { clipboard.setText(AnnotatedString(logLines.joinToString("\n"))) }
                ) { Text("Copy") }
                TextButton(onClick = { scheduler.syncService.clearLog() }) { Text("Clear") }
            }

            val logState = rememberLazyListState()
            LaunchedEffect(logLines.size) {
                if (logLines.isNotEmpty()) logState.scrollToItem(logLines.size - 1)
            }
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(240.dp)
            ) {
                if (logLines.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(12.dp)) {
                        Text(
                            text = "No sync activity yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    SelectionContainer {
                        LazyColumn(
                            state = logState,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        ) {
                            items(logLines) { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ----- Tax codes to import -----
            Spacer(Modifier.height(8.dp))
            val notSubscribed = taxCodeCandidates.count { !it.alreadySubscribed }
            Text("Tax Codes to Import", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "$notSubscribed not subscribed · ${taxCodeCandidates.size} HSN code(s) found in Tally",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (taxCodeCandidates.isNotEmpty()) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(taxCodeCandidates) { candidate ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = candidate.hsnCode,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = Modifier.width(120.dp)
                                )
                                Text(
                                    text = "${candidate.productCount} product(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (candidate.alreadySubscribed) "Subscribed" else "Not subscribed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (candidate.alreadySubscribed)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatResult(result: TallySyncResult): String =
    if (result.success)
        "OK — groups=${result.groupsSynced} categories=${result.categoriesSynced} products=${result.productsSynced} units=${result.unitsSynced} · ${result.taxCodesToImport} tax code(s) to import"
    else
        "Error: ${result.error}"
