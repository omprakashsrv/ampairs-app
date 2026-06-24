package com.ampairs.agent.permission

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.Speech.SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
import kotlin.coroutines.resume

/**
 * iOS mic-permission gate (T016): requests both speech-recognition authorization (for STT) and the
 * microphone record permission, returning true only when both are granted. The system prompts use the
 * host app's Info.plist `NSSpeechRecognitionUsageDescription` + `NSMicrophoneUsageDescription` strings.
 */
@OptIn(ExperimentalForeignApi::class)
@Inject
@ContributesBinding(AppScope::class)
class IosMicPermissionController : MicPermissionController {
    override suspend fun ensureMicGranted(): Boolean {
        val speechGranted = suspendCancellableCoroutine { cont ->
            SFSpeechRecognizer.requestAuthorization { status: SFSpeechRecognizerAuthorizationStatus ->
                cont.resume(status == SFSpeechRecognizerAuthorizationStatusAuthorized)
            }
        }
        if (!speechGranted) return false
        return suspendCancellableCoroutine { cont ->
            AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                cont.resume(granted)
            }
        }
    }
}
