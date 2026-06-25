package com.ampairs.agent.speech

import android.content.Context
import com.ampairs.agent.speech.whisper.AndroidAudioCapture
import com.ampairs.agent.speech.whisper.AndroidWhisperCppTranscriber
import com.ampairs.agent.speech.whisper.AudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.NoopAudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.WhisperModelCatalog
import com.ampairs.agent.speech.whisper.WhisperModelRegistry
import com.ampairs.agent.speech.whisper.WhisperModelSet
import com.ampairs.agent.speech.whisper.WhisperSttEngine
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Android speech adapters: the device's native [android.speech.SpeechRecognizer] STT
 * (`EXTRA_PREFER_OFFLINE`, the default) plus **offline Whisper** (whisper.cpp / ggml via the
 * `:whispercpp` JNI module — see [AndroidWhisperCppTranscriber]). TTS is the device
 * [android.speech.tts.TextToSpeech].
 *
 * Whisper uses the ggml catalog (the same models as Desktop). The earlier LiteRT `.tflite` path is
 * gone — its in-graph decoder failed at runtime; whisper.cpp does mel + decode natively.
 */
@ContributesTo(AppScope::class)
interface SpeechAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(WhisperModelCatalog.ggml)

        // Android lets the OS pick the mic — no in-app device selection.
        @Provides
        @SingleIn(AppScope::class)
        fun provideAudioInputDeviceProvider(): AudioInputDeviceProvider = NoopAudioInputDeviceProvider()

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(
            context: Context,
            registry: WhisperModelRegistry,
        ): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(id = "native", label = "Device", engine = AndroidSpeechToText(context)),
            SttAdapterEntry(
                id = "whisper",
                label = "Whisper (offline)",
                engine = WhisperSttEngine(registry, AndroidWhisperCppTranscriber(), AndroidAudioCapture()),
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(context: Context): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = AndroidTextToSpeech(context)),
        )
    }
}
