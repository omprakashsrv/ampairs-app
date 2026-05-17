import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.ampairs.auth.domain.DeviceInfo
import com.ampairs.common.DeviceService

class AndroidDeviceService(private val context: Context) : DeviceService {
    private val deviceIdKey = "ampairs_device_id"

    override fun getDeviceInfo(): DeviceInfo {
        val deviceId = getDeviceId()
        val deviceName = generateDeviceName()
        val deviceType = getDeviceType()
        val platform = "Android"
        val browser = "Mobile App"
        val os = getOSVersion()
        val userAgent = generateUserAgent()

        return DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceType = deviceType,
            platform = platform,
            browser = browser,
            os = os,
            userAgent = userAgent
        )
    }

    override fun getDeviceId(): String {
        return generateDeviceId()
    }

    override fun generateDeviceId(): String {
        // Use Android's official app-specific device identifier
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun clearDeviceId() {
        val prefs = context.getSharedPreferences("ampairs_device", Context.MODE_PRIVATE)
        prefs.edit {
            remove(deviceIdKey)
        }
    }

    private fun generateDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return "$manufacturer $model"
    }

    private fun getDeviceType(): String {
        return if (isTablet()) "Tablet" else "Mobile"
    }

    private fun isTablet(): Boolean {
        val configuration = context.resources.configuration
        val screenLayout =
            configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun getOSVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    private fun generateUserAgent(): String {
        return "Ampairs Mobile App Android/${Build.VERSION.RELEASE} (${Build.MANUFACTURER}; ${Build.MODEL})"
    }
}