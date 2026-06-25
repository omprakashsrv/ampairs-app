package com.ampairs.agent.speech

import com.ampairs.agent.speech.whisper.DesktopAudioCapture
import com.ampairs.agent.speech.whisper.DesktopWhisperTranscriber
import com.ampairs.agent.speech.whisper.WhisperModelCatalog
import com.ampairs.agent.speech.whisper.WhisperModelRegistry
import com.ampairs.agent.speech.whisper.WhisperModelSet
import com.ampairs.agent.speech.whisper.WhisperSttEngine
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Desktop has no native recognizer, so its single STT adapter is **offline Whisper** (whisper.cpp via
 * `whisper-jni`, ggml models). TTS stays empty (text-only output on desktop). The Whisper model set
 * feeds [WhisperModelRegistry] (download/selection over the shared `ModelManager`).
 */
@ContributesTo(AppScope::class)
interface SpeechDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(WhisperModelCatalog.ggml)

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(registry: WhisperModelRegistry): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(
                id = "whisper",
                label = "Whisper (offline)",
                engine = WhisperSttEngine(registry, DesktopWhisperTranscriber(), DesktopAudioCapture()),
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(): List<TtsAdapterEntry> = emptyList()
    }
}
