package com.ampairs.storefront.di

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.ampairs.auth.domain.DeviceInfo
import com.ampairs.common.DeviceService

/**
 * Android [DeviceService] for the Storefront app. Mirrors the impl in :shared (which the storefront app
 * does not depend on). Used by the auth/device-management flow to identify this install.
 */
class AndroidDeviceService(private val context: Context) : DeviceService {
    private val deviceIdKey = "ampairs_device_id"

    override fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        deviceId = getDeviceId(),
        deviceName = generateDeviceName(),
        deviceType = if (isTablet()) "Tablet" else "Mobile",
        platform = "Android",
        browser = "Mobile App",
        os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        userAgent = "Ampairs Mobile App Android/${Build.VERSION.RELEASE} (${Build.MANUFACTURER}; ${Build.MODEL})",
    )

    override fun getDeviceId(): String = generateDeviceId()

    override fun generateDeviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    override fun clearDeviceId() {
        context.getSharedPreferences("ampairs_device", Context.MODE_PRIVATE)
            .edit { remove(deviceIdKey) }
    }

    private fun generateDeviceName(): String =
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private fun isTablet(): Boolean {
        val screenLayout = context.resources.configuration.screenLayout and
            Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}
