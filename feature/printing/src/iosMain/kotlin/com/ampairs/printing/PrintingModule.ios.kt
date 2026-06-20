package com.ampairs.printing

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.transport.PrinterTransportFactory
import com.ampairs.printing.data.db.PrintingDatabase
import com.ampairs.printing.osprint.IosOsPrintTransport
import com.ampairs.printing.transport.network.NetworkTransport
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface PrintingIosModule {
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
         * iOS transport map: thermal ESC/POS over raw TCP, and inkjet/laser pages via AirPrint
         * (UIPrintInteractionController) under OS_PRINT. USB / classic Bluetooth need MFi-certified
         * hardware (ExternalAccessory) and are not supported.
         */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrinterTransportFactory(): PrinterTransportFactory =
            PrinterTransportFactory { profile ->
                when (profile.connectionType) {
                    ConnectionType.NETWORK -> NetworkTransport()
                    ConnectionType.OS_PRINT -> IosOsPrintTransport()
                    else -> null
                }
            }
    }
}
