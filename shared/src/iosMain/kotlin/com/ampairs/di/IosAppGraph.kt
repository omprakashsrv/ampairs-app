package com.ampairs.di

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
import com.ampairs.formwidgets.contact.ContactPickerService
import com.ampairs.formwidgets.location.LocationService
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): IosAppGraph
    }
}

@ContributesTo(AppScope::class)
interface IosSharedPlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideHttpEngine(): HttpClientEngine = Darwin.create()

        @Provides @SingleIn(AppScope::class)
        fun provideDatabasePathProvider(): DatabasePathProvider = IosDatabasePathProvider()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceAwareDatabaseFactory(
            pathProvider: DatabasePathProvider
        ): WorkspaceAwareDatabaseFactory =
            WorkspaceAwareDatabaseFactory(pathProvider, DispatcherProvider.io)

        @Provides @SingleIn(AppScope::class)
        fun provideAppPreferences(): AppPreferencesDataStore =
            DataStoreAppPreferences(createAppDataStore())

        @Provides @SingleIn(AppScope::class)
        fun provideDeviceService(): DeviceService = IosDeviceService()

        @Provides @SingleIn(AppScope::class)
        fun provideLocationService(): LocationService = LocationService()

        @Provides @SingleIn(AppScope::class)
        fun provideContactPickerService(): ContactPickerService = ContactPickerService()

        @Provides @SingleIn(AppScope::class)
        fun provideImageLoader(
            engine: HttpClientEngine,
            tokenRepository: TokenRepository
        ): ImageLoader = generateImageLoader(engine, tokenRepository)
    }
}
