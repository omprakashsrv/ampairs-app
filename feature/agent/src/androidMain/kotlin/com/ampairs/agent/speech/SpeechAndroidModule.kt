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
import com.ampairs.agent.speech.vosk.VoskModelCatalog
import com.ampairs.agent.speech.vosk.VoskModelRegistry
import com.ampairs.agent.speech.vosk.VoskModelSet
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

        // Vosk model catalog (zip directory models, downloaded on demand) for the Android Vosk adapter.
        @Provides
        @SingleIn(AppScope::class)
        fun provideVoskModelSet(): VoskModelSet = VoskModelSet(VoskModelCatalog.models)

        // Android lets the OS pick the mic — no in-app device selection.
        @Provides
        @SingleIn(AppScope::class)
        fun provideAudioInputDeviceProvider(): AudioInputDeviceProvider = NoopAudioInputDeviceProvider()

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(
            context: Context,
            whisperRegistry: WhisperModelRegistry,
            voskRegistry: VoskModelRegistry,
        ): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(id = "native", label = "Device", engine = AndroidSpeechToText(context)),
            SttAdapterEntry(
                id = "whisper",
                label = "Whisper (offline)",
                engine = WhisperSttEngine(whisperRegistry, AndroidWhisperCppTranscriber(), AndroidAudioCapture()),
            ),
            // Vosk (offline): streams live partials, self-endpoints on silence. Model is a zip dir
            // downloaded on demand via VoskModelRegistry. Uses its own AndroidAudioCapture instance.
            SttAdapterEntry(
                id = "vosk",
                label = "Vosk (offline)",
                engine = VoskSpeechToText(voskRegistry, AndroidAudioCapture()),
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(context: Context): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = AndroidTextToSpeech(context)),
        )
    }
}
