package com.ampairs.agent.speech

import com.ampairs.agent.speech.vosk.VoskModelRegistry
import com.ampairs.agent.speech.whisper.WhisperModelRegistry
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Exposes the STT engines' downloadable-model registries keyed by **STT adapter id** (matching
 * [SttAdapterEntry.id], e.g. "whisper" / "vosk"). The chat settings drive whichever engine is selected
 * through this map, so adding a downloadable-model STT engine is just: a registry + an entry here.
 * Engines with no models on the current platform (empty model set) are omitted.
 */
@ContributesTo(AppScope::class)
interface SttModelRegistryModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideSttModelRegistries(
            whisper: WhisperModelRegistry,
            vosk: VoskModelRegistry,
        ): Map<String, DownloadableModelRegistry> = buildMap {
            if (whisper.hasModels) put("whisper", whisper)
            if (vosk.hasModels) put("vosk", vosk)
        }
    }
}
