package com.ampairs.di

import com.ampairs.aws.s3.IosS3Client
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.DeviceService
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.IosDatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.customer.ui.components.contact.ContactPickerService
import com.ampairs.customer.ui.components.location.LocationService
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
        fun provideS3Client(): S3Client = IosS3Client()

        @Provides @SingleIn(AppScope::class)
        fun provideLocationService(): LocationService = LocationService()

        @Provides @SingleIn(AppScope::class)
        fun provideContactPickerService(): ContactPickerService = ContactPickerService()
    }
}
