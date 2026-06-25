package com.ampairs.agent.speech

import com.ampairs.agent.speech.whisper.WhisperModelSet
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * iOS speech adapters: native `SFSpeechRecognizer` STT + `AVSpeechSynthesizer` TTS, contributed as
 * selectable lists for the settings sheet. The offline Whisper STT engine (LiteRT) is not wired on
 * iOS yet, so [WhisperModelSet] is empty here — the shared `WhisperModelRegistry` stays inert
 * (`hasModels = false`) and the settings model picker is hidden.
 */
@ContributesTo(AppScope::class)
interface SpeechIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(emptyList())

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(id = "native", label = "Device", engine = IosSpeechToText()),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = IosTextToSpeech()),
        )
    }
}
