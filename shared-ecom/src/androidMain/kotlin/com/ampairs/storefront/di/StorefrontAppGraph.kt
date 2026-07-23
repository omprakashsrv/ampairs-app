package com.ampairs.storefront.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.DeviceService
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import com.ampairs.common.database.AndroidDatabasePathProvider
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.httpClient
import com.ampairs.formwidgets.contact.ContactPickerService
import com.ampairs.formwidgets.location.LocationService
import com.ampairs.sync.CentralSyncService
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath

/**
 * Root app graph for the Storefront ecom app. Deliberately minimal: it binds only the AppScope
 * contributions reachable from the reused modules (auth, ecom, store, data.common, data.sync).
 * Because binding happens by classpath (`@ContributesTo(AppScope::class)`), the 25 business modules
 * that :shared aggregates are simply absent — no customer/product/invoice DBs are ever created.
 */
@DependencyGraph(AppScope::class)
interface StorefrontAppGraph : ViewModelGraph {
    val workspaceManager: StorefrontWorkspaceManager
    val tokenRepository: TokenRepository
    val imageLoader: ImageLoader
    val centralSyncService: CentralSyncService

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): StorefrontAppGraph
    }
}

/**
 * Android platform bindings. Mirrors `AndroidSharedPlatformModule` in :shared, trimmed to what the
 * ecom + auth path needs (no maps/printing/tally providers).
 */
@ContributesTo(AppScope::class)
interface StorefrontPlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideHttpEngine(): HttpClientEngine = OkHttp.create()

        @Provides @SingleIn(AppScope::class)
        fun provideDatabasePathProvider(context: Context): DatabasePathProvider =
            AndroidDatabasePathProvider(context)

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceAwareDatabaseFactory(
            pathProvider: DatabasePathProvider,
        ): WorkspaceAwareDatabaseFactory = WorkspaceAwareDatabaseFactory(pathProvider, Dispatchers.IO)

        @Provides @SingleIn(AppScope::class)
        fun provideAppPreferences(context: Context): AppPreferencesDataStore =
            DataStoreAppPreferences(createAppDataStore(context))

        @Provides @SingleIn(AppScope::class)
        fun provideDeviceService(context: Context): DeviceService = AndroidDeviceService(context)

        @Provides @SingleIn(AppScope::class)
        fun provideLocationService(context: Context): LocationService = LocationService(context)

        @Provides @SingleIn(AppScope::class)
        fun provideContactPickerService(): ContactPickerService = ContactPickerService()

        @Provides @SingleIn(AppScope::class)
        fun provideFirebaseAnalytics(): FirebaseAnalytics = StorefrontFirebaseAnalytics()

        @Provides @SingleIn(AppScope::class)
        fun provideImageLoader(
            context: Context,
            engine: HttpClientEngine,
            tokenRepository: TokenRepository,
        ): ImageLoader {
            val client = httpClient(engine, tokenRepository)
            return ImageLoader.Builder(context)
                .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("storefront_images_cache").toOkioPath())
                        .maxSizeBytes(100L * 1024 * 1024)
                        .build()
                }
                .components {
                    add(KtorNetworkFetcherFactory(client))
                    add(ImageCacheKeyer())
                }
                .crossfade(true)
                .build()
        }
    }
}
