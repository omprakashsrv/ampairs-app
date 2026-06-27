package com.ampairs.agent.llm

import android.content.Context
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File

/**
 * Android model storage. Path is `filesDir/agent_models` — the SAME directory [LiteRtLmEngine] loads
 * from, so a downloaded `.litertlm` file is found by the engine without any extra wiring.
 */
@ContributesTo(AppScope::class)
interface ModelStorageAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideModelStorage(context: Context): ModelStorage = object : ModelStorage {
            override fun modelsDirectoryPath(): String =
                File(context.filesDir, ModelStorage.DIR_NAME).absolutePath
        }

        /** Zip extractor for directory models (Vosk) on Android. */
        @Provides
        @SingleIn(AppScope::class)
        fun provideArchiveExtractors(): List<ArchiveExtractor> = listOf(JvmZipArchiveExtractor())
    }
}
