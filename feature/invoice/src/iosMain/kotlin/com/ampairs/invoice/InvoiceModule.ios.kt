package com.ampairs.invoice

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.invoice.db.InvoiceRoomDatabase
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_1_2
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_2_3
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_3_4
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_4_5
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_5_6
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface InvoiceIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideInvoiceDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): InvoiceRoomDatabase = factory.createDatabase<InvoiceRoomDatabase>(
            moduleName = "invoice",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(INVOICE_MIGRATION_1_2, INVOICE_MIGRATION_2_3, INVOICE_MIGRATION_3_4, INVOICE_MIGRATION_4_5, INVOICE_MIGRATION_5_6),
        ).also { closableRegistry.register { it.close() } }
    }
}
