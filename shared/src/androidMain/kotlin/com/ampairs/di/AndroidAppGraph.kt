package com.ampairs.di

import android.content.Context
import com.ampairs.common.DeviceService
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import com.ampairs.common.database.AndroidDatabasePathProvider
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.customer.ui.components.contact.ContactPickerService
import com.ampairs.customer.ui.components.location.LocationService
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers

@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidAppGraph
    }
}

@ContributesTo(AppScope::class)
interface AndroidSharedPlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideHttpEngine(): HttpClientEngine = OkHttp.create()

        @Provides @SingleIn(AppScope::class)
        fun provideDatabasePathProvider(context: Context): DatabasePathProvider =
            AndroidDatabasePathProvider(context)

        @Provides @SingleIn(AppScope::class)
        fun provideWorkspaceAwareDatabaseFactory(
            pathProvider: DatabasePathProvider
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
    }
}
