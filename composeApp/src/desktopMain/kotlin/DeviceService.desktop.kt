import com.ampairs.auth.domain.DeviceInfo
import com.ampairs.common.DeviceService
import java.io.File
import java.net.InetAddress

class DesktopDeviceService() : DeviceService {

    private val deviceIdFile = getDeviceIdFile()

    override fun getDeviceInfo(): DeviceInfo {
        val deviceId = getDeviceId()
        val deviceName = generateDeviceName()
        val deviceType = "Desktop"
        val platform = getPlatform()
        val browser = "Desktop App"
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
        if (deviceIdFile.exists()) {
            return deviceIdFile.readText().trim()
        }

        val deviceId = generateDeviceId()
        deviceIdFile.parentFile?.mkdirs()
        deviceIdFile.writeText(deviceId)
        return deviceId
    }

    override fun generateDeviceId(): String {
        val fingerprint = generateFingerprint()
        return "DESKTOP_${fingerprint}_${System.currentTimeMillis()}"
    }

    override fun clearDeviceId() {
        if (deviceIdFile.exists()) {
            deviceIdFile.delete()
        }
    }

    private fun getDeviceIdFile(): File {
        val userHome = System.getProperty("user.home")
        val appDir = when (getPlatform()) {
            "Windows" -> File(userHome, "AppData\\Local\\Ampairs")
            "macOS" -> File(userHome, "Library/Application Support/Ampairs")
            else -> File(userHome, ".ampairs")
        }
        return File(appDir, "device_id")
    }

    private fun generateFingerprint(): String {
        val characteristics = listOf(
            System.getProperty("os.name", "unknown"),
            System.getProperty("os.version", "unknown"),
            System.getProperty("os.arch", "unknown"),
            System.getProperty("user.name", "unknown"),
            getHostname(),
            Runtime.getRuntime().availableProcessors().toString()
        ).joinToString("|")

        return characteristics.hashCode().toString(16).uppercase().take(12)
    }

    private fun generateDeviceName(): String {
        val hostname = getHostname()
        val os = getPlatform()
        return "$hostname ($os)"
    }

    private fun getPlatform(): String {
        val osName = System.getProperty("os.name", "").lowercase()
        return when {
            osName.contains("windows") -> "Windows"
            osName.contains("mac") -> "macOS"
            osName.contains("linux") -> "Linux"
            else -> "Unknown"
        }
    }

    private fun getOSVersion(): String {
        val osName = System.getProperty("os.name", "Unknown")
        val osVersion = System.getProperty("os.version", "Unknown")
        val osArch = System.getProperty("os.arch", "Unknown")
        return "$osName $osVersion ($osArch)"
    }

    private fun getHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "Desktop-${System.currentTimeMillis()}"
        }
    }

    private fun generateUserAgent(): String {
        val javaVersion = System.getProperty("java.version", "Unknown")
        val osInfo = getOSVersion()
        return "Ampairs Desktop App Java/$javaVersion ($osInfo)"
    }
}