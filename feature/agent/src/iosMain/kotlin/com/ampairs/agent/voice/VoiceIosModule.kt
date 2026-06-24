package com.ampairs.agent.voice

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * iOS voice-note binding — kodio's AVFoundation audio system needs no explicit initialization.
 * NOTE: the host iOS app's Info.plist must declare `NSMicrophoneUsageDescription` for recording.
 */
@ContributesTo(AppScope::class)
interface VoiceIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideVoiceNoteController(): VoiceNoteController = KodioVoiceNoteController()
    }
}
