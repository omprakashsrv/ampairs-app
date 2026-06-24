package com.ampairs.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_total
import ampairsapp.feature.agent.generated.resources.agent_voice_note_label
import ampairsapp.feature.agent.generated.resources.agent_voice_note_play_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_note_stop_play_cd
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.voice.formatVoiceNoteDuration
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageBubble(
    message: ChatMessage,
    onActionClick: ((Map<String, String>) -> Unit)? = null,
    isVoiceNotePlaying: Boolean = false,
    onVoiceNoteToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp,
                ),
                color = when {
                    message.isError -> MaterialTheme.colorScheme.errorContainer
                    message.isFromUser -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                tonalElevation = if (message.isFromUser) 0.dp else 1.dp,
            ) {
                val onColor = when {
                    message.isError -> MaterialTheme.colorScheme.onErrorContainer
                    message.isFromUser -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.isVoiceNote) {
                        VoiceNoteContent(
                            isPlaying = isVoiceNotePlaying,
                            durationMs = message.voiceNoteDurationMs ?: 0L,
                            contentColor = onColor,
                            onToggle = onVoiceNoteToggle,
                        )
                    } else {
                        Text(
                            text = message.text,
                            color = onColor,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // Money total rendered in the active workspace's business currency (FR-013).
                        message.amount?.let { amount ->
                            Text(
                                text = "${stringResource(Res.string.agent_total)}: ${formatMoney(amount, LocalAppLocale.current)}",
                                color = onColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            // Show action result card if present
            message.actionResult?.let { result ->
                ActionResultCard(
                    result = result,
                    onViewClick = onActionClick,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** A voice-note bubble: play/stop control + a waveform glyph + the duration label. */
@Composable
private fun VoiceNoteContent(
    isPlaying: Boolean,
    durationMs: Long,
    contentColor: Color,
    onToggle: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = { onToggle?.invoke() }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) Res.string.agent_voice_note_stop_play_cd else Res.string.agent_voice_note_play_cd,
                ),
                tint = contentColor,
            )
        }
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = stringResource(Res.string.agent_voice_note_label),
            tint = contentColor,
        )
        Text(
            text = formatVoiceNoteDuration(durationMs),
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
