package com.ampairs.subscription.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.subscription.db.SubscriptionDatabase
import org.koin.dsl.module
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.prefs.Preferences

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
 * Get desktop device ID - uses MAC address or generates a persistent ID
 */
actual fun getDeviceId(): String {
    return try {
        // Try to get stored device ID first
        val prefs = Preferences.userRoot().node("com/ampairs/subscription")
        val storedId = prefs.get("device_id", null)
        if (storedId != null) {
            return storedId
        }

        // Generate new device ID based on hardware
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
