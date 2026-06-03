package com.ampairs.subscription.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.subscription.db.SubscriptionDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.prefs.Preferences
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface SubscriptionDesktopModule {
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
 * Get desktop device ID — kept for non-DI usage if needed.
 */
fun getDesktopDeviceId(): String {
    return try {
        val prefs = Preferences.userRoot().node("com/ampairs/subscription")
        val storedId = prefs.get("device_id", null)
        if (storedId != null) return storedId
        val deviceId = getHardwareBasedId()
        prefs.put("device_id", deviceId)
        deviceId
    } catch (e: Exception) {
        "desktop-${System.getProperty("user.name")}-${System.currentTimeMillis()}"
    }
}

private fun getHardwareBasedId(): String {
    return try {
        val networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
        val mac = networkInterface?.hardwareAddress
        if (mac != null) {
            "desktop-${mac.joinToString("") { String.format("%02x", it) }}"
        } else {
            "desktop-${System.getProperty("user.name")}-${System.getProperty("os.name")}"
        }
    } catch (e: Exception) {
        "desktop-${System.getProperty("user.name")}-unknown"
    }
}
