package com.ampairs.subscription.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.subscription.db.SubscriptionDatabase
import org.koin.dsl.module
import platform.UIKit.UIDevice

actual val subscriptionPlatformModule = module {
    // Use factory instead of single to ensure fresh database instances after workspace switch
    // DatabaseScopeManager handles actual singleton behavior per workspace
    factory<SubscriptionDatabase> {
        get<WorkspaceAwareDatabaseFactory>().createDatabase(
            klass = SubscriptionDatabase::class,
            moduleName = "subscription"
        )
    }
}

/**
 * Get iOS device ID - uses identifierForVendor
 */
actual fun getDeviceId(): String {
    return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown-ios-device"
}
