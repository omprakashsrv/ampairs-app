package com.ampairs.order

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.order.db.OrderRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface OrderAndroidModule {
    companion object {
        @Provides
        fun provideOrderDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): OrderRoomDatabase =
            factory.createAndroidDatabase(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "order",
                migrations = emptyList()
            )
    }
}
