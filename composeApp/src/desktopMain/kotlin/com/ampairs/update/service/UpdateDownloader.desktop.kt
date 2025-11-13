package com.ampairs.update.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Desktop implementation for downloading update files
 */
actual suspend fun downloadUpdateFileImpl(
    url: String,
    fileName: String,
    onProgress: (Float) -> Unit
): String? = withContext(Dispatchers.IO) {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000 // 5 minutes for large file downloads
            connectTimeoutMillis = 30_000  // 30 seconds to establish connection
            socketTimeoutMillis = 60_000   // 60 seconds between data packets
        }
    }

    try {
        // Get downloads directory or temp directory
        val downloadDir = getDownloadDirectory()
        val file = File(downloadDir, fileName)

        // Ensure parent directory exists
        file.parentFile?.mkdirs()

        // Download with progress tracking
        client.prepareGet(url).execute { response ->
            val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
            var downloadedBytes = 0L

            val channel: ByteReadChannel = response.bodyAsChannel()
            val fileOutputStream = file.outputStream()

            try {
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead > 0) {
                        fileOutputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Report progress
                        if (contentLength > 0) {
                            val progress = (downloadedBytes.toFloat() / contentLength.toFloat())
                            onProgress(progress)
                        }
                    }
                }
                fileOutputStream.flush()
                onProgress(1.0f)
                file.absolutePath
            } finally {
                fileOutputStream.close()
            }
        }
    } catch (e: Exception) {
        println("❌ Download error: ${e.message}")
        e.printStackTrace()
        null
    } finally {
        client.close()
    }
}

/**
 * Desktop implementation for checksum verification using SHA-256
 */
actual fun verifyChecksumImpl(filePath: String, expectedChecksum: String): Boolean {
    return try {
        val file = File(filePath)
        if (!file.exists()) {
            println("❌ File not found for checksum verification: $filePath")
            return false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val fileBytes = file.readBytes()
        val hash = digest.digest(fileBytes)
        val actualChecksum = hash.joinToString("") { "%02x".format(it) }

        actualChecksum.equals(expectedChecksum, ignoreCase = true)
    } catch (e: Exception) {
        println("❌ Checksum verification error: ${e.message}")
        false
    }
}

/**
 * Desktop implementation for file deletion
 */
actual fun deleteFileImpl(filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
            println("🗑️ Deleted file: $filePath")
        }
    } catch (e: Exception) {
        println("❌ Failed to delete file: ${e.message}")
    }
}

/**
 * Get platform-specific download directory
 */
private fun getDownloadDirectory(): String {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    return when {
        osName.contains("mac") || osName.contains("darwin") -> {
            // macOS: ~/Downloads
            "$userHome/Downloads"
        }
        osName.contains("win") -> {
            // Windows: %USERPROFILE%\Downloads
            "$userHome\\Downloads"
        }
        else -> {
            // Linux: ~/Downloads or /tmp
            val downloadsDir = File("$userHome/Downloads")
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                "$userHome/Downloads"
            } else {
                System.getProperty("java.io.tmpdir")
            }
        }
    }
}
