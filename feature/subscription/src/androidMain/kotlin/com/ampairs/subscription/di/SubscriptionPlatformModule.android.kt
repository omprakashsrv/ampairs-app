package com.ampairs.subscription.di

import android.provider.Settings
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.subscription.db.SubscriptionDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

actual val subscriptionPlatformModule = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<SubscriptionDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "subscription"
        )
    }
}

/**
 * Get Android device ID
 */
actual fun getDeviceId(): String {
    val context = KoinPlatform.getKoin().get<android.content.Context>()
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-android-device"
}
