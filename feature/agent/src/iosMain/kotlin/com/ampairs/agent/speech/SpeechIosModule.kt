package com.ampairs.agent.speech

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * iOS speech bindings: real on-device `SFSpeechRecognizer` STT + `AVSpeechSynthesizer` TTS, enabling
 * the hands-free voice conversation on iOS. Needs the host app's Info.plist
 * `NSMicrophoneUsageDescription` + `NSSpeechRecognitionUsageDescription` keys; permission is driven by
 * `IosMicPermissionController`.
 */
@ContributesTo(AppScope::class)
interface SpeechIosModule {
    companion object {
        @Provides
        fun provideSpeechToText(): SpeechToText = IosSpeechToText()

        @Provides
        fun provideTextToSpeech(): TextToSpeech = IosTextToSpeech()
    }
}
