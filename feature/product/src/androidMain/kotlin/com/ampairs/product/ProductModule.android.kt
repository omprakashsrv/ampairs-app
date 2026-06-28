package com.ampairs.product

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.db.migrations.MIGRATION_1_2
import com.ampairs.product.db.migrations.MIGRATION_2_3
import com.ampairs.product.db.migrations.MIGRATION_3_4
import com.ampairs.product.db.migrations.MIGRATION_4_5
import com.ampairs.product.db.migrations.MIGRATION_5_6
import com.ampairs.product.db.migrations.MIGRATION_6_7
import com.ampairs.product.db.migrations.MIGRATION_7_8
import com.ampairs.product.db.migrations.MIGRATION_8_9
import com.ampairs.product.db.migrations.MIGRATION_9_10
import com.ampairs.product.db.migrations.MIGRATION_10_11
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface ProductAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideProductDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): ProductRoomDatabase = factory.createAndroidDatabase<ProductRoomDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "product",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11),
        ).also { closableRegistry.register { it.close() } }
    }
}
