package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ampairs.common.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-based implementation for app preferences (theme, settings, configs)
 * Works across all platforms with proper file system persistence
 */
class DataStoreAppPreferences(
    private val dataStore: DataStore<Preferences>
) : AppPreferencesDataStore {

    companion object {
        private val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
        private val LANGUAGE_PREFERENCE_KEY = stringPreferencesKey("language_preference")
        private val LAST_UPDATE_CHECK_TIME_KEY = longPreferencesKey("last_update_check_time")
        private val LAST_WORKSPACE_ID_KEY = stringPreferencesKey("last_workspace_id")
        private val LAST_USER_ID_KEY = stringPreferencesKey("last_user_id")
        private val LLM_MODEL_DOWNLOAD_CONSENT_KEY = booleanPreferencesKey("llm_model_download_consent")
        private val CHAT_TELEMETRY_ENABLED_KEY = booleanPreferencesKey("chat_telemetry_enabled")
        private val ASSISTANT_REASONING_ENABLED_KEY = booleanPreferencesKey("assistant_reasoning_enabled")
        private val SELECTED_LLM_MODEL_ID_KEY = stringPreferencesKey("selected_llm_model_id")
        private val SELECTED_STT_ADAPTER_ID_KEY = stringPreferencesKey("selected_stt_adapter_id")
        private val SELECTED_TTS_ADAPTER_ID_KEY = stringPreferencesKey("selected_tts_adapter_id")
        private val SELECTED_WHISPER_MODEL_ID_KEY = stringPreferencesKey("selected_whisper_model_id")
        private val SELECTED_AUDIO_INPUT_DEVICE_ID_KEY = stringPreferencesKey("selected_audio_input_device_id")

        // Workspace-aware preference keys
        // Note: These keys include workspace slug to maintain separate state per workspace
        private fun getCustomerLastSyncTimeKey(workspaceSlug: String) =
            stringPreferencesKey("customer_last_sync_time_$workspaceSlug")

        private fun getFormConfigLastSyncTimeKey(workspaceSlug: String) =
            stringPreferencesKey("form_config_last_sync_time_$workspaceSlug")

        // Update-specific keys
        private fun getUpdateVersionDismissedKey(version: String) =
            stringPreferencesKey("update_version_dismissed_$version")

        // Subscription onboarding keys
        private fun getHasSeenPlanSelectionKey(workspaceId: String) =
            booleanPreferencesKey("has_seen_plan_selection_$workspaceId")

        private fun getSubscriptionPlanKey(workspaceId: String) =
            stringPreferencesKey("subscription_plan_$workspaceId")

        private fun getShouldShowUpgradeKey(workspaceId: String) =
            booleanPreferencesKey("should_show_upgrade_$workspaceId")

        // Tally ERP sync config
        private fun getTallyHostKey(ws: String) = stringPreferencesKey("tally_host_$ws")
        private fun getTallyPortKey(ws: String) = intPreferencesKey("tally_port_$ws")
        private fun getTallyAlterIdKey(ws: String, entity: String) =
            longPreferencesKey("tally_alter_id_${entity}_$ws")

        // Connector mirror cache (offline-first): raw JSON blobs of the backend connector metadata
        private fun getConnectorInstallationsKey(ws: String) =
            stringPreferencesKey("connector_installations_$ws")
        private fun getConnectorConfigKey(uid: String) =
            stringPreferencesKey("connector_config_$uid")
        // Notification preferences (device-local)
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val NOTIFY_ORDER_UPDATES_KEY = booleanPreferencesKey("notif_order_updates")
        private val NOTIFY_INVOICE_UPDATES_KEY = booleanPreferencesKey("notif_invoice_updates")
        private val NOTIFY_ANNOUNCEMENTS_KEY = booleanPreferencesKey("notif_announcements")
    }

    override fun getThemePreference(): Flow<ThemePreference> {
        return dataStore.data.map { preferences ->
            val preferenceString = preferences[THEME_PREFERENCE_KEY] ?: DEFAULT_THEME_PREFERENCE
            try {
                ThemePreference.valueOf(preferenceString)
            } catch (e: IllegalArgumentException) {
                println("⚠️ Invalid theme preference '$preferenceString', using default")
                ThemePreference.SYSTEM
            }
        }
    }

    override suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = preference.name
        }
    }

    override fun getLastUpdateCheckTime(): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[LAST_UPDATE_CHECK_TIME_KEY] ?: 0L // Default to 0 (never checked)
        }
    }

    override suspend fun setLastUpdateCheckTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_UPDATE_CHECK_TIME_KEY] = timestamp
        }
    }

    override fun isUpdateVersionDismissed(version: String): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            val key = getUpdateVersionDismissedKey(version)
            preferences[key]?.toBoolean() ?: false
        }
    }

    override suspend fun setUpdateVersionDismissed(version: String, dismissed: Boolean) {
        val key = getUpdateVersionDismissedKey(version)
        dataStore.edit { preferences ->
            preferences[key] = dismissed.toString()
        }
    }

    override fun getLastWorkspaceId(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[LAST_WORKSPACE_ID_KEY]
        }
    }

    override suspend fun setLastWorkspaceId(workspaceId: String?) {
        dataStore.edit { preferences ->
            if (workspaceId != null) {
                preferences[LAST_WORKSPACE_ID_KEY] = workspaceId
                println("💾 Last workspace ID saved: $workspaceId")
            } else {
                preferences.remove(LAST_WORKSPACE_ID_KEY)
                println("🧹 Last workspace ID cleared")
            }
        }
    }

    override suspend fun clearLastWorkspaceId() {
        dataStore.edit { preferences ->
            preferences.remove(LAST_WORKSPACE_ID_KEY)
            println("🧹 Last workspace ID cleared")
        }
    }

    override fun getLastUserId(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[LAST_USER_ID_KEY]
        }
    }

    override suspend fun setLastUserId(userId: String?) {
        dataStore.edit { preferences ->
            if (userId != null) {
                preferences[LAST_USER_ID_KEY] = userId
                println("💾 Last user ID saved: $userId")
            } else {
                preferences.remove(LAST_USER_ID_KEY)
                println("🧹 Last user ID cleared")
            }
        }
    }

    override suspend fun clearLastUserId() {
        dataStore.edit { preferences ->
            preferences.remove(LAST_USER_ID_KEY)
            println("🧹 Last user ID cleared")
        }
    }

    override fun getLanguagePreference(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[LANGUAGE_PREFERENCE_KEY] ?: "en" // Default to English
        }
    }

    override suspend fun setLanguagePreference(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_PREFERENCE_KEY] = languageCode
            println("🌐 Language preference saved: $languageCode")
        }
    }

    // Subscription onboarding methods
    override fun hasSeenPlanSelection(workspaceId: String): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            val key = getHasSeenPlanSelectionKey(workspaceId)
            preferences[key] ?: false
        }
    }

    override suspend fun markPlanSelectionSeen(workspaceId: String) {
        val key = getHasSeenPlanSelectionKey(workspaceId)
        dataStore.edit { preferences ->
            preferences[key] = true
            println("✅ Marked plan selection as seen for workspace: $workspaceId")
        }
    }

    override suspend fun resetPlanSelectionSeen(workspaceId: String) {
        val key = getHasSeenPlanSelectionKey(workspaceId)
        dataStore.edit { preferences ->
            preferences.remove(key)
            println("🔄 Reset plan selection seen flag for workspace: $workspaceId")
        }
    }

    override suspend fun saveSubscriptionPlan(workspaceId: String, planCode: String) {
        val key = getSubscriptionPlanKey(workspaceId)
        dataStore.edit { preferences ->
            preferences[key] = planCode
            println("💾 Saved subscription plan for workspace $workspaceId: $planCode")
        }
    }

    override fun getSavedSubscriptionPlan(workspaceId: String): Flow<String?> {
        return dataStore.data.map { preferences ->
            val key = getSubscriptionPlanKey(workspaceId)
            preferences[key]
        }
    }

    override suspend fun setShouldShowUpgrade(workspaceId: String, shouldShow: Boolean) {
        val key = getShouldShowUpgradeKey(workspaceId)
        dataStore.edit { preferences ->
            preferences[key] = shouldShow
            println("🔔 Set should show upgrade for workspace $workspaceId: $shouldShow")
        }
    }

    override fun shouldShowUpgrade(workspaceId: String): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            val key = getShouldShowUpgradeKey(workspaceId)
            preferences[key] ?: false
        }
    }

    override suspend fun clearSubscriptionOnboardingData(workspaceId: String) {
        dataStore.edit { preferences ->
            preferences.remove(getHasSeenPlanSelectionKey(workspaceId))
            preferences.remove(getSubscriptionPlanKey(workspaceId))
            preferences.remove(getShouldShowUpgradeKey(workspaceId))
            println("🧹 Cleared subscription onboarding data for workspace: $workspaceId")
        }
    }

    override fun getTallyHost(workspaceSlug: String): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[getTallyHostKey(workspaceSlug)] ?: ""
        }
    }

    override suspend fun setTallyHost(workspaceSlug: String, host: String) {
        dataStore.edit { preferences ->
            preferences[getTallyHostKey(workspaceSlug)] = host
        }
    }

    override fun getTallyPort(workspaceSlug: String): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[getTallyPortKey(workspaceSlug)] ?: 9008
        }
    }

    override suspend fun setTallyPort(workspaceSlug: String, port: Int) {
        dataStore.edit { preferences ->
            preferences[getTallyPortKey(workspaceSlug)] = port
        }
    }

    override fun getTallyLastAlterId(workspaceSlug: String, entityType: String): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[getTallyAlterIdKey(workspaceSlug, entityType)] ?: 0L
        }
    }

    override suspend fun setTallyLastAlterId(workspaceSlug: String, entityType: String, alterId: Long) {
        dataStore.edit { preferences ->
            preferences[getTallyAlterIdKey(workspaceSlug, entityType)] = alterId
        }
    }

    override fun getConnectorInstallationsJson(workspaceSlug: String): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[getConnectorInstallationsKey(workspaceSlug)]
        }
    }

    override suspend fun setConnectorInstallationsJson(workspaceSlug: String, json: String) {
        dataStore.edit { preferences ->
            preferences[getConnectorInstallationsKey(workspaceSlug)] = json
        }
    }

    override fun getConnectorConfigJson(installationUid: String): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[getConnectorConfigKey(installationUid)]
        }
    }

    override suspend fun setConnectorConfigJson(installationUid: String, json: String) {
        dataStore.edit { preferences ->
            preferences[getConnectorConfigKey(installationUid)] = json
        }
    }
    override fun getLlmModelDownloadConsent(): Flow<Boolean?> {
        return dataStore.data.map { preferences ->
            preferences[LLM_MODEL_DOWNLOAD_CONSENT_KEY]
        }
    }

    override suspend fun setLlmModelDownloadConsent(granted: Boolean) {
        dataStore.edit { preferences ->
            preferences[LLM_MODEL_DOWNLOAD_CONSENT_KEY] = granted
        }
    }

    override fun getChatTelemetryEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[CHAT_TELEMETRY_ENABLED_KEY] ?: false
        }
    }

    override suspend fun setChatTelemetryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CHAT_TELEMETRY_ENABLED_KEY] = enabled
        }
    }

    override fun getAssistantReasoningEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[ASSISTANT_REASONING_ENABLED_KEY] ?: true
        }
    }

    override suspend fun setAssistantReasoningEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ASSISTANT_REASONING_ENABLED_KEY] = enabled
        }
    }

    override fun getSelectedLlmModelId(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[SELECTED_LLM_MODEL_ID_KEY]
        }
    }

    override suspend fun setSelectedLlmModelId(modelId: String?) {
        dataStore.edit { preferences ->
            if (modelId != null) {
                preferences[SELECTED_LLM_MODEL_ID_KEY] = modelId
            } else {
                preferences.remove(SELECTED_LLM_MODEL_ID_KEY)
            }
        }
    }

    override fun getSelectedSttAdapterId(): Flow<String?> =
        dataStore.data.map { it[SELECTED_STT_ADAPTER_ID_KEY] }

    override suspend fun setSelectedSttAdapterId(id: String?) {
        dataStore.edit { preferences ->
            if (id != null) preferences[SELECTED_STT_ADAPTER_ID_KEY] = id
            else preferences.remove(SELECTED_STT_ADAPTER_ID_KEY)
        }
    }

    override fun getSelectedTtsAdapterId(): Flow<String?> =
        dataStore.data.map { it[SELECTED_TTS_ADAPTER_ID_KEY] }

    override suspend fun setSelectedTtsAdapterId(id: String?) {
        dataStore.edit { preferences ->
            if (id != null) preferences[SELECTED_TTS_ADAPTER_ID_KEY] = id
            else preferences.remove(SELECTED_TTS_ADAPTER_ID_KEY)
        }
    }

    override fun getSelectedModelId(namespace: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey("selected_model_id_$namespace")] }

    override suspend fun setSelectedModelId(namespace: String, id: String?) {
        val key = stringPreferencesKey("selected_model_id_$namespace")
        dataStore.edit { preferences ->
            if (id != null) preferences[key] = id else preferences.remove(key)
        }
    }

    override fun getSelectedWhisperModelId(): Flow<String?> =
        dataStore.data.map { it[SELECTED_WHISPER_MODEL_ID_KEY] }

    override suspend fun setSelectedWhisperModelId(id: String?) {
        dataStore.edit { preferences ->
            if (id != null) preferences[SELECTED_WHISPER_MODEL_ID_KEY] = id
            else preferences.remove(SELECTED_WHISPER_MODEL_ID_KEY)
        }
    }

    override fun getSelectedAudioInputDeviceId(): Flow<String?> =
        dataStore.data.map { it[SELECTED_AUDIO_INPUT_DEVICE_ID_KEY] }

    override suspend fun setSelectedAudioInputDeviceId(id: String?) {
        dataStore.edit { preferences ->
            if (id != null) preferences[SELECTED_AUDIO_INPUT_DEVICE_ID_KEY] = id
            else preferences.remove(SELECTED_AUDIO_INPUT_DEVICE_ID_KEY)
        }
    }

    // ---- Notification preferences (device-local; default ON) ----

    override fun getNotificationsEnabled(): Flow<Boolean> =
        dataStore.data.map { it[NOTIFICATIONS_ENABLED_KEY] ?: true }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    override fun getNotifyOrderUpdates(): Flow<Boolean> =
        dataStore.data.map { it[NOTIFY_ORDER_UPDATES_KEY] ?: true }

    override suspend fun setNotifyOrderUpdates(enabled: Boolean) {
        dataStore.edit { it[NOTIFY_ORDER_UPDATES_KEY] = enabled }
    }

    override fun getNotifyInvoiceUpdates(): Flow<Boolean> =
        dataStore.data.map { it[NOTIFY_INVOICE_UPDATES_KEY] ?: true }

    override suspend fun setNotifyInvoiceUpdates(enabled: Boolean) {
        dataStore.edit { it[NOTIFY_INVOICE_UPDATES_KEY] = enabled }
    }

    override fun getNotifyAnnouncements(): Flow<Boolean> =
        dataStore.data.map { it[NOTIFY_ANNOUNCEMENTS_KEY] ?: true }

    override suspend fun setNotifyAnnouncements(enabled: Boolean) {
        dataStore.edit { it[NOTIFY_ANNOUNCEMENTS_KEY] = enabled }
    }
}