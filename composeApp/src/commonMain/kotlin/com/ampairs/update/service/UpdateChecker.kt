package com.ampairs.update.service

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.update.api.UpdateApi
import com.ampairs.update.domain.UpdateCheckResult
import com.ampairs.update.domain.asDomainModel
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.hours

/**
 * Service to check for app updates with rate limiting
 *
 * Rate Limiting:
 * - Checks are rate-limited to once per 4 hours
 * - Can be forced with forceCheck = true
 * - Tracks last check time in DataStore
 */
class UpdateChecker(
    private val updateApi: UpdateApi,
    private val appPreferences: AppPreferencesDataStore,
) {
    companion object {
        // Rate limit: Check maximum once every 4 hours
        private val CHECK_INTERVAL = 4.hours.inWholeMilliseconds
    }

    /**
     * Check for updates with rate limiting
     *
     * @param forceCheck If true, bypasses rate limiting and checks immediately
     * @return UpdateCheckResult with update information, or null if rate limited
     */
    suspend fun checkForUpdates(forceCheck: Boolean = false): UpdateCheckResult? {
        // Get last check time
        val lastCheckTime = appPreferences.getLastUpdateCheckTime().first()
        val currentTime = getCurrentTimeMillis()
        val timeSinceLastCheck = currentTime - lastCheckTime

        // Check if we should skip due to rate limiting
        if (!forceCheck && lastCheckTime > 0 && timeSinceLastCheck < CHECK_INTERVAL) {
            val remainingMinutes = ((CHECK_INTERVAL - timeSinceLastCheck) / 1000 / 60)
            println("⏰ Update check rate limited. Last check: ${timeSinceLastCheck / 1000 / 60}m ago. Next check in: ${remainingMinutes}m")
            return null
        }

        // Get current platform and version
        val platform = getCurrentPlatform()
        val currentVersion = AppVersion.VERSION_NAME
        val versionCode = AppVersion.VERSION_CODE

        println("🔍 Checking for updates...")
        println("   Platform: ${platform.platformCode}")
        println("   Current version: $currentVersion ($versionCode)")

        return try {
            // Call API to check for updates
            val response = updateApi.checkForUpdates(
                platform = platform.platformCode,
                currentVersion = currentVersion,
                versionCode = versionCode
            )

            // Handle response
            val responseData = response.data
            if (responseData != null && response.error == null) {
                val result = responseData.asDomainModel()

                if (result.updateAvailable) {
                    println("✅ Update available: ${result.updateInfo?.version}")
                    println("   Mandatory: ${result.updateInfo?.isMandatory}")
                    println("   Size: ${result.updateInfo?.fileSizeMb} MB")
                } else {
                    println("✅ App is up to date")
                }

                // Update last check time
                appPreferences.setLastUpdateCheckTime(currentTime)
                result
            } else {
                println("⚠️ Failed to check for updates: ${response.error}")
                null
            }
        } catch (e: Exception) {
            println("❌ Error checking for updates: ${e.message}")
            // Update last check time even on error to avoid hammering the server
            appPreferences.setLastUpdateCheckTime(currentTime)
            null
        }
    }

    /**
     * Check if user has dismissed a specific update version
     */
    suspend fun isUpdateDismissed(version: String): Boolean {
        return appPreferences.isUpdateVersionDismissed(version).first()
    }

    /**
     * Mark an update version as dismissed by the user
     */
    suspend fun dismissUpdate(version: String) {
        appPreferences.setUpdateVersionDismissed(version, true)
        println("👤 User dismissed update version: $version")
    }

    /**
     * Clear dismissed status for an update (useful when forcing a check)
     */
    suspend fun clearDismissedStatus(version: String) {
        appPreferences.setUpdateVersionDismissed(version, false)
    }

    /**
     * Get current time in milliseconds
     */
    private fun getCurrentTimeMillis(): Long {
        return currentTimeMillis()
    }
}

/**
 * Get current time in milliseconds - platform-specific implementation
 */
expect fun currentTimeMillis(): Long
