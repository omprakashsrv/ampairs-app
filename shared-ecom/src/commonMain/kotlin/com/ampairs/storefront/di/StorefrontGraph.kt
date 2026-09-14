package com.ampairs.storefront.di

import coil3.ImageLoader
import com.ampairs.auth.api.TokenRepository
import com.ampairs.sync.CentralSyncService
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * Platform-agnostic contract for the Storefront ecom app's root graph.
 *
 * The concrete `@DependencyGraph` is declared per platform (Android: `StorefrontAppGraph` with a
 * `Context` factory param; iOS: `StorefrontIosGraph` with a no-arg factory), because the AppScope
 * platform bindings differ. Everything the shared Compose layer ([com.ampairs.storefront.ui.StorefrontRoot])
 * touches is declared here, so the UI never depends on a platform graph type.
 *
 * Mirrors the `AppGraph` interface split in :shared, trimmed to the slim ecom feature set.
 */
interface StorefrontGraph : ViewModelGraph {
    val workspaceManager: StorefrontWorkspaceManager
    val tokenRepository: TokenRepository
    val imageLoader: ImageLoader
    val centralSyncService: CentralSyncService
}
