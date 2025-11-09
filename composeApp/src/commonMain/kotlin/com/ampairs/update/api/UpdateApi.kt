package com.ampairs.update.api

import com.ampairs.common.model.Response

/**
 * API interface for checking app updates
 */
interface UpdateApi {

    /**
     * Check for available updates for the current platform
     *
     * @param platform Platform code (MACOS, WINDOWS, LINUX)
     * @param currentVersion Current app version (e.g., "1.0.0")
     * @param versionCode Current app version code
     * @return UpdateCheckResponse with update availability and details
     */
    suspend fun checkForUpdates(
        platform: String,
        currentVersion: String,
        versionCode: Int,
    ): Response<UpdateCheckResponse>

    /**
     * Download update file from the given URL
     * This is a simple wrapper - actual download with progress tracking
     * should be handled by the UpdateDownloader service
     */
    suspend fun downloadUpdate(downloadUrl: String): ByteArray
}
