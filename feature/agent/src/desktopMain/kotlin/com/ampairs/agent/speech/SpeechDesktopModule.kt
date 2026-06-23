package com.ampairs.agent.speech

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/** Desktop has no on-device recognizer/synthesizer → text-only fallback (T015). */
@ContributesTo(AppScope::class)
interface SpeechDesktopModule {
    companion object {
        @Provides
        fun provideSpeechToText(): SpeechToText = UnsupportedSpeechToText

        @Provides
        fun provideTextToSpeech(): TextToSpeech = NoOpTextToSpeech
    }
}
