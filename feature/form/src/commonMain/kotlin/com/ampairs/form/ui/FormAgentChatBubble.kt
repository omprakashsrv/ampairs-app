package com.ampairs.form.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.form_agent_chat_title
import ampairsapp.feature.form.generated.resources.form_agent_close_cd
import ampairsapp.feature.form.generated.resources.form_agent_editing
import ampairsapp.feature.form.generated.resources.form_agent_empty
import ampairsapp.feature.form.generated.resources.form_agent_listening
import ampairsapp.feature.form.generated.resources.form_agent_open_cd
import ampairsapp.feature.form.generated.resources.form_agent_processing
import ampairsapp.feature.form.generated.resources.form_agent_tap_to_speak
import ampairsapp.feature.form.generated.resources.form_agent_voice_listen
import ampairsapp.feature.form.generated.resources.form_agent_voice_stop
import ampairsapp.feature.form.generated.resources.form_agent_voice_unavailable
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Floating bubble entry point for AI-assisted form filling: a FAB plus the assistant dialog. Use this
 * when a screen wants an overlay trigger. Screens that already have a TopAppBar should prefer a toolbar
 * action that opens [FormAgentChatDialog] directly, for a guaranteed top-right affordance (see
 * CustomerFormScreen).
 */
@Composable
fun FormAgentChatBubble(
    entityType: String,
    onFieldFill: (fieldKey: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormAgentViewModel = assistedMetroViewModel<FormAgentViewModel, FormAgentViewModel.Factory>(
        key = entityType,
    ) { create(entityType) },
) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.fills.collect { fill -> onFieldFill(fill.fieldKey, fill.value) }
    }

    Box(modifier = modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp)) {
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.TopEnd),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(Res.string.form_agent_open_cd),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    if (showDialog) {
        FormAgentChatDialog(
            entityType = entityType,
            viewModel = viewModel,
            onDismiss = { showDialog = false },
        )
    }
}

/**
 * The assistant chat as a modal **voice** dialog. On open it starts listening; the user speaks all the
 * details ("name … phone … PAN … address …") and every matching field is filled in one shot. A text box
 * is kept as a fallback when speech is unavailable or the user prefers typing. Public so a host screen
 * can trigger it from a TopAppBar action while owning the [viewModel] and the fill collection.
 */
@Composable
fun FormAgentChatDialog(
    entityType: String,
    viewModel: FormAgentViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Start listening as soon as the voice assistant opens (the whole point: tap → speak → fill).
    LaunchedEffect(Unit) {
        if (state.voiceAvailable) viewModel.startVoice()
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    fun dismiss() {
        viewModel.stopVoice()
        onDismiss()
    }

    Dialog(onDismissRequest = { dismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.form_agent_chat_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { dismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.form_agent_close_cd))
                    }
                }

                Text(
                    text = stringResource(Res.string.form_agent_editing, entityType),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Voice control — the primary interaction.
                if (state.voiceAvailable) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FloatingActionButton(
                            onClick = { if (state.isListening) viewModel.stopVoice() else viewModel.startVoice() },
                            modifier = Modifier.size(72.dp),
                            containerColor = if (state.isListening) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        ) {
                            Icon(
                                imageVector = if (state.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = stringResource(
                                    if (state.isListening) Res.string.form_agent_voice_stop
                                    else Res.string.form_agent_voice_listen,
                                ),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Text(
                            text = stringResource(
                                if (state.isListening) Res.string.form_agent_listening
                                else Res.string.form_agent_tap_to_speak,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.partialTranscript.isNotBlank()) {
                            Text(
                                text = state.partialTranscript,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.messages.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.form_agent_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(state.messages) { message -> ChatMessageBubble(message) }
                }

                if (state.isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(Res.string.form_agent_processing),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (!state.voiceAvailable) {
                    Text(
                        text = stringResource(Res.string.form_agent_voice_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: FormChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 0.dp,
                topEnd = if (message.isUser) 0.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
