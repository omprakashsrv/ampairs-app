package com.ampairs.agent.speech

import android.content.Context
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Android speech bindings (T013): real [SpeechRecognizer]-backed STT (`EXTRA_PREFER_OFFLINE`) +
 * [android.speech.tts.TextToSpeech]-backed TTS, both taking the app [Context] from the graph. The
 * `RECORD_AUDIO` runtime permission gate (T016) is a host-app concern; without it STT emits an error
 * and the UI degrades to text input.
 */
@ContributesTo(AppScope::class)
interface SpeechAndroidModule {
    companion object {
        @Provides
        fun provideSpeechToText(context: Context): SpeechToText = AndroidSpeechToText(context)

        @Provides
        fun provideTextToSpeech(context: Context): TextToSpeech = AndroidTextToSpeech(context)
    }
}
