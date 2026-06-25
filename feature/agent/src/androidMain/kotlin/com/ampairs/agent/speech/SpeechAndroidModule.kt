package com.ampairs.agent.speech

import android.content.Context
import com.ampairs.agent.speech.whisper.WhisperModelCatalog
import com.ampairs.agent.speech.whisper.WhisperModelSet
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Android speech adapters: the device's native [SpeechRecognizer] STT (`EXTRA_PREFER_OFFLINE`, the
 * default) and [android.speech.tts.TextToSpeech].
 *
 * The offline **Whisper** STT adapter is intentionally NOT registered on Android right now: it ran on
 * a LiteRT `.tflite` graph whose in-graph decoder fails at runtime (`gather index out of bounds`), so
 * it could never produce a transcript. It's being reimplemented on whisper.cpp/ggml — see
 * `feature/agent/whisper-cpp-android.md`. Until that lands, Android exposes only the working "Device"
 * STT so users aren't handed a broken option. The [WhisperModelSet] is still provided so the
 * registry/settings infra stays intact; iOS and Desktop keep their (working) Whisper adapters.
 */
@ContributesTo(AppScope::class)
interface SpeechAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(WhisperModelCatalog.tflite)

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(context: Context): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(id = "native", label = "Device", engine = AndroidSpeechToText(context)),
            // "whisper" (LiteRT tflite) removed — broken decoder; returns via whisper.cpp (see recipe).
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(context: Context): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = AndroidTextToSpeech(context)),
        )
    }
}
