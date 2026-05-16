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
        try {
            // Use DataDirectoryManager for consistent cache location
            val baseCacheDir = com.ampairs.common.desktop.DataDirectoryManager.getCacheDir()
            val customerCacheDir = File(baseCacheDir, "customer_images")

            if (!customerCacheDir.exists()) {
                customerCacheDir.mkdirs()
            }

            customerCacheDir.absolutePath
        } catch (e: IllegalStateException) {
            // Fallback to user.home if data directory not set (initialization phase)
            val fallbackCacheDir = File(System.getProperty("user.home"), ".ampairs/cache/customer_images")
            fallbackCacheDir.mkdirs()
            println("WARNING: Using fallback customer cache directory: ${fallbackCacheDir.absolutePath}")
            fallbackCacheDir.absolutePath
        }
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