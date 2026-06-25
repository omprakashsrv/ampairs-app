package com.ampairs.agent.speech

import com.ampairs.agent.speech.whisper.AudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.DesktopAudioCapture
import com.ampairs.agent.speech.whisper.DesktopAudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.DesktopWhisperTranscriber
import com.ampairs.agent.speech.whisper.WhisperModelCatalog
import com.ampairs.agent.speech.whisper.WhisperModelRegistry
import com.ampairs.agent.speech.whisper.WhisperModelSet
import com.ampairs.agent.speech.whisper.WhisperSttEngine
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Desktop has no native recognizer, so its single STT adapter is **offline Whisper** (whisper.cpp via
 * `whisper-jni`, ggml models). TTS stays empty (text-only output on desktop). The Whisper model set
 * feeds [WhisperModelRegistry] (download/selection over the shared `ModelManager`). The mic input
 * device is user-selectable ([DesktopAudioInputDeviceProvider]); the chosen device drives capture.
 */
@ContributesTo(AppScope::class)
interface SpeechDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(WhisperModelCatalog.ggml)

        @Provides
        @SingleIn(AppScope::class)
        fun provideAudioInputDeviceProvider(prefs: AppPreferencesDataStore): AudioInputDeviceProvider =
            DesktopAudioInputDeviceProvider(prefs)

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(
            registry: WhisperModelRegistry,
            prefs: AppPreferencesDataStore,
        ): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(
                id = "whisper",
                label = "Whisper (offline)",
                engine = WhisperSttEngine(registry, DesktopWhisperTranscriber(), DesktopAudioCapture(prefs)),
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = DesktopTextToSpeech()),
        )
    }
}
