package com.ampairs.file

import com.ampairs.common.di.AppScope
import com.ampairs.file.db.FileRoomDatabase
import com.ampairs.file.db.dao.FileDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface FileModule {
    companion object {
        @Provides
        fun provideFileDao(db: FileRoomDatabase): FileDao = db.fileDao()
    }
}
