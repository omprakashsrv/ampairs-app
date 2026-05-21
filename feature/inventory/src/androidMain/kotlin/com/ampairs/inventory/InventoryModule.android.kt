package com.ampairs.inventory

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.inventory.db.InventoryRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface InventoryAndroidModule {
    companion object {
        @Provides
        fun provideInventoryDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): InventoryRoomDatabase =
            factory.createAndroidDatabase(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "inventory",
                migrations = emptyList()
            )
    }
}
