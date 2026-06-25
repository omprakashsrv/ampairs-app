package com.ampairs.agent.speech

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * iOS speech adapters: native `SFSpeechRecognizer` STT + `AVSpeechSynthesizer` TTS, contributed as
 * selectable lists for the settings sheet. Additional engines (e.g. an offline Whisper STT adapter)
 * are appended to these lists.
 */
@ContributesTo(AppScope::class)
interface SpeechIosModule {
    companion object {
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
