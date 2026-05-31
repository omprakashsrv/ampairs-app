package com.ampairs.product

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.db.migrations.MIGRATION_1_2
import com.ampairs.product.db.migrations.MIGRATION_2_3
import com.ampairs.product.db.migrations.MIGRATION_3_4
import com.ampairs.product.db.migrations.MIGRATION_4_5
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface ProductAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideProductDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): ProductRoomDatabase =
            factory.createAndroidDatabase(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "product",
                migrations = listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            )
    }
}
