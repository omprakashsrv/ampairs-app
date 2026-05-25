package com.ampairs.update.service

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.update.api.UpdateApi
import dev.zacsweers.metro.Inject
import com.ampairs.update.domain.UpdateCheckResult
import com.ampairs.update.domain.asDomainModel
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * Service to check for app updates with rate limiting
 *
 * Rate Limiting:
 * - Checks are rate-limited to once per 4 hours
 * - Can be forced with forceCheck = true
 * - Tracks last check time in DataStore
 */
@Inject
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
    @OptIn(ExperimentalTime::class)
    suspend fun checkForUpdates(forceCheck: Boolean = false): UpdateCheckResult? {
        // Get last check time
        val lastCheckTime = appPreferences.getLastUpdateCheckTime().first()
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val timeSinceLastCheck = currentTime - lastCheckTime

        // Check if we should skip due to rate limiting
        if (!forceCheck && lastCheckTime > 0 && timeSinceLastCheck < CHECK_INTERVAL) {
            return null
        }

        val platform = getCurrentPlatform()
        val currentVersion = AppVersion.VERSION_NAME
        val versionCode = AppVersion.VERSION_CODE

        return try {
            val response = updateApi.checkForUpdates(
                platform = platform.platformCode,
                currentVersion = currentVersion,
                versionCode = versionCode
            )

            val responseData = response.data
            if (responseData != null && response.error == null) {
                appPreferences.setLastUpdateCheckTime(currentTime)
                responseData.asDomainModel()
            } else {
                null
            }
        } catch (_: Exception) {
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
    }

    /**
     * Clear dismissed status for an update (useful when forcing a check)
     */
    suspend fun clearDismissedStatus(version: String) {
        appPreferences.setUpdateVersionDismissed(version, false)
    }

}
