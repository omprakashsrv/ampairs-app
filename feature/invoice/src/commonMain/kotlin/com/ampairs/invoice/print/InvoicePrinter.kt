package com.ampairs.invoice.print

import androidx.compose.runtime.Composable

/**
 * Returns a platform print action. Invoke the returned lambda with a self-contained HTML string
 * to hand it to the OS print/preview pipeline:
 *  - Android: WebView → PrintManager
 *  - iOS: UIPrintInteractionController (markup formatter)
 *  - Desktop: writes a temp .html and opens it in the default browser for printing
 */
@Composable
expect fun rememberInvoicePrinter(): (html: String) -> Unit
