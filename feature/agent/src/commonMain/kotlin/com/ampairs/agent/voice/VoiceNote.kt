package com.ampairs.agent.voice

/**
 * A recorded in-chat voice note (FR: voice notes). The audio bytes themselves live in the
 * [VoiceNoteController] (kept in memory, keyed by [id]); this is just the lightweight handle the
 * chat message carries for display. [id] equals the owning [com.ampairs.agent.core.ChatMessage.id].
 */
data class VoiceNote(
    val id: String,
    val durationMs: Long,
)

/** Recorder lifecycle exposed to the UI — the chat input toggles between these. */
enum class VoiceRecordingState { Idle, Recording }

/**
 * Formats a voice-note duration as `m:ss` (e.g. 3200ms → "0:03"). Pure/KMP-safe — no `String.format`.
 */
fun formatVoiceNoteDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) + 500L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
