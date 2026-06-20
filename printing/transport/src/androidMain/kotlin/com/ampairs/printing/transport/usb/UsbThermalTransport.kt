package com.ampairs.printing.transport.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterStatus
import com.ampairs.printing.core.transport.PrinterTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * USB transport for ESC/POS thermal printers using [UsbManager] bulk transfers. Finds a device that
 * exposes a USB printer interface (class 7), requests per-device permission via a [PendingIntent]
 * broadcast when needed, claims the interface and writes [RenderedOutput.Bytes] to the bulk-OUT
 * endpoint in chunks. The target is selected by [PrinterProfile.address] (the USB device name); a
 * blank address picks the first connected printer.
 */
class UsbThermalTransport(
    private val context: Context,
    private val deviceName: String?,
) : PrinterTransport {

    override val connectionType: ConnectionType = ConnectionType.USB

    private val usbManager: UsbManager
        get() = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var connection: UsbDeviceConnection? = null
    private var claimed: UsbInterface? = null
    private var endpoint: UsbEndpoint? = null

    override suspend fun discover(): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        usbManager.deviceList.values
            .filter { it.printerInterface() != null }
            .map { device ->
                DiscoveredPrinter(
                    id = device.deviceName,
                    name = device.productNameCompat() ?: device.deviceName,
                    connectionType = ConnectionType.USB,
                    address = device.deviceName,
                )
            }
    }

    override suspend fun open(profile: PrinterProfile): Result<Unit> {
        return runCatching {
            val target = (profile.address ?: deviceName)?.trim()
            val device = usbManager.deviceList.values.firstOrNull {
                if (target.isNullOrEmpty()) it.printerInterface() != null else it.deviceName == target
            } ?: error("USB printer not found")

            if (!usbManager.hasPermission(device) && !requestPermission(device)) {
                error("USB permission denied for ${device.deviceName}")
            }

            val iface = device.printerInterface() ?: error("Device has no USB printer interface")
            val outEndpoint = (0 until iface.endpointCount)
                .map { iface.getEndpoint(it) }
                .firstOrNull {
                    it.direction == UsbConstants.USB_DIR_OUT &&
                        it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                }
                ?: error("No bulk-OUT endpoint on the USB printer interface")

            val conn = usbManager.openDevice(device) ?: error("Unable to open the USB device")
            if (!conn.claimInterface(iface, true)) {
                conn.close()
                error("Unable to claim the USB printer interface")
            }
            connection = conn
            claimed = iface
            endpoint = outEndpoint
        }
    }

    override suspend fun send(rendered: RenderedOutput): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = connection ?: error("UsbThermalTransport.send called before open()")
            val ep = endpoint ?: error("No USB endpoint")
            val bytes = (rendered as? RenderedOutput.Bytes)?.data
                ?: error("USB thermal transport requires RenderedOutput.Bytes")
            var offset = 0
            while (offset < bytes.size) {
                val size = minOf(CHUNK, bytes.size - offset)
                val sent = conn.bulkTransfer(ep, bytes, offset, size, TIMEOUT_MS)
                if (sent < 0) error("USB bulkTransfer failed at offset $offset")
                offset += if (sent == 0) size else sent
            }
        }
    }

    override suspend fun queryStatus(profile: PrinterProfile): PrinterStatus =
        if (connection != null) PrinterStatus.Ready else PrinterStatus.Unknown("not opened")

    override suspend fun close() {
        runCatching { claimed?.let { connection?.releaseInterface(it) } }
        runCatching { connection?.close() }
        connection = null
        claimed = null
        endpoint = null
    }

    private suspend fun requestPermission(device: UsbDevice): Boolean =
        suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action != ACTION_USB_PERMISSION) return
                    runCatching { context.unregisterReceiver(this) }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (cont.isActive) cont.resume(granted)
                }
            }
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
            val pending = PendingIntent.getBroadcast(context, 0, intent, flags)
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
            usbManager.requestPermission(device, pending)
        }

    private fun UsbDevice.printerInterface(): UsbInterface? =
        (0 until interfaceCount)
            .map { getInterface(it) }
            .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER }

    private fun UsbDevice.productNameCompat(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) productName else null

    companion object {
        private const val ACTION_USB_PERMISSION = "com.ampairs.printing.USB_PERMISSION"
        private const val CHUNK = 16 * 1024
        private const val TIMEOUT_MS = 5_000
    }
}
