package com.ampairs.invoice.print

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberInvoicePrinter(): (html: String) -> Unit {
    val context = LocalContext.current
    // Retain WebViews until print finishes — the PrintDocumentAdapter keeps a weak link only.
    val retained = remember { mutableListOf<WebView>() }
    return remember(context) {
        { html ->
            val webView = WebView(context)
            retained.add(webView)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val adapter = view.createPrintDocumentAdapter("Tax Invoice")
                    printManager.print(
                        "Tax Invoice",
                        adapter,
                        PrintAttributes.Builder().build()
                    )
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }
}
