package com.ampairs.agent.permission

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * iOS mic-permission gate (T016). iOS prompts for microphone/speech access at
 * `AVAudioSession`/`SFSpeechRecognizer` time, so this reports granted and lets the recognizer drive
 * the prompt. Replace with the Grant iOS binding once that path is verified (paired with T014).
 */
@Inject
@ContributesBinding(AppScope::class)
class IosMicPermissionController : MicPermissionController {
    override suspend fun ensureMicGranted(): Boolean = true
}
