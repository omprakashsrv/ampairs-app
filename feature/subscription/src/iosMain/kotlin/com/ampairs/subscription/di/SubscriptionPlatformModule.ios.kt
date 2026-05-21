package com.ampairs.subscription.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.subscription.db.SubscriptionDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import platform.UIKit.UIDevice

// Replaced Koin subscriptionPlatformModule (actual) for iOS.
// SubscriptionDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface SubscriptionIosModule {
    companion object {
        @Provides
        fun provideSubscriptionDatabase(
            factory: WorkspaceAwareDatabaseFactory
        ): SubscriptionDatabase = factory.createDatabase(
            klass = SubscriptionDatabase::class,
            moduleName = "subscription"
        )
    }
}

/**
 * Get iOS device ID — kept for non-DI usage if needed.
 */
fun getIosDeviceId(): String =
    UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown-ios-device"
