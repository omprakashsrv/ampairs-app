package com.ampairs.agent.voice

import android.content.Context
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import space.kodio.core.AndroidAudioSystem

/**
 * Android voice-note binding. kodio's [AndroidAudioSystem] requires the application context before
 * any recording/playback, so we set it here when the singleton controller is first constructed.
 * The `RECORD_AUDIO` permission is already declared in the module manifest and gated at runtime by
 * the Grant-backed `MicPermissionController`.
 */
@ContributesTo(AppScope::class)
interface VoiceAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideVoiceNoteController(context: Context): VoiceNoteController {
            AndroidAudioSystem.setApplicationContext(context.applicationContext)
            return KodioVoiceNoteController()
        }
    }
}
