package com.ampairs.subscription.di

import android.content.Context
import android.provider.Settings
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.subscription.db.SubscriptionDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

// Replaced Koin subscriptionPlatformModule (actual) for Android.
// SubscriptionDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface SubscriptionAndroidModule {
    companion object {
        @Provides
        fun provideSubscriptionDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context
        ): SubscriptionDatabase = factory.createAndroidDatabase(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "subscription"
        )
    }
}

/**
 * Get Android device ID — kept for non-DI usage if needed.
 */
fun getAndroidDeviceId(context: Context): String =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?: "unknown-android-device"
