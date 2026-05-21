package com.ampairs.customer.di

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.AndroidFileManager
import com.ampairs.customer.data.repository.PlatformFileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface CustomerAndroidModule {
    companion object {
        @Provides
        fun provideCustomerDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): CustomerDatabase =
            factory.createAndroidDatabase(context = context, queryDispatcher = Dispatchers.IO, moduleName = "customer")

        @Provides
        fun providePlatformFileManager(context: Context): PlatformFileManager = AndroidFileManager(context)
    }
}
