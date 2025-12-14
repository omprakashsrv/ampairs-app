package com.ampairs.product

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.db.migrations.MIGRATION_1_2
import org.koin.core.module.Module
import org.koin.dsl.module

val productPlatformModule: Module = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<ProductRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = ProductRoomDatabase::class,
            moduleName = "product",
            migrations = listOf(MIGRATION_1_2)
        )
    }
}