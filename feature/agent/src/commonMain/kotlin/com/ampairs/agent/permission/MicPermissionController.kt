package com.ampairs.agent.permission

/**
 * Cross-platform microphone-permission gate for voice input (T016, US4-3). The ChatViewModel calls
 * [ensureMicGranted] before starting recognition; on denial the UI stays on text input (the Android
 * recognizer also surfaces a permission error as a secondary fallback).
 *
 * Platform bindings (Metro `@ContributesBinding(AppScope)`): Android uses the Grant library's
 * `RECORD_AUDIO` request; iOS prompts at `AVAudioSession`/`SFSpeechRecognizer` time (granted here for
 * now); Desktop has no recognizer and reports granted.
 */
interface MicPermissionController {
    /** Request/confirm microphone access. Returns true if granted. */
    suspend fun ensureMicGranted(): Boolean
}
