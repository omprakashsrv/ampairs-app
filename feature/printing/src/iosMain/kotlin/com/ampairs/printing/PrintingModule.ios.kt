package com.ampairs.printing

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.transport.PrintPermissions
import com.ampairs.printing.core.transport.PrinterDiscoverer
import com.ampairs.printing.core.transport.PrinterTransportFactory
import com.ampairs.printing.osprint.IosOsPrintTransport
import com.ampairs.printing.transport.network.NetworkTransport
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface PrintingIosModule {
    companion object {
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

        /** AirPrint shows the system printer picker at print time — nothing to enumerate up front. */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrinterDiscoverer(): PrinterDiscoverer = PrinterDiscoverer { emptyList() }

        /** TCP/AirPrint need no app-level runtime grant (local-network is handled via Info.plist). */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrintPermissions(): PrintPermissions = PrintPermissions { true }
    }
}
