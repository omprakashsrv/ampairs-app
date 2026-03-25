package com.ampairs.product

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.db.migrations.MIGRATION_1_2
import com.ampairs.product.db.migrations.MIGRATION_2_3
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val productPlatformModule: Module = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<ProductRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "product",
            migrations = listOf(MIGRATION_1_2, MIGRATION_2_3)
        )
    }
}