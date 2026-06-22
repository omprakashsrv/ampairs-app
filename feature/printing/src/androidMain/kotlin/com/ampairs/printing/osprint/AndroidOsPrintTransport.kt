package com.ampairs.printing.osprint

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ampairs.common.CurrentActivity
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
import kotlin.coroutines.resumeWithException

/**
 * Android inkjet / laser channel via the system print framework (`android.print` / [PrintManager]) —
 * the mobile counterpart to `javax.print` on desktop. A PAGE template renders to HTML
 * ([RenderedOutput.Markup]); we lay it out in an offscreen [WebView], build a `PrintDocumentAdapter`
 * and hand it to [PrintManager], which shows the system print UI (printer picker, copies, "Save as
 * PDF"). Requires a foreground Activity (via [CurrentActivity]).
 *
 * This is NOT for ESC/POS thermal printers — those use the raw USB / Bluetooth / TCP transports.
 */
@SuppressLint("StaticFieldLeak")
class AndroidOsPrintTransport(private val appContext: Context) : PrinterTransport {

    override val connectionType: ConnectionType = ConnectionType.OS_PRINT

    // Held only until PrintManager takes ownership of the adapter; not destroyed in close() because
    // the system pulls pages from the adapter after send() returns.
    private var webView: WebView? = null

    override suspend fun discover(): List<DiscoveredPrinter> = emptyList()

    override suspend fun open(profile: PrinterProfile): Result<Unit> = Result.success(Unit)

    override suspend fun send(rendered: RenderedOutput): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val html = (rendered as? RenderedOutput.Markup)?.html
                ?: error("Android OS print needs RenderedOutput.Markup (HTML page)")
            val activity = CurrentActivity.get()
                ?: error("No foreground Activity available to launch the print dialog")
            suspendCancellableCoroutine { cont ->
                val wv = WebView(activity)
                webView = wv
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        try {
                            val adapter = view.createPrintDocumentAdapter(PRINT_JOB_NAME)
                            val printManager =
                                activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                            printManager.print(
                                PRINT_JOB_NAME,
                                adapter,
                                PrintAttributes.Builder().build(),
                            )
                            if (cont.isActive) cont.resume(Unit)
                        } catch (e: Throwable) {
                            if (cont.isActive) cont.resumeWithException(e)
                        }
                    }
                }
                wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    }

    override suspend fun queryStatus(profile: PrinterProfile): PrinterStatus =
        PrinterStatus.Unknown("os-print is UI-driven")

    override suspend fun close() {
        // Drop our reference only — PrintManager retains the adapter (and thus the WebView) until the
        // job finishes, so destroying it here would corrupt an in-flight print.
        webView = null
    }

    companion object {
        private const val PRINT_JOB_NAME = "Ampairs Document"
    }
}
