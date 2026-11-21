package com.ampairs.storefront.di

import coil3.ImageLoader
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.DeviceService
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.IosDatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.formwidgets.contact.ContactPickerService
import com.ampairs.formwidgets.location.LocationService
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS root app graph for the Storefront ecom app — the concrete `@DependencyGraph` implementing the
 * platform-agnostic [StorefrontGraph]. Mirrors `IosAppGraph` in :shared, trimmed to the slim ecom
 * feature set. Created in `StorefrontViewController` at launch.
 *
 * Because binding happens by classpath (`@ContributesTo(AppScope::class)`), the 25 business modules
 * that :shared aggregates are simply absent — no customer/product/invoice DBs are ever created.
 */
@DependencyGraph(AppScope::class)
interface StorefrontIosGraph : StorefrontGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): StorefrontIosGraph
    }
}

/**
 * iOS platform bindings. Mirrors `IosSharedPlatformModule` in :shared, trimmed to what the ecom +
 * auth path needs (no maps/printing/FCM providers). Firebase Analytics is a no-op on iOS here
 * (see [StorefrontNoopFirebaseAnalytics]).
 */
@ContributesTo(AppScope::class)
interface StorefrontIosPlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideHttpEngine(): HttpClientEngine = Darwin.create()

        @Provides @SingleIn(AppScope::class)
        fun provideDatabasePathProvider(): DatabasePathProvider = IosDatabasePathProvider()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceAwareDatabaseFactory(
            pathProvider: DatabasePathProvider,
        ): WorkspaceAwareDatabaseFactory =
            WorkspaceAwareDatabaseFactory(pathProvider, DispatcherProvider.io)

        @Provides @SingleIn(AppScope::class)
        fun provideAppPreferences(): AppPreferencesDataStore =
            DataStoreAppPreferences(createAppDataStore())

        @Provides @SingleIn(AppScope::class)
        fun provideDeviceService(): DeviceService = IosStorefrontDeviceService()

        @Provides @SingleIn(AppScope::class)
        fun provideLocationService(): LocationService = LocationService()

        @Provides @SingleIn(AppScope::class)
        fun provideContactPickerService(): ContactPickerService = ContactPickerService()

        @Provides @SingleIn(AppScope::class)
        fun provideFirebaseAnalytics(): FirebaseAnalytics = StorefrontNoopFirebaseAnalytics()

        @Provides @SingleIn(AppScope::class)
        fun provideImageLoader(
            engine: HttpClientEngine,
            tokenRepository: TokenRepository,
        ): ImageLoader = generateStorefrontImageLoader(engine, tokenRepository)
    }
}
