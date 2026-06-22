package com.ampairs.printing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders a self-contained HTML page in the platform's native web view — the *same engine the OS
 * print path uses* on each platform, so the page-template preview matches the printout:
 * Android `WebView` (PrintManager), iOS `WKWebView` (AirPrint), Desktop `JEditorPane` (javax.print).
 */
@Composable
expect fun HtmlPreview(html: String, modifier: Modifier = Modifier)
