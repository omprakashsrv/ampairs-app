package com.ampairs.form.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.form_agent_chat_title
import ampairsapp.feature.form.generated.resources.form_agent_voice_listen
import ampairsapp.feature.form.generated.resources.form_agent_voice_stop
import ampairsapp.feature.form.generated.resources.form_agent_processing
import org.jetbrains.compose.resources.stringResource

/**
 * Chat bubble FAB for voice-to-form field filling. Displays on edit screens (customer, product, etc.)
 * in the top-right corner. On tap, opens a chat interface where users can say things like
 * "set customer name to John" and fields are auto-filled.
 *
 * @param entityType the entity being edited (e.g., "customer", "product")
 * @param onFieldFill callback when a field should be filled with (fieldKey, value)
 * @param modifier optional Modifier for positioning
 */
@Composable
fun FormAgentChatBubble(
    entityType: String,
    onFieldFill: (fieldKey: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    // FAB in top-right
    Box(modifier = modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp)) {
        FloatingActionButton(
            onClick = { showDialog = !showDialog },
            modifier = Modifier.align(Alignment.TopEnd),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(Res.string.form_agent_chat_title),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    // Dialog when open
    if (showDialog) {
        FormAgentChatDialog(
            entityType = entityType,
            onDismiss = { showDialog = false },
            onFieldFill = onFieldFill,
        )
    }
}

/**
 * Modal dialog showing the form agent chat interface with voice input.
 * Placeholder for now — full implementation requires integration with AgentViewModel
 * and voice STT handling from the agent module.
 */
@Composable
private fun FormAgentChatDialog(
    entityType: String,
    onDismiss: () -> Unit,
    onFieldFill: (fieldKey: String, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isListening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var chatHistory by remember { mutableStateOf(listOf<ChatMessage>()) }

    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.form_agent_chat_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Entity context
            Text(
                text = "Editing: $entityType",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Chat history
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chatHistory.size) { index ->
                    ChatMessageBubble(chatHistory[index])
                }
            }

            // Voice input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mic button
                FloatingActionButton(
                    onClick = { isListening = !isListening },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening)
                            stringResource(Res.string.form_agent_voice_stop)
                        else
                            stringResource(Res.string.form_agent_voice_listen),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                // Transcript display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (isProcessing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                stringResource(Res.string.form_agent_processing),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    } else {
                        Text(
                            text = if (transcript.isBlank()) "Say something..." else transcript,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (transcript.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Done")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Single chat message bubble (user or assistant).
 */
@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 0.dp,
                topEnd = if (isUser) 0.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

/**
 * Simple chat message model for display in the dialog.
 */
private data class ChatMessage(
    val text: String,
    val isUser: Boolean = true,
)
