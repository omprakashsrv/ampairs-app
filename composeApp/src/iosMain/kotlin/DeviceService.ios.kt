import com.ampairs.auth.domain.DeviceInfo
import com.ampairs.common.DeviceService
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiomPhone

class IosDeviceService : DeviceService {
    private val deviceIdKey = "ampairs_device_id"

    override fun getDeviceInfo(): DeviceInfo {
        val deviceId = getDeviceId()
        val deviceName = generateDeviceName()
        val deviceType = getDeviceType()
        val platform = "iOS"
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
        val defaults = NSUserDefaults.standardUserDefaults
        val existingId = defaults.stringForKey(deviceIdKey)

        return if (existingId != null) {
            existingId
        } else {
            val newId = NSUUID().UUIDString
            defaults.setObject(newId, deviceIdKey)
            newId
        }
    }

    override fun clearDeviceId() {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.removeObjectForKey(deviceIdKey)
    }

    private fun generateDeviceName(): String {
        val device = UIDevice.currentDevice
        return device.name
    }

    private fun getDeviceType(): String {
        val device = UIDevice.currentDevice
        return when (device.userInterfaceIdiom) {
            UIUserInterfaceIdiomPad -> "Tablet"
            UIUserInterfaceIdiomPhone -> "Mobile"
            else -> "Mobile"
        }
    }

    private fun getOSVersion(): String {
        val device = UIDevice.currentDevice
        return "iOS ${device.systemVersion}"
    }

    private fun generateUserAgent(): String {
        val device = UIDevice.currentDevice
        return "Ampairs Mobile App iOS/${device.systemVersion} (${device.model})"
    }
}