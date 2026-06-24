package com.ampairs.agent.llm

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File

/** Desktop model storage under the app's `~/.ampairs` data directory. */
@ContributesTo(AppScope::class)
interface ModelStorageDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideModelStorage(): ModelStorage = object : ModelStorage {
            override fun modelsDirectoryPath(): String =
                File(System.getProperty("user.home"), ".ampairs/${ModelStorage.DIR_NAME}").absolutePath
        }
    }
}
