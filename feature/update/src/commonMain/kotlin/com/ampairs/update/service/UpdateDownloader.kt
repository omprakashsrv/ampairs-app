package com.ampairs.update.service

import com.ampairs.update.domain.UpdateDownloadState
import com.ampairs.update.domain.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service to download update files with progress tracking
 *
 * Features:
 * - Progress tracking via StateFlow
 * - File download with streaming
 * - Checksum verification (if provided)
 * - Platform-specific file storage
 */
class UpdateDownloader {
    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    /**
     * Download update file to platform-specific location
     *
     * @param updateInfo Update information containing download URL
     * @param onProgress Callback for download progress (0.0 to 1.0)
     * @return Path to downloaded file, or null on failure
     */
    suspend fun downloadUpdate(
        updateInfo: UpdateInfo,
        onProgress: ((Float) -> Unit)? = null
    ): String? {
        println("📥 Starting update download...")
        println("   URL: ${updateInfo.downloadUrl}")
        println("   Size: ${updateInfo.fileSizeMb} MB")

        _downloadState.value = UpdateDownloadState.Downloading(0f)

        return try {
            // Use platform-specific download implementation
            val filePath = downloadUpdateFile(
                url = updateInfo.downloadUrl,
                fileName = getUpdateFileName(updateInfo),
                onProgress = { progress ->
                    _downloadState.value = UpdateDownloadState.Downloading(progress)
                    onProgress?.invoke(progress)
                }
            )

            if (filePath != null) {
                // Verify checksum if provided
                if (updateInfo.checksum != null) {
                    println("🔐 Verifying checksum...")
                    val isValid = verifyChecksum(filePath, updateInfo.checksum)
                    if (!isValid) {
                        println("❌ Checksum verification failed!")
                        _downloadState.value = UpdateDownloadState.Failed("Checksum verification failed")
                        deleteFile(filePath)
                        return null
                    }
                    println("✅ Checksum verified")
                }

                println("✅ Download complete: $filePath")
                _downloadState.value = UpdateDownloadState.Downloaded(filePath)
                filePath
            } else {
                println("❌ Download failed")
                _downloadState.value = UpdateDownloadState.Failed("Download failed")
                null
            }
        } catch (e: Exception) {
            println("❌ Download error: ${e.message}")
            _downloadState.value = UpdateDownloadState.Failed(e.message ?: "Unknown error")
            null
        }
    }

    /**
     * Cancel ongoing download
     */
    fun cancelDownload() {
        if (_downloadState.value is UpdateDownloadState.Downloading) {
            _downloadState.value = UpdateDownloadState.Idle
            println("⏹️ Download cancelled")
        }
    }

    /**
     * Reset download state
     */
    fun resetState() {
        _downloadState.value = UpdateDownloadState.Idle
    }

    /**
     * Generate appropriate file name based on platform and version
     */
    private fun getUpdateFileName(updateInfo: UpdateInfo): String {
        return when (updateInfo.platform.platformCode) {
            "MACOS" -> "Ampairs-${updateInfo.version}.dmg"
            "WINDOWS" -> "Ampairs-${updateInfo.version}.msi"
            "LINUX" -> "Ampairs-${updateInfo.version}.deb"
            else -> "Ampairs-${updateInfo.version}.bin"
        }
    }

    /**
     * Platform-specific download implementation
     * This is implemented using expect/actual pattern
     */
    private suspend fun downloadUpdateFile(
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): String? {
        return downloadUpdateFileImpl(url, fileName, onProgress)
    }

    /**
     * Verify file checksum
     * This is implemented using expect/actual pattern
     */
    private fun verifyChecksum(filePath: String, expectedChecksum: String): Boolean {
        return verifyChecksumImpl(filePath, expectedChecksum)
    }

    /**
     * Delete file
     * This is implemented using expect/actual pattern
     */
    private fun deleteFile(filePath: String) {
        deleteFileImpl(filePath)
    }
}

/**
 * Platform-specific download implementation
 */
expect suspend fun downloadUpdateFileImpl(
    url: String,
    fileName: String,
    onProgress: (Float) -> Unit
): String?

/**
 * Platform-specific checksum verification
 */
expect fun verifyChecksumImpl(filePath: String, expectedChecksum: String): Boolean

/**
 * Platform-specific file deletion
 */
expect fun deleteFileImpl(filePath: String)
