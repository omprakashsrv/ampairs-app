package com.ampairs.invoice

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.invoice.db.InvoiceRoomDatabase
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_1_2
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_2_3
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_3_4
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface InvoiceAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideInvoiceDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): InvoiceRoomDatabase = factory.createAndroidDatabase<InvoiceRoomDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "invoice",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(INVOICE_MIGRATION_1_2, INVOICE_MIGRATION_2_3, INVOICE_MIGRATION_3_4),
        ).also { closableRegistry.register { it.close() } }
    }
}
