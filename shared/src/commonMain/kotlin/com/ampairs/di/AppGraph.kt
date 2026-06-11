package com.ampairs.di

import coil3.ImageLoader
import com.ampairs.common.localization.LocaleManager
import com.ampairs.common.theme.ThemeManager
import com.ampairs.formwidgets.location.LocationService
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * Root application graph interface.
 * Exposes all dependencies that Compose code (entry providers, screens) needs to access.
 * Platform-specific @DependencyGraph implementations are in each platform source set.
 */
interface AppGraph : ViewModelGraph {

    // ── Infrastructure ────────────────────────────────────────────────────────
    val themeManager: ThemeManager
    val localeManager: LocaleManager
    val imageLoader: ImageLoader
    val locationService: LocationService

}
