package com.ampairs.printing.permissions

import android.content.Context
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.transport.PrintPermissions
import dev.brewkits.grant.AppGrant
import dev.brewkits.grant.GrantFactory
import dev.brewkits.grant.GrantManager
import dev.brewkits.grant.GrantStatus

/**
 * Android runtime permissions for printing, via the Grant library (`dev.brewkits:grant`). Bluetooth
 * (classic SPP) needs BLUETOOTH_CONNECT/SCAN at runtime on API 31+; Grant manages the request without
 * an Activity binding. USB is granted per-device by the transport's own PendingIntent flow, and
 * NETWORK/OS_PRINT need no runtime grant — so those return true.
 */
class AndroidPrintPermissions(context: Context) : PrintPermissions {

    private val grantManager: GrantManager = GrantFactory.create(context)

    override suspend fun ensure(connectionType: ConnectionType): Boolean = when (connectionType) {
        ConnectionType.BLUETOOTH -> grantManager.request(AppGrant.BLUETOOTH) == GrantStatus.GRANTED
        else -> true
    }
}
