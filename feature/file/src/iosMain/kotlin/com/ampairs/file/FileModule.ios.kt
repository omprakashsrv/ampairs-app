package com.ampairs.file

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.file.manager.FileManager
import com.ampairs.file.manager.IosFileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface FileIosModule {
    companion object {
        @Provides
        fun provideFileManager(): FileManager = IosFileManager()
    }
}
