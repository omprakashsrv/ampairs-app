package com.ampairs.printing.core.transport

/**
 * Scans for printers reachable on the current device and returns what it finds. Implemented per
 * platform and supplied through Metro DI: Android enumerates USB printer-class devices and bonded
 * Bluetooth devices; desktop lists the Java Print Service printers; iOS returns nothing (AirPrint
 * uses the system picker at print time). Network/TCP printers are added by IP, not discovered here.
 */
fun interface PrinterDiscoverer {
    suspend fun discover(): List<DiscoveredPrinter>
}
