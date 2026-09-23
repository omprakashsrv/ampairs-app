package com.ampairs.storefront.di

import com.ampairs.auth.domain.DeviceInfo
import com.ampairs.common.DeviceService
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiomPhone

/**
 * iOS [DeviceService] for the Storefront app. Mirrors `IosDeviceService` in :shared (which the
 * storefront app does not depend on). Used by the auth/device-management flow to identify this
 * install. Persists a generated device id in [NSUserDefaults].
 */
class IosStorefrontDeviceService : DeviceService {
    private val deviceIdKey = "ampairs_device_id"

    override fun getDeviceInfo(): DeviceInfo {
        val device = UIDevice.currentDevice
        return DeviceInfo(
            deviceId = getDeviceId(),
            deviceName = device.name,
            deviceType = when (device.userInterfaceIdiom) {
                UIUserInterfaceIdiomPad -> "Tablet"
                UIUserInterfaceIdiomPhone -> "Mobile"
                else -> "Mobile"
            },
            platform = "iOS",
            browser = "Mobile App",
            os = "iOS ${device.systemVersion}",
            userAgent = "Ampairs Storefront App iOS/${device.systemVersion} (${device.model})",
        )
    }

    override fun getDeviceId(): String = generateDeviceId()

    override fun generateDeviceId(): String {
        val defaults = NSUserDefaults.standardUserDefaults
        return defaults.stringForKey(deviceIdKey) ?: NSUUID().UUIDString.also {
            defaults.setObject(it, deviceIdKey)
        }
    }

    override fun clearDeviceId() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(deviceIdKey)
    }
}
