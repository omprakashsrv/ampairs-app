package com.ampairs.ecom

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.ecom.data.db.EcomRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface EcomAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideEcomDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): EcomRoomDatabase =
            factory.createAndroidDatabase(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "ecom",
            )
    }
}
