package com.ampairs.di

import DesktopDeviceService
import coil3.ImageLoader
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.DeviceService
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.DesktopDatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.customer.ui.components.contact.ContactPickerService
import com.ampairs.customer.ui.components.location.LocationService
import com.ampairs.sync.CentralSyncService
import com.ampairs.update.service.UpdateChecker
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import kotlinx.coroutines.Dispatchers

@DependencyGraph(AppScope::class)
interface DesktopAppGraph : AppGraph {
    val centralSyncService: CentralSyncService
    val updateChecker: UpdateChecker
    val workspaceManager: WorkspaceManager
    val appPreferences: AppPreferencesDataStore

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): DesktopAppGraph
    }
}

@ContributesTo(AppScope::class)
interface DesktopSharedPlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideHttpEngine(): HttpClientEngine = CIO.create {
            requestTimeout = 15_000
            endpoint {
                connectTimeout = 10_000
                socketTimeout = 15_000
            }
        }

        @Provides @SingleIn(AppScope::class)
        fun provideDatabasePathProvider(): DatabasePathProvider = DesktopDatabasePathProvider()

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceAwareDatabaseFactory(
            pathProvider: DatabasePathProvider
        ): WorkspaceAwareDatabaseFactory =
            WorkspaceAwareDatabaseFactory(pathProvider, Dispatchers.IO)

        @Provides @SingleIn(AppScope::class)
        fun provideAppPreferences(): AppPreferencesDataStore =
            DataStoreAppPreferences(createAppDataStore())

        @Provides @SingleIn(AppScope::class)
        fun provideDeviceService(): DeviceService = DesktopDeviceService()

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
