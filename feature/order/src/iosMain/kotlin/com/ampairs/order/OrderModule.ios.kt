package com.ampairs.order

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.order.db.OrderRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface OrderIosModule {
    companion object {
        @Provides
        fun provideOrderDatabase(factory: WorkspaceAwareDatabaseFactory): OrderRoomDatabase =
            factory.createDatabase(klass = OrderRoomDatabase::class, moduleName = "order", migrations = emptyList())
    }
}
