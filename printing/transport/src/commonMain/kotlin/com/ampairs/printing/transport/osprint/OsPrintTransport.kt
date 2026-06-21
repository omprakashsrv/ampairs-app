package com.ampairs.printing.transport.osprint

import com.ampairs.printing.core.transport.PrinterTransport

/**
 * Returns a transport that drives the host OS print spooler, or null on platforms that have no
 * direct OS-print channel yet. Desktop (JVM) backs this with the Java Print Service (`javax.print`),
 * giving raw byte (ESC/POS) pass-through and printer discovery on Windows/macOS/Linux. Android and
 * iOS return null for now — their OS print integrations (PrintManager / UIKit) are a follow-up.
 */
expect fun createOsPrintTransport(): PrinterTransport?
