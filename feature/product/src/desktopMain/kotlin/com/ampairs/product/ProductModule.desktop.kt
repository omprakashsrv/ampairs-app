package com.ampairs.product

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.db.migrations.MIGRATION_1_2
import com.ampairs.product.db.migrations.MIGRATION_2_3
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface ProductDesktopModule {
    companion object {
        @Provides
        fun provideProductDatabase(factory: WorkspaceAwareDatabaseFactory): ProductRoomDatabase =
            factory.createDatabase(
                klass = ProductRoomDatabase::class,
                moduleName = "product",
                migrations = listOf(MIGRATION_1_2, MIGRATION_2_3)
            )
    }
}
