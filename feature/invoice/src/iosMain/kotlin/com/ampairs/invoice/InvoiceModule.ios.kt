package com.ampairs.invoice

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.invoice.db.InvoiceRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface InvoiceIosModule {
    companion object {
        @Provides
        fun provideInvoiceDatabase(factory: WorkspaceAwareDatabaseFactory): InvoiceRoomDatabase =
            factory.createDatabase(klass = InvoiceRoomDatabase::class, moduleName = "invoice", migrations = emptyList())
    }
}
