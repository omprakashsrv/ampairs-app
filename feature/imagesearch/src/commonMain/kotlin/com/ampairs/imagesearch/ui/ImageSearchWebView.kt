package com.ampairs.imagesearch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Headless (visually hidden) per-platform WebView that loads [url], injects [ImageScraperJs.SCRIPT],
 * and reports scraped result batches as raw JSON via [onResults] (parse with [ImageResultParser]).
 *
 * The user never sees this — the clean Compose grid renders the parsed results. It is kept in the
 * composition (not fully detached) so lazy content still loads; on Desktop the JavaFX engine must be
 * attached to a shown component to run, so the actual gives it a tiny/inconspicuous footprint.
 *
 * Recomposing with a new [url] triggers a fresh load + scrape. A blank [url] loads nothing.
 *
 * @param onError reports a load/scrape failure message.
 */
@Composable
expect fun ImageSearchWebView(
    url: String,
    onResults: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
)
