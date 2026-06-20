package com.ampairs.printing.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.data.db.PrintJobDao
import com.ampairs.printing.data.db.PrintRoutingDao
import com.ampairs.printing.data.db.PrintTemplateDao
import com.ampairs.printing.data.db.PrinterDao
import com.ampairs.printing.data.db.PrintingDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/** Provides the printing DAOs from the workspace-scoped [PrintingDatabase]. */
@ContributesTo(WorkspaceScope::class)
interface PrintingDaoModule {
    companion object {
        @Provides
        fun providePrinterDao(db: PrintingDatabase): PrinterDao = db.printerDao()

        @Provides
        fun providePrintRoutingDao(db: PrintingDatabase): PrintRoutingDao = db.printRoutingDao()

        @Provides
        fun providePrintJobDao(db: PrintingDatabase): PrintJobDao = db.printJobDao()

        @Provides
        fun providePrintTemplateDao(db: PrintingDatabase): PrintTemplateDao = db.printTemplateDao()
    }
}
