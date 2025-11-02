package com.ampairs.customer.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Desktop-specific implementation of PlatformFileManager.
 * Handles file operations using the user's home directory cache.
 */
class DesktopFileManager : PlatformFileManager {

    override suspend fun saveImageToCache(imageId: String, imageData: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            val cacheDir = File(getCacheDirectory())
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val fileExtension = fileName.substringAfterLast('.', "")
            val cacheFileName = if (fileExtension.isNotEmpty()) {
                "${imageId}.$fileExtension"
            } else {
                imageId
            }

            val file = File(cacheDir, cacheFileName)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(imageData)
            }

            file.absolutePath
        }

    override suspend fun deleteFile(filePath: String): Unit = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    override suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).exists()
    }

    override suspend fun getFileSize(filePath: String): Long = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.length() else 0L
    }

    override suspend fun getCacheDirectory(): String = withContext(Dispatchers.IO) {
        val applicationName = "ampairs"
        val cacheDir = when (currentOperatingSystem) {
            OperatingSystem.Windows -> File(System.getenv("AppData"), "$applicationName/customer_images")
            OperatingSystem.Linux -> File(System.getProperty("user.home"), ".cache/$applicationName/customer_images")
            OperatingSystem.MacOS -> File(
                System.getProperty("user.home"),
                "Library/Caches/$applicationName/customer_images"
            )
            else -> throw IllegalStateException("Unsupported operating system")
        }

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        cacheDir.absolutePath
    }

    override suspend fun readFile(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }
        file.readBytes()
    }
}

// Helper to detect operating system (already exists in Koin.desktop.kt)
private enum class OperatingSystem {
    Windows, Linux, MacOS
}

private val currentOperatingSystem: OperatingSystem
    get() {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> OperatingSystem.Windows
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OperatingSystem.Linux
            osName.contains("mac") -> OperatingSystem.MacOS
            else -> throw IllegalStateException("Unsupported operating system: $osName")
        }
    }