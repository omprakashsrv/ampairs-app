package com.ampairs.agent.speech

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * iOS speech bindings. Placeholder fallbacks for now — T014 replaces these bodies with the real
 * `SFSpeechRecognizer` (`requiresOnDeviceRecognition = true`) + `AVSpeechSynthesizer` implementations
 * (`Dispatchers.Default`, `@OptIn(ExperimentalForeignApi::class)`; need a device to verify). The
 * ChatViewModel wiring (T017) already consumes the ports, so T014 is a drop-in.
 */
@ContributesTo(AppScope::class)
interface SpeechIosModule {
    companion object {
        @Provides
        fun provideSpeechToText(): SpeechToText = UnsupportedSpeechToText

        @Provides
        fun provideTextToSpeech(): TextToSpeech = NoOpTextToSpeech
    }
}
