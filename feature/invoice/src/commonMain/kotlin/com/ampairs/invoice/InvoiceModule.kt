package com.ampairs.invoice

import com.ampairs.common.di.AppScope
import com.ampairs.invoice.db.InvoiceRoomDatabase
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface InvoiceDaoModule {
    companion object {
        @Provides
        fun provideInvoiceDao(db: InvoiceRoomDatabase): InvoiceDao = db.invoiceDao()

        @Provides
        fun provideInvoiceItemDao(db: InvoiceRoomDatabase): InvoiceItemDao = db.invoiceItemDao()
    }
}
