package com.ampairs.subscription.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.subscription.db.SubscriptionDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import platform.UIKit.UIDevice
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface SubscriptionIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideSubscriptionDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): SubscriptionDatabase = factory.createDatabase<SubscriptionDatabase>(
            moduleName = "subscription",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}

/**
 * Get iOS device ID — kept for non-DI usage if needed.
 */
fun getIosDeviceId(): String =
    UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown-ios-device"
