package com.ampairs.printing.core.transport

import com.ampairs.printing.core.model.ConnectionType

/**
 * Ensures any OS runtime permission required to use a connection is granted before printing/discovery.
 * Implemented per platform via Metro: Android requests Bluetooth (BLUETOOTH_CONNECT/SCAN on API 31+);
 * iOS/Desktop are no-ops (TCP/AirPrint/javax.print need no app-level runtime grant — local-network
 * access on iOS is handled by the OS prompt via Info.plist). USB is granted per-device by the
 * transport's own PendingIntent flow, so it returns true here.
 */
fun interface PrintPermissions {
    /** Returns true if the permission for [connectionType] is granted (or not required). */
    suspend fun ensure(connectionType: ConnectionType): Boolean
}
