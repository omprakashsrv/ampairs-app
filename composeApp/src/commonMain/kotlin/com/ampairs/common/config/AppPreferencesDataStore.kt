package com.ampairs.common.config

import com.ampairs.common.theme.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * DataStore-based app preferences storage (theme, settings, configs)
 * This interface abstracts the DataStore implementation for easier testing and platform compatibility
 */
interface AppPreferencesDataStore {

    /**
     * Get the current theme preference as a Flow
     */
    fun getThemePreference(): Flow<ThemePreference>

    /**
     * Set the theme preference
     */
    suspend fun setThemePreference(preference: ThemePreference)

    /**
     * Get the last customer sync time as ISO 8601 string (yyyy-mm-ddTHH:mm:ss)
     */
    fun getCustomerLastSyncTime(): Flow<String>

    /**
     * Set the last customer sync time as ISO 8601 string (yyyy-mm-ddTHH:mm:ss)
     */
    suspend fun setCustomerLastSyncTime(timestamp: String)

    /**
     * Get the last form config sync time as ISO 8601 string (yyyy-mm-ddTHH:mm:ss)
     */
    fun getFormConfigLastSyncTime(): Flow<String>

    /**
     * Set the last form config sync time as ISO 8601 string (yyyy-mm-ddTHH:mm:ss)
     */
    suspend fun setFormConfigLastSyncTime(timestamp: String)

    /**
     * Get the last update check time as epoch milliseconds
     */
    fun getLastUpdateCheckTime(): Flow<Long>

    /**
     * Set the last update check time as epoch milliseconds
     */
    suspend fun setLastUpdateCheckTime(timestamp: Long)

    /**
     * Get whether the user dismissed a specific update version
     */
    fun isUpdateVersionDismissed(version: String): Flow<Boolean>

    /**
     * Set whether the user dismissed a specific update version
     */
    suspend fun setUpdateVersionDismissed(version: String, dismissed: Boolean)

    /**
     * Get the last selected workspace ID for auto-resume on app launch
     */
    fun getLastWorkspaceId(): Flow<String?>

    /**
     * Set the last selected workspace ID
     */
    suspend fun setLastWorkspaceId(workspaceId: String?)

    /**
     * Clear the last workspace ID (e.g., on logout)
     */
    suspend fun clearLastWorkspaceId()

    /**
     * Get the last selected user ID for auto-resume on app launch
     */
    fun getLastUserId(): Flow<String?>

    /**
     * Set the last selected user ID
     */
    suspend fun setLastUserId(userId: String?)

    /**
     * Clear the last user ID (e.g., when user is removed)
     */
    suspend fun clearLastUserId()

    /**
     * Get the current language preference (language code: "en", "hi")
     */
    fun getLanguagePreference(): Flow<String>

    /**
     * Set the language preference
     */
    suspend fun setLanguagePreference(languageCode: String)

    // Future app settings can be added here:
    // fun getNotificationSettings(): Flow<NotificationSettings>
    // suspend fun setNotificationSettings(settings: NotificationSettings)
}

/**
 * Default theme preference when none is stored
 */
const val DEFAULT_THEME_PREFERENCE = "SYSTEM"