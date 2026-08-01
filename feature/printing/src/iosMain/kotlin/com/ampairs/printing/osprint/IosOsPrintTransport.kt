package com.ampairs.printing.osprint

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterStatus
import com.ampairs.printing.core.transport.PrinterTransport
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIMarkupTextPrintFormatter
import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInfoOutputType
import platform.UIKit.UIPrintInteractionController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS inkjet / laser channel via AirPrint ([UIPrintInteractionController]) — the mobile counterpart
 * to `javax.print` / Android `PrintManager`. A PAGE template renders to HTML
 * ([RenderedOutput.Markup]) laid out by a [UIMarkupTextPrintFormatter]; [RenderedOutput.Pdf] prints
 * as a printing item. Presents the system AirPrint sheet.
 *
 * This is NOT for ESC/POS thermal printers — those need MFi hardware or the raw TCP transport.
 */
class IosOsPrintTransport : PrinterTransport {

    override val connectionType: ConnectionType = ConnectionType.OS_PRINT

    override suspend fun discover(): List<DiscoveredPrinter> = emptyList()

    override suspend fun open(profile: PrinterProfile): Result<Unit> = Result.success(Unit)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun send(rendered: RenderedOutput): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val controller = UIPrintInteractionController.sharedPrintController()
            val info = UIPrintInfo.printInfo()
            info.outputType = UIPrintInfoOutputType.UIPrintInfoOutputGeneral
            info.jobName = JOB_NAME
            controller.printInfo = info

            when (rendered) {
                is RenderedOutput.Markup ->
                    controller.printFormatter = UIMarkupTextPrintFormatter(markupText = rendered.html)
                is RenderedOutput.Pdf ->
                    controller.printingItem = rendered.data.toNSData()
                is RenderedOutput.Bytes ->
                    error("iOS OS print needs Markup/Pdf, not raw ESC/POS bytes")
            }

            suspendCancellableCoroutine { cont ->
                controller.presentAnimated(true) { _, completed, error ->
                    if (!cont.isActive) return@presentAnimated
                    when {
                        error != null ->
                            cont.resumeWithException(RuntimeException(error.localizedDescription))
                        else -> cont.resume(Unit)
                    }
                }
            }
        }
    }

    override suspend fun queryStatus(profile: PrinterProfile): PrinterStatus =
        PrinterStatus.Unknown("airprint is UI-driven")

    override suspend fun close() = Unit

    companion object {
        private const val JOB_NAME = "Ampairs Document"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData.create(bytes = null, length = 0u)
    } else {
        usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }
