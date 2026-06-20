package com.ampairs.printing

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrintPermissions
import com.ampairs.printing.core.transport.PrinterDiscoverer
import com.ampairs.printing.core.transport.PrinterTransportFactory
import com.ampairs.printing.data.db.PrintingDatabase
import com.ampairs.printing.osprint.AndroidOsPrintTransport
import com.ampairs.printing.permissions.AndroidPrintPermissions
import com.ampairs.printing.transport.bluetooth.BluetoothThermalTransport
import com.ampairs.printing.transport.network.NetworkTransport
import com.ampairs.printing.transport.usb.UsbThermalTransport
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(WorkspaceScope::class)
interface PrintingAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrintingDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PrintingDatabase = factory.createAndroidDatabase<PrintingDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "printing",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }

        /**
         * Android transport map: thermal ESC/POS over raw TCP (:9100), USB (UsbManager) and classic
         * Bluetooth SPP (BluetoothAdapter/RFCOMM); inkjet/laser pages via the system print framework
         * (PrintManager) under OS_PRINT.
         */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrinterTransportFactory(context: Context): PrinterTransportFactory =
            PrinterTransportFactory { profile ->
                when (profile.connectionType) {
                    ConnectionType.NETWORK -> NetworkTransport()
                    ConnectionType.USB -> UsbThermalTransport(context, profile.address)
                    ConnectionType.BLUETOOTH -> BluetoothThermalTransport(context, profile.address)
                    ConnectionType.OS_PRINT -> AndroidOsPrintTransport(context)
                    else -> null
                }
            }

        /** Discover connected USB printer-class devices and bonded classic-Bluetooth devices. */
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrinterDiscoverer(context: Context): PrinterDiscoverer =
            PrinterDiscoverer {
                val usb = UsbThermalTransport(context, null).discover()
                val bluetooth = runCatching { BluetoothThermalTransport(context, null).discover() }
                    .getOrDefault(emptyList<DiscoveredPrinter>())
                usb + bluetooth
            }

        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrintPermissions(context: Context): PrintPermissions = AndroidPrintPermissions(context)
    }
}
