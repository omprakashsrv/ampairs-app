package com.ampairs.invoice

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.agent.InvoiceAgentDao
import com.ampairs.invoice.db.InvoiceRoomDatabase
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface InvoiceDaoModule {
    companion object {
        @Provides
        fun provideInvoiceDao(db: InvoiceRoomDatabase): InvoiceDao = db.invoiceDao()

        @Provides
        fun provideInvoiceItemDao(db: InvoiceRoomDatabase): InvoiceItemDao = db.invoiceItemDao()

        @Provides
        fun provideInvoiceAgentDao(db: InvoiceRoomDatabase): InvoiceAgentDao = db.invoiceAgentDao()
    }
}
