package com.ampairs.communication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import ampairsapp.feature.communication.generated.resources.comm_credential_channel
import ampairsapp.feature.communication.generated.resources.comm_credential_display
import ampairsapp.feature.communication.generated.resources.comm_credential_fallback
import ampairsapp.feature.communication.generated.resources.comm_credential_provider
import ampairsapp.feature.communication.generated.resources.comm_credential_secret
import ampairsapp.feature.communication.generated.resources.comm_credential_secret_hint
import ampairsapp.feature.communication.generated.resources.comm_credential_sender
import ampairsapp.feature.communication.generated.resources.comm_credential_status
import ampairsapp.feature.communication.generated.resources.comm_credential_validate
import ampairsapp.feature.communication.generated.resources.comm_credentials_title
import ampairsapp.feature.communication.generated.resources.comm_delete
import ampairsapp.feature.communication.generated.resources.comm_save
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.communication.data.api.CommunicationActionApi
import com.ampairs.communication.data.api.CredentialRequest
import com.ampairs.communication.data.api.CredentialResponse
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

data class CredentialUiState(
    val credentials: List<CredentialResponse> = emptyList(),
    val channel: String = "WHATSAPP",
    val provider: String = "",
    val senderRef: String = "",
    val displayName: String = "",
    val secret: String = "",
    val allowFallback: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class CredentialSettingsViewModel(
    private val actionApi: CommunicationActionApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CredentialUiState())
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val resp = actionApi.listCredentials()
            resp.data?.let { list -> _uiState.update { it.copy(credentials = list) } }
        }
    }

    fun onChannel(v: String) = _uiState.update { it.copy(channel = v) }
    fun onProvider(v: String) = _uiState.update { it.copy(provider = v) }
    fun onSenderRef(v: String) = _uiState.update { it.copy(senderRef = v) }
    fun onDisplayName(v: String) = _uiState.update { it.copy(displayName = v) }
    fun onSecret(v: String) = _uiState.update { it.copy(secret = v) }
    fun onAllowFallback(v: Boolean) = _uiState.update { it.copy(allowFallback = v) }

    fun save() {
        val state = _uiState.value
        if (state.provider.isBlank() || state.senderRef.isBlank() || state.secret.isBlank()) {
            _uiState.update { it.copy(error = "Provider, sender and secret are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val resp = actionApi.createCredential(
                CredentialRequest(
                    channel = state.channel,
                    provider = state.provider.trim(),
                    senderRef = state.senderRef.trim(),
                    displayName = state.displayName.takeIf { it.isNotBlank() },
                    secret = state.secret,
                    allowPlatformFallback = state.allowFallback,
                ),
            )
            if (resp.error == null) {
                _uiState.update { it.copy(saving = false, secret = "", provider = "", senderRef = "", displayName = "") }
                refresh()
            } else {
                _uiState.update { it.copy(saving = false, error = resp.error?.message ?: "Save failed") }
            }
        }
    }

    fun validate(uid: String) {
        viewModelScope.launch {
            actionApi.validateCredential(uid)
            refresh()
        }
    }

    fun delete(uid: String) {
        viewModelScope.launch {
            actionApi.deleteCredential(uid)
            refresh()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialSettingsScreen(
    viewModel: CredentialSettingsViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.comm_credentials_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.credentials.forEach { credential ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("${credential.provider} · ${credential.channel}", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${credential.senderRef}  ••${credential.secretLast4 ?: "----"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${stringResource(Res.string.comm_credential_status)}: ${credential.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.validate(credential.uid) }) {
                                Text(stringResource(Res.string.comm_credential_validate))
                            }
                            TextButton(onClick = { viewModel.delete(credential.uid) }) {
                                Text(stringResource(Res.string.comm_delete))
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = state.channel, onValueChange = viewModel::onChannel, singleLine = true,
                label = { Text(stringResource(Res.string.comm_credential_channel)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.provider, onValueChange = viewModel::onProvider, singleLine = true,
                label = { Text(stringResource(Res.string.comm_credential_provider)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.senderRef, onValueChange = viewModel::onSenderRef, singleLine = true,
                label = { Text(stringResource(Res.string.comm_credential_sender)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.displayName, onValueChange = viewModel::onDisplayName, singleLine = true,
                label = { Text(stringResource(Res.string.comm_credential_display)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.secret, onValueChange = viewModel::onSecret, singleLine = true,
                label = { Text(stringResource(Res.string.comm_credential_secret)) },
                supportingText = { Text(stringResource(Res.string.comm_credential_secret_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.comm_credential_fallback))
                Switch(checked = state.allowFallback, onCheckedChange = viewModel::onAllowFallback)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.comm_save))
            }
        }
    }
}
