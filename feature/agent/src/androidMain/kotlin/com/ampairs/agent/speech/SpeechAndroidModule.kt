package com.ampairs.agent.speech

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Android speech bindings. Placeholder fallbacks for now — T013 replaces these bodies with the real
 * `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE`) + `android.speech.tts.TextToSpeech` implementations
 * (which need `Context` from the platform graph and a device to verify). The ChatViewModel wiring
 * (T017) already consumes the [SpeechToText]/[TextToSpeech] ports, so T013 is a drop-in.
 */
@ContributesTo(AppScope::class)
interface SpeechAndroidModule {
    companion object {
        @Provides
        fun provideSpeechToText(): SpeechToText = UnsupportedSpeechToText

        @Provides
        fun provideTextToSpeech(): TextToSpeech = NoOpTextToSpeech
    }
}
