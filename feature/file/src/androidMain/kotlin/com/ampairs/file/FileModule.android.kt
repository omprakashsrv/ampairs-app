package com.ampairs.file

import android.content.Context
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.file.manager.AndroidFileManager
import com.ampairs.file.manager.FileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface FileAndroidModule {
    companion object {
        @Provides
        fun provideFileManager(context: Context): FileManager = AndroidFileManager(context)
    }
}
