package com.ampairs.agent.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_back_cd
import ampairsapp.feature.agent.generated.resources.agent_cancel
import ampairsapp.feature.agent.generated.resources.agent_clear_chat_cd
import ampairsapp.feature.agent.generated.resources.agent_confirm
import ampairsapp.feature.agent.generated.resources.agent_confirm_prompt
import ampairsapp.feature.agent.generated.resources.agent_empty_hint
import ampairsapp.feature.agent.generated.resources.agent_empty_title
import ampairsapp.feature.agent.generated.resources.agent_input_placeholder
import ampairsapp.feature.agent.generated.resources.agent_model_download_confirm
import ampairsapp.feature.agent.generated.resources.agent_model_download_decline
import ampairsapp.feature.agent.generated.resources.agent_model_download_failed_title
import ampairsapp.feature.agent.generated.resources.agent_model_download_hide
import ampairsapp.feature.agent.generated.resources.agent_model_download_message
import ampairsapp.feature.agent.generated.resources.agent_model_download_retry
import ampairsapp.feature.agent.generated.resources.agent_model_download_title
import ampairsapp.feature.agent.generated.resources.agent_model_downloading_progress
import ampairsapp.feature.agent.generated.resources.agent_model_downloading_title
import ampairsapp.feature.agent.generated.resources.agent_model_active
import ampairsapp.feature.agent.generated.resources.agent_model_chip_cd
import ampairsapp.feature.agent.generated.resources.agent_model_chip_downloading
import ampairsapp.feature.agent.generated.resources.agent_model_chip_loading
import ampairsapp.feature.agent.generated.resources.agent_model_chip_rule_based
import ampairsapp.feature.agent.generated.resources.agent_model_chip_unsupported
import ampairsapp.feature.agent.generated.resources.agent_model_manager_unsupported_note
import ampairsapp.feature.agent.generated.resources.agent_model_delete
import ampairsapp.feature.agent.generated.resources.agent_model_download_action
import ampairsapp.feature.agent.generated.resources.agent_model_manager_rule_based_note
import ampairsapp.feature.agent.generated.resources.agent_model_manager_subtitle
import ampairsapp.feature.agent.generated.resources.agent_model_manager_title
import ampairsapp.feature.agent.generated.resources.agent_model_recommended
import ampairsapp.feature.agent.generated.resources.agent_model_retry
import ampairsapp.feature.agent.generated.resources.agent_model_role_size
import ampairsapp.feature.agent.generated.resources.agent_model_status_cloud
import ampairsapp.feature.agent.generated.resources.agent_model_status_downloading
import ampairsapp.feature.agent.generated.resources.agent_model_status_failed
import ampairsapp.feature.agent.generated.resources.agent_model_status_installed
import ampairsapp.feature.agent.generated.resources.agent_model_status_not_installed
import ampairsapp.feature.agent.generated.resources.agent_model_use
import ampairsapp.feature.agent.generated.resources.agent_mute_cd
import ampairsapp.feature.agent.generated.resources.agent_send_cd
import ampairsapp.feature.agent.generated.resources.agent_stop_cd
import ampairsapp.feature.agent.generated.resources.agent_unmute_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_mode_cd
import ampairsapp.feature.agent.generated.resources.agent_thinking
import ampairsapp.feature.agent.generated.resources.agent_title
import ampairsapp.feature.agent.generated.resources.agent_adapter_device
import ampairsapp.feature.agent.generated.resources.agent_adapter_whisper
import ampairsapp.feature.agent.generated.resources.agent_settings_cd
import ampairsapp.feature.agent.generated.resources.agent_settings_llm
import ampairsapp.feature.agent.generated.resources.agent_settings_manage_models
import ampairsapp.feature.agent.generated.resources.agent_settings_mic
import ampairsapp.feature.agent.generated.resources.agent_settings_none
import ampairsapp.feature.agent.generated.resources.agent_settings_reasoning
import ampairsapp.feature.agent.generated.resources.agent_settings_reasoning_desc
import ampairsapp.feature.agent.generated.resources.agent_settings_stt
import ampairsapp.feature.agent.generated.resources.agent_settings_telemetry
import ampairsapp.feature.agent.generated.resources.agent_settings_telemetry_desc
import ampairsapp.feature.agent.generated.resources.agent_settings_title
import ampairsapp.feature.agent.generated.resources.agent_settings_tts
import com.ampairs.agent.speech.SpeechAdapterOption
import com.ampairs.agent.ui.components.MessageBubble
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToRoute: (Map<String, String>) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    // IME insets are handled once by the global layout's Scaffold (GlobalAppLayoutNav3); applying
    // imePadding() here too double-pads and leaves a keyboard-sized black gap above the keyboard.
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.agent_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.agent_back_cd),
                    )
                }
            },
            actions = {
                ModelStatusChip(
                    state = uiState.modelBar,
                    onClick = { viewModel.openModelManager() },
                )
                IconButton(onClick = { viewModel.toggleMute() }) {
                    Icon(
                        imageVector = if (uiState.isTtsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = stringResource(
                            if (uiState.isTtsMuted) Res.string.agent_unmute_cd else Res.string.agent_mute_cd,
                        ),
                    )
                }
                IconButton(onClick = { viewModel.openSettings() }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.agent_settings_cd))
                }
                if (uiState.messages.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(Res.string.agent_clear_chat_cd))
                    }
                }
            },
        )

        // Messages list
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.messages.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.agent_empty_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.agent_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onActionClick = onNavigateToRoute,
                        )
                    }

                    // Typing / thinking indicator: while the model reasons (thought) or before its
                    // reply bubble has started (last message still the user's). Hidden once text streams.
                    val showThinking = uiState.streamingThought != null ||
                        (uiState.isProcessing && (uiState.messages.lastOrNull()?.isFromUser != false))
                    if (showThinking) {
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = uiState.streamingThought?.takeIf { it.isNotBlank() }
                                        ?: stringResource(Res.string.agent_thinking),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }

                    // Bottom spacer
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        // Confirmation bar for money/destructive actions — nothing persists until the user taps Confirm (FR-006)
        if (uiState.pendingConfirmation != null) {
            ConfirmActionBar(
                onConfirm = viewModel::confirmPending,
                onCancel = viewModel::cancelPending,
            )
        }

        // First-use on-device model download: consent → progress → failure (auto-download on accept).
        uiState.llmDownloadPrompt?.let { prompt ->
            LlmDownloadDialog(
                prompt = prompt,
                onAccept = viewModel::onAcceptModelDownload,
                onDecline = viewModel::onDeclineModelDownload,
                onDismiss = viewModel::dismissModelDownloadPrompt,
            )
        }

        // Full model manager: download other models, switch the active one, or delete files.
        if (uiState.showModelSheet) {
            ModelManagerSheet(
                models = uiState.models,
                supported = uiState.llmSupported,
                isRuleBased = uiState.modelBar.phase == ModelPhase.RULE_BASED,
                onUse = viewModel::onUseModel,
                onDownload = viewModel::onDownloadModel,
                onDelete = viewModel::onDeleteModel,
                onDismiss = viewModel::closeModelManager,
            )
        }

        // Assistant settings: switch the STT / TTS engine adapters, and open the model manager.
        if (uiState.showSettings) {
            AssistantSettingsSheet(
                sttOptions = uiState.sttOptions,
                ttsOptions = uiState.ttsOptions,
                selectedSttId = uiState.selectedSttId,
                selectedTtsId = uiState.selectedTtsId,
                modelName = uiState.modelBar.modelName,
                whisperModels = uiState.whisperModels,
                micDevices = uiState.micDevices,
                selectedMicId = uiState.selectedMicId,
                telemetryEnabled = uiState.telemetryEnabled,
                onToggleTelemetry = viewModel::onToggleTelemetry,
                reasoningEnabled = uiState.reasoningEnabled,
                onToggleReasoning = viewModel::onToggleReasoning,
                onSelectMicDevice = viewModel::onSelectMicDevice,
                onSelectStt = viewModel::onSelectSttAdapter,
                onSelectTts = viewModel::onSelectTtsAdapter,
                onSelectWhisperModel = viewModel::onSelectWhisperModel,
                onDownloadWhisperModel = viewModel::onDownloadWhisperModel,
                onDeleteWhisperModel = viewModel::onDeleteWhisperModel,
                onManageModels = {
                    viewModel.closeSettings()
                    viewModel.openModelManager()
                },
                onDismiss = viewModel::closeSettings,
            )
        }

        // Input area
        ChatInputBar(
            text = uiState.inputText,
            onTextChange = viewModel::updateInputText,
            onSend = viewModel::sendMessage,
            isProcessing = uiState.isProcessing,
            onStop = viewModel::stopGeneration,
            onVoiceMode = viewModel::enterVoiceMode,
        )
    }

        // Full-screen hands-free voice conversation overlay (covers the chat while active).
        uiState.voiceMode?.let { voice ->
            VoiceConversationScreen(
                state = voice,
                onPauseToggle = viewModel::toggleVoicePause,
                onExit = viewModel::exitVoiceMode,
            )
        }
    }
}

@Composable
private fun ConfirmActionBar(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.agent_confirm_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.agent_cancel)) }
            Button(onClick = onConfirm) { Text(stringResource(Res.string.agent_confirm)) }
        }
    }
}

@Composable
private fun LlmDownloadDialog(
    prompt: LlmDownloadPrompt,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        is LlmDownloadPrompt.Consent -> AlertDialog(
            onDismissRequest = onDecline,
            title = { Text(stringResource(Res.string.agent_model_download_title)) },
            text = {
                Text(stringResource(Res.string.agent_model_download_message, prompt.modelName, prompt.sizeText))
            },
            confirmButton = {
                Button(onClick = onAccept) { Text(stringResource(Res.string.agent_model_download_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDecline) { Text(stringResource(Res.string.agent_model_download_decline)) }
            },
        )

        is LlmDownloadPrompt.Downloading -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.agent_model_downloading_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (prompt.progress > 0f) {
                        LinearProgressIndicator(progress = { prompt.progress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        stringResource(
                            Res.string.agent_model_downloading_progress,
                            (prompt.progress * 100).toInt(),
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.agent_model_download_hide)) }
            },
        )

        is LlmDownloadPrompt.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.agent_model_download_failed_title)) },
            text = { Text(prompt.message) },
            confirmButton = {
                Button(onClick = onAccept) { Text(stringResource(Res.string.agent_model_download_retry)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.agent_model_download_hide)) }
            },
        )
    }
}

/** Compact top-bar chip showing the active model + its phase; tap opens the model manager. */
@Composable
private fun ModelStatusChip(
    state: ModelBarState,
    onClick: () -> Unit,
) {
    val chipCd = stringResource(Res.string.agent_model_chip_cd)
    val label = when (state.phase) {
        ModelPhase.UNSUPPORTED -> stringResource(Res.string.agent_model_chip_unsupported)
        ModelPhase.RULE_BASED -> stringResource(Res.string.agent_model_chip_rule_based)
        ModelPhase.LOADING -> stringResource(Res.string.agent_model_chip_loading)
        ModelPhase.DOWNLOADING ->
            state.modelName + " " + stringResource(Res.string.agent_model_chip_downloading, (state.downloadProgress * 100).toInt())
        ModelPhase.NOT_DOWNLOADED, ModelPhase.READY, ModelPhase.FAILED ->
            state.modelName.ifBlank { stringResource(Res.string.agent_model_chip_rule_based) }
    }
    AssistChip(
        onClick = onClick,
        label = {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
        },
        leadingIcon = {
            when (state.phase) {
                ModelPhase.LOADING, ModelPhase.DOWNLOADING ->
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                ModelPhase.READY ->
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                ModelPhase.FAILED ->
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                ModelPhase.NOT_DOWNLOADED ->
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                ModelPhase.RULE_BASED, ModelPhase.UNSUPPORTED ->
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
            }
        },
        modifier = Modifier.semantics { contentDescription = chipCd },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelManagerSheet(
    models: List<ModelUiItem>,
    supported: Boolean,
    isRuleBased: Boolean,
    onUse: (String) -> Unit,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.agent_model_manager_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!supported) {
                // No on-device engine on this platform (Desktop/iOS) — explain, offer nothing.
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.agent_model_manager_unsupported_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                return@Column
            }
            Text(
                text = stringResource(Res.string.agent_model_manager_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isRuleBased) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.agent_model_manager_rule_based_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            models.forEach { item ->
                ModelRow(
                    item = item,
                    onUse = { onUse(item.id) },
                    onDownload = { onDownload(item.id) },
                    onDelete = { onDelete(item.id) },
                )
            }
        }
    }
}

@Composable
private fun ModelRow(
    item: ModelUiItem,
    onUse: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.recommended) {
                    StatusPill(
                        text = stringResource(Res.string.agent_model_recommended),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                if (item.isActive) {
                    StatusPill(
                        text = stringResource(Res.string.agent_model_active),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                // Cloud models have no on-disk size — show just the role rather than "CHAT · ".
                text = if (item.sizeText.isBlank()) {
                    item.role
                } else {
                    stringResource(Res.string.agent_model_role_size, item.role, item.sizeText)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (val status = item.status) {
                ModelItemStatus.Cloud -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.agent_model_status_cloud),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (!item.isActive) {
                        Button(onClick = onUse) { Text(stringResource(Res.string.agent_model_use)) }
                    }
                }

                is ModelItemStatus.Downloading -> {
                    if (status.progress > 0f) {
                        LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        text = stringResource(Res.string.agent_model_status_downloading, (status.progress * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is ModelItemStatus.Installed -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!item.isActive) {
                        Button(onClick = onUse) { Text(stringResource(Res.string.agent_model_use)) }
                    } else {
                        Text(
                            text = stringResource(Res.string.agent_model_status_installed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                        )
                    }
                    OutlinedButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.agent_model_delete))
                    }
                }

                is ModelItemStatus.Failed -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.agent_model_status_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = onDownload) { Text(stringResource(Res.string.agent_model_retry)) }
                }

                is ModelItemStatus.NotInstalled -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.agent_model_status_not_installed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = onDownload) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.agent_model_download_action))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantSettingsSheet(
    sttOptions: List<SpeechAdapterOption>,
    ttsOptions: List<SpeechAdapterOption>,
    selectedSttId: String?,
    selectedTtsId: String?,
    modelName: String,
    whisperModels: List<WhisperModelUi>,
    micDevices: List<SpeechAdapterOption>,
    selectedMicId: String?,
    telemetryEnabled: Boolean,
    onToggleTelemetry: (Boolean) -> Unit,
    reasoningEnabled: Boolean,
    onToggleReasoning: (Boolean) -> Unit,
    onSelectMicDevice: (String) -> Unit,
    onSelectStt: (String) -> Unit,
    onSelectTts: (String) -> Unit,
    onSelectWhisperModel: (String) -> Unit,
    onDownloadWhisperModel: (String) -> Unit,
    onDeleteWhisperModel: (String) -> Unit,
    onManageModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.agent_settings_title), style = MaterialTheme.typography.headlineSmall)

            val effectiveSttId = selectedSttId ?: sttOptions.firstOrNull()?.id
            AdapterSection(
                title = stringResource(Res.string.agent_settings_stt),
                options = sttOptions,
                selectedId = effectiveSttId,
                onSelect = onSelectStt,
            )
            // Whisper size picker — only when the Whisper STT engine is the active choice.
            if (effectiveSttId == "whisper" && whisperModels.isNotEmpty()) {
                WhisperModelSection(
                    models = whisperModels,
                    onSelect = onSelectWhisperModel,
                    onDownload = onDownloadWhisperModel,
                    onDelete = onDeleteWhisperModel,
                )
            }
            // Mic input device picker — Desktop only (the list is empty elsewhere).
            if (micDevices.isNotEmpty()) {
                AdapterSection(
                    title = stringResource(Res.string.agent_settings_mic),
                    options = micDevices,
                    selectedId = selectedMicId ?: micDevices.firstOrNull()?.id,
                    onSelect = onSelectMicDevice,
                )
            }
            AdapterSection(
                title = stringResource(Res.string.agent_settings_tts),
                options = ttsOptions,
                selectedId = selectedTtsId ?: ttsOptions.firstOrNull()?.id,
                onSelect = onSelectTts,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.agent_settings_llm), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = modelName.ifBlank { stringResource(Res.string.agent_model_chip_rule_based) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onManageModels) {
                        Text(stringResource(Res.string.agent_settings_manage_models))
                    }
                }
            }

            // Opt-in chat telemetry (default OFF): upload transcripts to improve the assistant.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.agent_settings_telemetry),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(Res.string.agent_settings_telemetry_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = telemetryEnabled, onCheckedChange = onToggleTelemetry)
            }

            // Show the model's transient "thinking" (reasoning) text while it reasons (default ON).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.agent_settings_reasoning),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(Res.string.agent_settings_reasoning_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = reasoningEnabled, onCheckedChange = onToggleReasoning)
            }
        }
    }
}

@Composable
private fun AdapterSection(
    title: String,
    options: List<SpeechAdapterOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (options.isEmpty()) {
            Text(
                text = stringResource(Res.string.agent_settings_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            options.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(option.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option.id == selectedId, onClick = { onSelect(option.id) })
                    Text(adapterLabel(option), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Whisper model-size rows: select the active size, and download/delete each one. */
@Composable
private fun WhisperModelSection(
    models: List<WhisperModelUi>,
    onSelect: (String) -> Unit,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Heading reuses the existing Whisper adapter label (no new string resource needed).
        Text(stringResource(Res.string.agent_adapter_whisper), style = MaterialTheme.typography.titleMedium)
        models.forEach { model ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(model.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = model.isSelected, onClick = { onSelect(model.id) })
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = whisperStatusText(model),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when (model.status) {
                    is ModelItemStatus.Downloading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    ModelItemStatus.Installed -> TextButton(onClick = { onDelete(model.id) }) {
                        Text(stringResource(Res.string.agent_model_delete))
                    }
                    is ModelItemStatus.Failed -> TextButton(onClick = { onDownload(model.id) }) {
                        Text(stringResource(Res.string.agent_model_retry))
                    }
                    ModelItemStatus.NotInstalled -> TextButton(onClick = { onDownload(model.id) }) {
                        Text(stringResource(Res.string.agent_model_download_action))
                    }
                    // Whisper STT models are always on-device files — never cloud. Branch only for
                    // exhaustiveness over the shared ModelItemStatus.
                    ModelItemStatus.Cloud -> Unit
                }
            }
        }
    }
}

/** Secondary line under a Whisper size row: size + install state. */
@Composable
private fun whisperStatusText(model: WhisperModelUi): String {
    val state = when (val s = model.status) {
        is ModelItemStatus.Downloading ->
            stringResource(Res.string.agent_model_status_downloading, (s.progress * 100).toInt())
        ModelItemStatus.Installed -> stringResource(Res.string.agent_model_status_installed)
        is ModelItemStatus.Failed -> stringResource(Res.string.agent_model_status_failed)
        ModelItemStatus.NotInstalled -> stringResource(Res.string.agent_model_status_not_installed)
        // Whisper STT models are always on-device — never cloud (branch for exhaustiveness only).
        ModelItemStatus.Cloud -> stringResource(Res.string.agent_model_status_cloud)
    }
    return "${model.sizeText} · $state"
}

/** Localized display name for a speech adapter, by id, falling back to the registered label. */
@Composable
private fun adapterLabel(option: SpeechAdapterOption): String = when (option.id) {
    "native" -> stringResource(Res.string.agent_adapter_device)
    "whisper" -> stringResource(Res.string.agent_adapter_whisper)
    else -> option.label
}

@Composable
private fun StatusPill(text: String, container: Color, content: Color) {
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
    onStop: () -> Unit,
    onVoiceMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(Res.string.agent_input_placeholder)) },
                modifier = Modifier
                    .weight(1f)
                    // Hardware-keyboard ergonomics: plain Enter sends; any modifier (Shift / Ctrl /
                    // Alt·Option / Cmd·Meta) + Enter inserts a newline. We intercept on key-down and
                    // consume the bare-Enter so the field doesn't also insert a line break.
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            val withModifier = event.isShiftPressed || event.isCtrlPressed ||
                                event.isAltPressed || event.isMetaPressed
                            if (withModifier) {
                                false // let the newline through
                            } else {
                                if (text.isNotBlank() && !isProcessing) onSend()
                                true // consume — don't insert a line break
                            }
                        } else {
                            false
                        }
                    },
                maxLines = 4,
                shape = MaterialTheme.shapes.extraLarge,
            )

            Spacer(modifier = Modifier.width(8.dp))

            when {
                // While the assistant is generating, the action button stops the stream.
                isProcessing -> IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = stringResource(Res.string.agent_stop_cd))
                }

                text.isNotBlank() -> IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(Res.string.agent_send_cd))
                }

                // Blank input → enter the full-screen hands-free voice conversation.
                else -> IconButton(
                    onClick = onVoiceMode,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(Icons.Default.Mic, contentDescription = stringResource(Res.string.agent_voice_mode_cd))
                }
            }
        }
    }
}
