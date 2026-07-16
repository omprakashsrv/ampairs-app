package com.ampairs.file

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.file.manager.DesktopFileManager
import com.ampairs.file.manager.FileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface FileDesktopModule {
    companion object {
        @Provides
        fun provideFileManager(): FileManager = DesktopFileManager()
    }
}
