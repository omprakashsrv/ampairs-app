package com.ampairs.printing.transport.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterStatus
import com.ampairs.printing.core.transport.PrinterTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

/**
 * Classic Bluetooth (SPP / RFCOMM) transport for ESC/POS thermal printers — the way virtually all
 * thermal printers expose Bluetooth. Uses the platform [BluetoothAdapter] + [BluetoothSocket]
 * because Jetpack `androidx.bluetooth` is BLE-only and does not support RFCOMM/SPP. Carries
 * [RenderedOutput.Bytes] (raw ESC/POS) written straight to the device's output stream.
 *
 * Requires `BLUETOOTH_CONNECT` (API 31+) / `BLUETOOTH` + `BLUETOOTH_ADMIN` (≤30) to be granted by
 * the UI before printing; a missing grant surfaces as a [Result.failure] from [open].
 */
@SuppressLint("MissingPermission")
class BluetoothThermalTransport(
    private val context: Context,
    private val address: String?,
) : PrinterTransport {

    override val connectionType: ConnectionType = ConnectionType.BLUETOOTH

    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override suspend fun discover(): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        val a = adapter() ?: return@withContext emptyList()
        a.bondedDevices.orEmpty().map { device ->
            DiscoveredPrinter(
                id = device.address,
                name = device.name ?: device.address,
                connectionType = ConnectionType.BLUETOOTH,
                address = device.address,
            )
        }
    }

    override suspend fun open(profile: PrinterProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val a = adapter() ?: error("Bluetooth is not available on this device")
            val target = (profile.address ?: address)?.trim().orEmpty()
            val device: BluetoothDevice = when {
                target.isNotEmpty() && BluetoothAdapter.checkBluetoothAddress(target) ->
                    a.getRemoteDevice(target)
                else ->
                    a.bondedDevices.orEmpty().firstOrNull()
                        ?: error("No paired Bluetooth printer found; pair one or pass its address")
            }
            a.cancelDiscovery() // discovery slows down / breaks an outgoing connection
            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()
            socket = sock
            output = sock.outputStream
        }
    }

    override suspend fun send(rendered: RenderedOutput): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val os = output ?: error("BluetoothThermalTransport.send called before open()")
            val bytes = (rendered as? RenderedOutput.Bytes)?.data
                ?: error("Bluetooth thermal transport requires RenderedOutput.Bytes")
            os.write(bytes)
            os.flush()
        }
    }

    override suspend fun queryStatus(profile: PrinterProfile): PrinterStatus =
        if (socket?.isConnected == true) PrinterStatus.Ready else PrinterStatus.Unknown("not connected")

    override suspend fun close() {
        runCatching { output?.flush() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        output = null
        socket = null
    }

    companion object {
        /** Serial Port Profile UUID — the standard RFCOMM service for ESC/POS Bluetooth printers. */
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
