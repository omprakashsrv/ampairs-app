package com.ampairs.agent.voice

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** Desktop voice-note binding — kodio's JVM audio system needs no explicit initialization. */
@ContributesTo(AppScope::class)
interface VoiceDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideVoiceNoteController(): VoiceNoteController = KodioVoiceNoteController()
    }
}
