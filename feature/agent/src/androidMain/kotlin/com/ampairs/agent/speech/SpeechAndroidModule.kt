package com.ampairs.agent.speech

import android.content.Context
import com.ampairs.agent.llm.ModelManager
import com.ampairs.agent.speech.whisper.AndroidAudioCapture
import com.ampairs.agent.speech.whisper.AndroidWhisperTranscriber
import com.ampairs.agent.speech.whisper.WhisperModelCatalog
import com.ampairs.agent.speech.whisper.WhisperModelRegistry
import com.ampairs.agent.speech.whisper.WhisperModelSet
import com.ampairs.agent.speech.whisper.WhisperSttEngine
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Android speech adapters: the device's native [SpeechRecognizer] STT (`EXTRA_PREFER_OFFLINE`, the
 * default) and [android.speech.tts.TextToSpeech], plus an offline **Whisper** STT adapter (LiteRT
 * `.tflite`). Contributed as selectable lists so the settings sheet can switch engines. The Whisper
 * model set feeds [WhisperModelRegistry] (download/selection over the shared `ModelManager`).
 */
@ContributesTo(AppScope::class)
interface SpeechAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(WhisperModelCatalog.tflite)

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(
            context: Context,
            registry: WhisperModelRegistry,
            modelManager: ModelManager,
        ): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(id = "native", label = "Device", engine = AndroidSpeechToText(context)),
            SttAdapterEntry(
                id = "whisper",
                label = "Whisper (offline)",
                engine = WhisperSttEngine(
                    registry = registry,
                    transcriber = AndroidWhisperTranscriber(modelManager, WhisperModelCatalog.tfliteFilters),
                    audio = AndroidAudioCapture(),
                ),
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(context: Context): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = AndroidTextToSpeech(context)),
        )
    }
}
