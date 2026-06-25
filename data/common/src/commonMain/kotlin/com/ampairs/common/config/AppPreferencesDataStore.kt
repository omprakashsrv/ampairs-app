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

    /**
     * Get whether user has seen subscription plan selection for a workspace
     */
    fun hasSeenPlanSelection(workspaceId: String): Flow<Boolean>

    /**
     * Mark that user has seen subscription plan selection for a workspace
     */
    suspend fun markPlanSelectionSeen(workspaceId: String)

    /**
     * Reset plan selection seen flag (for testing or re-onboarding)
     */
    suspend fun resetPlanSelectionSeen(workspaceId: String)

    /**
     * Save current subscription plan for workspace
     */
    suspend fun saveSubscriptionPlan(workspaceId: String, planCode: String)

    /**
     * Get saved subscription plan for workspace
     */
    fun getSavedSubscriptionPlan(workspaceId: String): Flow<String?>

    /**
     * Set flag to show upgrade prompt later
     */
    suspend fun setShouldShowUpgrade(workspaceId: String, shouldShow: Boolean)

    /**
     * Check if should show upgrade prompt
     */
    fun shouldShowUpgrade(workspaceId: String): Flow<Boolean>

    /**
     * Clear all subscription onboarding data for a workspace
     */
    suspend fun clearSubscriptionOnboardingData(workspaceId: String)

    // Tally ERP sync config (per-workspace, Desktop only)
    fun getTallyHost(workspaceSlug: String): Flow<String>
    suspend fun setTallyHost(workspaceSlug: String, host: String)
    fun getTallyPort(workspaceSlug: String): Flow<Int>
    suspend fun setTallyPort(workspaceSlug: String, port: Int)

    // Incremental sync watermarks: last seen ALTERID per entity type per workspace
    fun getTallyLastAlterId(workspaceSlug: String, entityType: String): Flow<Long>
    suspend fun setTallyLastAlterId(workspaceSlug: String, entityType: String, alterId: Long)

    /**
     * The user's decision on downloading the on-device AI assistant model.
     * `null` = not asked yet (show the consent prompt), `true` = consented (auto-download),
     * `false` = declined (stay rule-based, never re-prompt). App-wide (not per-workspace).
     */
    fun getLlmModelDownloadConsent(): Flow<Boolean?>
    suspend fun setLlmModelDownloadConsent(granted: Boolean)

    /**
     * The on-device AI model the user explicitly chose in the model manager, by
     * `ModelDescriptor.id`. `null` = no explicit choice; selection falls back to the RAM-gated
     * auto-pick. App-wide (not per-workspace).
     */
    fun getSelectedLlmModelId(): Flow<String?>
    suspend fun setSelectedLlmModelId(modelId: String?)

    /**
     * The last-known on-device AI model catalog (the backend manifest JSON), persisted after each
     * successful pull. Lets the app recognize, select, and load already-downloaded server models when
     * the backend is unreachable — the bundled fallback catalog uses different ids/file names, so
     * without this an offline device can't match a downloaded model to a descriptor. App-wide.
     */
    fun getCachedAgentModelCatalog(): Flow<String?>
    suspend fun setCachedAgentModelCatalog(json: String?)

    /**
     * The assistant speech adapter the user picked in settings, by adapter id (e.g. "native" /
     * "whisper"). `null` = use the platform default (first registered). App-wide (the available
     * adapters differ per platform, so this effectively selects per platform).
     */
    fun getSelectedSttAdapterId(): Flow<String?>
    suspend fun setSelectedSttAdapterId(id: String?)
    fun getSelectedTtsAdapterId(): Flow<String?>
    suspend fun setSelectedTtsAdapterId(id: String?)

    /**
     * The Whisper model the user picked for the offline Whisper STT adapter, by `ModelDescriptor.id`
     * (e.g. "whisper-base" / "whisper-tiny"). `null` = use the catalog default (first entry). App-wide;
     * the available models differ per platform (`.tflite` on mobile, ggml on desktop), so this
     * effectively selects per platform.
     */
    fun getSelectedWhisperModelId(): Flow<String?>
    suspend fun setSelectedWhisperModelId(id: String?)
}

/**
 * Default theme preference when none is stored
 */
const val DEFAULT_THEME_PREFERENCE = "SYSTEM"