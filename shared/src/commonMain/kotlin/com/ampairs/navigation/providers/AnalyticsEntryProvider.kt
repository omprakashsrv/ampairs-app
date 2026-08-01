package com.ampairs.navigation.providers

import Route
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.analytics.ui.dashboard.DashboardScreen

/**
 * Entry provider for the Analytics & Forecasting dashboard (feature 022). A single top-level screen,
 * so [Route.Analytics] is handled directly (no sub-route redirect) — the global header/nav owns the
 * chrome around it.
 */
fun analyticsEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is Route.Analytics -> NavEntry(key) { DashboardScreen() }
    else -> null
}
