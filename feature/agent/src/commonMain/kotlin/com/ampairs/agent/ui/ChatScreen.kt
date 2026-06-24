package com.ampairs.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_cancel
import ampairsapp.feature.agent.generated.resources.agent_clear_chat_cd
import ampairsapp.feature.agent.generated.resources.agent_confirm
import ampairsapp.feature.agent.generated.resources.agent_confirm_prompt
import ampairsapp.feature.agent.generated.resources.agent_empty_hint
import ampairsapp.feature.agent.generated.resources.agent_empty_title
import ampairsapp.feature.agent.generated.resources.agent_input_placeholder
import ampairsapp.feature.agent.generated.resources.agent_mute_cd
import ampairsapp.feature.agent.generated.resources.agent_send_cd
import ampairsapp.feature.agent.generated.resources.agent_unmute_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_note_cancel_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_note_record_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_note_recording
import ampairsapp.feature.agent.generated.resources.agent_voice_note_stop_cd
import ampairsapp.feature.agent.generated.resources.agent_thinking
import ampairsapp.feature.agent.generated.resources.agent_title
import com.ampairs.agent.ui.components.MessageBubble
import com.ampairs.agent.ui.components.VoiceInputButton
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToRoute: (Map<String, String>) -> Unit = {},
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

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.agent_title)) },
            actions = {
                IconButton(onClick = { viewModel.toggleMute() }) {
                    Icon(
                        imageVector = if (uiState.isTtsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = stringResource(
                            if (uiState.isTtsMuted) Res.string.agent_unmute_cd else Res.string.agent_mute_cd,
                        ),
                    )
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
                            isVoiceNotePlaying = uiState.playingVoiceNoteId == message.id,
                            onVoiceNoteToggle = { viewModel.toggleVoiceNotePlayback(message.id) },
                        )
                    }

                    // Typing indicator
                    if (uiState.isProcessing) {
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = stringResource(Res.string.agent_thinking),
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

        // Input area
        ChatInputBar(
            text = uiState.inputText,
            onTextChange = viewModel::updateInputText,
            onSend = viewModel::sendMessage,
            isProcessing = uiState.isProcessing,
            isListening = uiState.isListening,
            onVoiceClick = { viewModel.toggleVoiceInput() },
            isRecordingVoiceNote = uiState.isRecordingVoiceNote,
            onStartVoiceNote = viewModel::startVoiceNote,
            onStopAndSendVoiceNote = viewModel::stopAndSendVoiceNote,
            onCancelVoiceNote = viewModel::cancelVoiceNote,
        )
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
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
    isListening: Boolean,
    onVoiceClick: () -> Unit,
    isRecordingVoiceNote: Boolean,
    onStartVoiceNote: () -> Unit,
    onStopAndSendVoiceNote: () -> Unit,
    onCancelVoiceNote: () -> Unit,
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
            if (isRecordingVoiceNote) {
                // Recording a voice note: discard | "Recording…" | stop & send.
                IconButton(onClick = onCancelVoiceNote, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.agent_voice_note_cancel_cd))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.agent_voice_note_recording),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onStopAndSendVoiceNote,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(Res.string.agent_voice_note_stop_cd),
                    )
                }
            } else {
                VoiceInputButton(
                    isListening = isListening,
                    onClick = onVoiceClick,
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text(stringResource(Res.string.agent_input_placeholder)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = MaterialTheme.shapes.extraLarge,
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (text.isNotBlank() && !isProcessing) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(Res.string.agent_send_cd),
                        )
                    }
                } else {
                    // Blank input → record a voice note (waveform glyph distinguishes it from STT mic).
                    IconButton(
                        onClick = onStartVoiceNote,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = stringResource(Res.string.agent_voice_note_record_cd),
                        )
                    }
                }
            }
        }
    }
}
