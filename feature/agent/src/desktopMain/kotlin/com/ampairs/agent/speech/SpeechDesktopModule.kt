package com.ampairs.agent.speech

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Desktop has no native on-device recognizer/synthesizer, so it contributes no speech adapters today
 * (the registry then reports STT/TTS unavailable → text-only). An offline Whisper STT adapter would
 * be appended here to give Desktop speech input.
 */
@ContributesTo(AppScope::class)
interface SpeechDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(): List<SttAdapterEntry> = emptyList()

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(): List<TtsAdapterEntry> = emptyList()
    }
}
