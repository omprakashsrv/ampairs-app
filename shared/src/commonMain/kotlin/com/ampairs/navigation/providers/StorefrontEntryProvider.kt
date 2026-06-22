package com.ampairs.navigation.providers

import EcomRoute
import Route
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.ecom.ui.management.StorefrontManagementScreen

/**
 * Entry provider for the merchant-side online-store (ecom storefront) setup screen.
 * "Preview store" launches the live customer storefront for the configured slug via [EcomRoute.Storefront].
 */
fun storefrontEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is Route.Storefront -> NavEntry(key) {
        StorefrontManagementScreen(
            onPreview = { slug -> backStack.add(EcomRoute.Storefront(slug)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    else -> null
}
