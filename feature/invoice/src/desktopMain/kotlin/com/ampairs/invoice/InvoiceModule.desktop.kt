package com.ampairs.invoice

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.invoice.db.InvoiceRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val invoicePlatformModule: Module = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<InvoiceRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = InvoiceRoomDatabase::class,
            moduleName = "invoice",
            migrations = emptyList()
        )
    }
}
