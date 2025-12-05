package com.ampairs.tax

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.tax.data.db.TaxRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val taxPlatformModule: Module = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<TaxRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = TaxRoomDatabase::class,
            moduleName = "tax"
        )
    }
}