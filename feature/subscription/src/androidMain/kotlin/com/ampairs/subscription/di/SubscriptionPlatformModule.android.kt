package com.ampairs.subscription.di

import android.content.Context
import android.provider.Settings
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.subscription.db.SubscriptionDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface SubscriptionAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideSubscriptionDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): SubscriptionDatabase = factory.createAndroidDatabase<SubscriptionDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "subscription",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}

/**
 * Get Android device ID — kept for non-DI usage if needed.
 */
fun getAndroidDeviceId(context: Context): String =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?: "unknown-android-device"
