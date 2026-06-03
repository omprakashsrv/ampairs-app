package com.ampairs.ecom

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.ecom.data.db.EcomRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface EcomDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideEcomDatabase(factory: WorkspaceAwareDatabaseFactory): EcomRoomDatabase =
            factory.createDatabase(
                klass = EcomRoomDatabase::class,
                moduleName = "ecom",
            )
    }
}
