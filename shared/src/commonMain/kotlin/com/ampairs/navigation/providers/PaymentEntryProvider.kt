package com.ampairs.navigation.providers

import PaymentRoute
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.payment.ui.PaymentDashboardScreen

/**
 * Entry provider for Payment & Collection module routes (Navigation 3).
 * Returns a NavEntry for payment routes or null if the route doesn't match.
 */
fun paymentEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is PaymentRoute.Dashboard -> NavEntry(key) {
        PaymentDashboardScreen()
    }

    else -> null
}
