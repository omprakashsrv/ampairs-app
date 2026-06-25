package com.ampairs.agent.speech

import android.content.Context
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Android speech adapters: the device's native [SpeechRecognizer] STT (`EXTRA_PREFER_OFFLINE`) and
 * [android.speech.tts.TextToSpeech]. Contributed as selectable lists so the settings sheet can switch
 * engines; additional engines (e.g. an offline Whisper STT adapter) are appended to these lists.
 */
@ContributesTo(AppScope::class)
interface SpeechAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(context: Context): List<SttAdapterEntry> = listOf(
            SttAdapterEntry(id = "native", label = "Device", engine = AndroidSpeechToText(context)),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(context: Context): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = AndroidTextToSpeech(context)),
        )
    }
}
