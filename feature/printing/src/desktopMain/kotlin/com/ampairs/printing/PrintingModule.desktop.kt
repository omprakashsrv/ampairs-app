package com.ampairs.printing

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.transport.PrintPermissions
import com.ampairs.printing.core.transport.PrinterDiscoverer
import com.ampairs.printing.core.transport.PrinterTransportFactory
import com.ampairs.printing.data.db.PrintingDatabase
import com.ampairs.printing.transport.network.NetworkTransport
import com.ampairs.printing.transport.osprint.createOsPrintTransport
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface PrintingDesktopModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrintingDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PrintingDatabase = factory.createDatabase<PrintingDatabase>(
            moduleName = "printing",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }

        /**
         * Desktop transport map: raw ESC/POS over TCP for thermal, and the Java Print Service
         * (`javax.print`) for inkjet/laser pages (OS_PRINT).
         */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrinterTransportFactory(): PrinterTransportFactory =
            PrinterTransportFactory { profile ->
                when (profile.connectionType) {
                    ConnectionType.NETWORK -> NetworkTransport()
                    ConnectionType.OS_PRINT -> createOsPrintTransport()
                    else -> null
                }
            }

        /** List the installed system printers via the Java Print Service (inkjet/laser). */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrinterDiscoverer(): PrinterDiscoverer =
            PrinterDiscoverer { createOsPrintTransport()?.discover().orEmpty() }

        /** Desktop printing (TCP / javax.print) needs no app-level runtime grant. */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrintPermissions(): PrintPermissions = PrintPermissions { true }
    }
}
