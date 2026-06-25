package com.ampairs.agent.speech

import com.ampairs.agent.speech.whisper.AudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.IosAudioCapture
import com.ampairs.agent.speech.whisper.IosWhisperTranscriber
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
 * iOS speech adapters: native `SFSpeechRecognizer` STT + `AVSpeechSynthesizer` TTS, plus the offline
 * **Whisper** STT adapter (whisper.cpp via Kotlin/Native cinterop — [IosWhisperTranscriber] +
 * [IosAudioCapture]). Contributed as selectable lists for the settings sheet; the Whisper model set
 * (ggml) feeds the shared [WhisperModelRegistry].
 */
@ContributesTo(AppScope::class)
interface SpeechIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(WhisperModelCatalog.ggml)

        // iOS lets the OS pick the mic — no in-app device selection.
        @Provides
        @SingleIn(AppScope::class)
        fun provideAudioInputDeviceProvider(): AudioInputDeviceProvider = NoopAudioInputDeviceProvider()

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(registry: WhisperModelRegistry): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(id = "native", label = "Device", engine = IosSpeechToText()),
            SttAdapterEntry(
                id = "whisper",
                label = "Whisper (offline)",
                engine = WhisperSttEngine(
                    registry = registry,
                    transcriber = IosWhisperTranscriber(),
                    audio = IosAudioCapture(),
                ),
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = IosTextToSpeech()),
        )
    }
}
