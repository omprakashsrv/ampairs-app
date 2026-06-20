package com.ampairs.printing.core.transport

import com.ampairs.printing.core.model.PrinterProfile

/**
 * Resolves the right [PrinterTransport] for a printer profile. Implemented per platform and supplied
 * through Metro DI (not constructed in commonMain) because some channels need platform handles the
 * common code can't hold — Android USB/Bluetooth need a `Context`, desktop OS-print needs
 * `javax.print`. Returns null when the requested [PrinterProfile.connectionType] has no channel on
 * the current platform.
 */
fun interface PrinterTransportFactory {
    fun transportFor(profile: PrinterProfile): PrinterTransport?
}
