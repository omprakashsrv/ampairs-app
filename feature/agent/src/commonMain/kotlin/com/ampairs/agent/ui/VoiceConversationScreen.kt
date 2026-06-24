package com.ampairs.agent.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_voice_connecting
import ampairsapp.feature.agent.generated.resources.agent_voice_exit_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_listening
import ampairsapp.feature.agent.generated.resources.agent_voice_listening_hint
import ampairsapp.feature.agent.generated.resources.agent_voice_pause_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_paused
import ampairsapp.feature.agent.generated.resources.agent_voice_resume_cd
import ampairsapp.feature.agent.generated.resources.agent_voice_speaking
import ampairsapp.feature.agent.generated.resources.agent_voice_thinking
import ampairsapp.feature.agent.generated.resources.agent_voice_title
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen hands-free voice conversation overlay (FR-008/009). Renders the loop's [VoicePhase]
 * with a pulsing mic orb, a live transcript while listening / the spoken reply while speaking, and a
 * pause-resume control. Stateless — driven entirely by [state]; the loop lives in [ChatViewModel].
 */
@Composable
fun VoiceConversationScreen(
    state: VoiceConversationState,
    onPauseToggle: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // Exit (top-right)
            IconButton(
                onClick = onExit,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.agent_voice_exit_cd))
            }

            Text(
                text = stringResource(Res.string.agent_voice_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )

            // Center: orb + status + transcript/reply
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                MicOrb(phase = state.phase)
                Spacer(Modifier.height(28.dp))
                Text(
                    text = phaseLabel(state.phase),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = transcriptLine(state),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            // Pause / resume (bottom-center), hidden in terminal Error state.
            if (state.phase != VoicePhase.Error) {
                val paused = state.phase == VoicePhase.Paused
                FilledIconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier.align(Alignment.BottomCenter).size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = stringResource(
                            if (paused) Res.string.agent_voice_resume_cd else Res.string.agent_voice_pause_cd,
                        ),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

/** The mic orb: pulses while listening/speaking, spins while thinking, static otherwise. */
@Composable
private fun MicOrb(phase: VoicePhase) {
    val active = phase == VoicePhase.Listening || phase == VoicePhase.Speaking
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val container = when (phase) {
        VoicePhase.Listening -> MaterialTheme.colorScheme.primary
        VoicePhase.Speaking -> MaterialTheme.colorScheme.tertiary
        VoicePhase.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (phase) {
        VoicePhase.Listening -> MaterialTheme.colorScheme.onPrimary
        VoicePhase.Speaking -> MaterialTheme.colorScheme.onTertiary
        VoicePhase.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = container,
            modifier = Modifier.size(140.dp).scale(if (active) pulse else 1f),
        ) {}
        if (phase == VoicePhase.Thinking) {
            CircularProgressIndicator(
                modifier = Modifier.size(72.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

@Composable
private fun phaseLabel(phase: VoicePhase): String = when (phase) {
    VoicePhase.Connecting -> stringResource(Res.string.agent_voice_connecting)
    VoicePhase.Listening -> stringResource(Res.string.agent_voice_listening)
    VoicePhase.Thinking -> stringResource(Res.string.agent_voice_thinking)
    VoicePhase.Speaking -> stringResource(Res.string.agent_voice_speaking)
    VoicePhase.Paused -> stringResource(Res.string.agent_voice_paused)
    VoicePhase.Error -> ""
}

/** The supporting line under the status: live transcript, the reply being spoken, or a hint/error. */
@Composable
private fun transcriptLine(state: VoiceConversationState): String = when (state.phase) {
    VoicePhase.Listening -> state.partialTranscript.ifBlank { stringResource(Res.string.agent_voice_listening_hint) }
    VoicePhase.Thinking -> state.lastUserText
    VoicePhase.Speaking -> state.lastReply
    VoicePhase.Error -> state.error ?: ""
    VoicePhase.Connecting, VoicePhase.Paused -> ""
}
