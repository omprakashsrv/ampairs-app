package com.ampairs.storefront

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import coil3.compose.setSingletonImageLoaderFactory
import com.ampairs.storefront.di.StorefrontGraphHolder
import com.ampairs.storefront.di.StorefrontIosGraph
import com.ampairs.storefront.ui.DefaultSeedColor
import com.ampairs.storefront.ui.StorefrontRoot
import dev.zacsweers.metro.createGraphFactory
import platform.UIKit.UIViewController

/**
 * iOS entry point for the customer storefront ecom apps. Both iOS apps call this from Swift:
 *
 * - **marketplaceApp/iosApp** (multi-store directory): `StorefrontViewController(workspaceSlug: nil,
 *   seedColorArgb: 0)` → directory mode (storefront picker; each store's brand color applied on
 *   selection).
 * - **clientApp/iosApp** (white-label, pinned to one store): passes the client's workspace slug and
 *   brand color (ARGB as a Long, e.g. `0xFF1B6C4A`), read from the app's Info.plist so one framework
 *   serves every client — mirrors the Android `-Pclient=<id>` build.
 *
 * `FirebaseApp.configure()` is called by the Swift `AppDelegate` before this runs (the storefront
 * framework doesn't link the FirebaseCore cinterop; the app Podfile provides the Firebase pods).
 *
 * @param workspaceSlug the pinned storefront/workspace slug, or `null` for the multi-store directory.
 * @param seedColorArgb 32-bit ARGB brand seed color as a Long; `0` → the default Ampairs green.
 */
fun StorefrontViewController(
    workspaceSlug: String?,
    seedColorArgb: Long,
): UIViewController = ComposeUIViewController {
    val graph = remember { createGraphFactory<StorefrontIosGraph.Factory>().create() }
    remember(graph) { StorefrontGraphHolder.graph = graph; graph }

    setSingletonImageLoaderFactory { _ -> graph.imageLoader }

    val seedColor = if (seedColorArgb != 0L) Color(seedColorArgb) else DefaultSeedColor

    StorefrontRoot(
        graph = graph,
        workspaceSlug = workspaceSlug,
        seedColor = seedColor,
    )
}
