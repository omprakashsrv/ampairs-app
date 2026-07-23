package com.ampairs.navigation.providers

import EcomRoute
import Route
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.ecom.ui.management.EcomCustomerManagementScreen
import com.ampairs.ecom.ui.management.StorefrontManagementScreen

/**
 * Entry provider for the merchant-side online-store (ecom storefront) setup screen, and the
 * owner-facing "ecom users" management screen reachable from it.
 * "Preview store" launches the live customer storefront for the configured slug via [EcomRoute.Storefront].
 */
fun storefrontEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is Route.Storefront -> NavEntry(key) {
        StorefrontManagementScreen(
            onPreview = { slug -> backStack.add(EcomRoute.Storefront(slug)) },
            onManageUsers = { backStack.add(Route.EcomUsers) },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    is Route.EcomUsers -> NavEntry(key) {
        EcomCustomerManagementScreen(onBack = { backStack.removeLastOrNull() })
    }

    else -> null
}
