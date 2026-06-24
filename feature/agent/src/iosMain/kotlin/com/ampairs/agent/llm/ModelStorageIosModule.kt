package com.ampairs.agent.llm

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/** iOS model storage under the app's Documents directory. */
@ContributesTo(AppScope::class)
interface ModelStorageIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideModelStorage(): ModelStorage = object : ModelStorage {
            override fun modelsDirectoryPath(): String {
                val documents = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true,
                ).first() as String
                return "$documents/${ModelStorage.DIR_NAME}"
            }
        }
    }
}
