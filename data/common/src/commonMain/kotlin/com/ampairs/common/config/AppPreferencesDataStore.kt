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

    // The exact ledger name Tally uses for sales (varies by company setup — e.g. "Sales Account").
    // Must match the Tally ledger byte-for-byte or the voucher import fails with a LINEERROR.
    fun getTallySalesLedger(workspaceSlug: String): Flow<String>
    suspend fun setTallySalesLedger(workspaceSlug: String, ledgerName: String)

    // Incremental sync watermarks: last seen ALTERID per entity type per workspace
    fun getTallyLastAlterId(workspaceSlug: String, entityType: String): Flow<Long>
    suspend fun setTallyLastAlterId(workspaceSlug: String, entityType: String, alterId: Long)

    // Local invoice ids already pushed *into* Tally (per-workspace). Drives which invoices the Tally
    // push considers new, and lets the pull skip re-importing our own pushed vouchers (by REMOTEID).
    fun getTallyPushedInvoiceIds(workspaceSlug: String): Flow<Set<String>>
    suspend fun addTallyPushedInvoiceIds(workspaceSlug: String, invoiceIds: Set<String>)

    // Local payment-voucher uids already pushed *into* Tally as Receipt/Payment vouchers
    // (per-workspace) — the same self-authored-vouchers dedup guard as the invoice set above.
    fun getTallyPushedPaymentIds(workspaceSlug: String): Flow<Set<String>>
    suspend fun addTallyPushedPaymentIds(workspaceSlug: String, paymentIds: Set<String>)

    // The exact ledger names Tally uses for cash/bank (the Receipt/Payment counter-leg), varying by
    // company setup. CASH-mode vouchers post to the cash ledger; every other payment mode posts to
    // the bank ledger. Must match the Tally ledger byte-for-byte or the voucher import fails silently.
    fun getTallyCashLedger(workspaceSlug: String): Flow<String>
    suspend fun setTallyCashLedger(workspaceSlug: String, ledgerName: String)
    fun getTallyBankLedger(workspaceSlug: String): Flow<String>
    suspend fun setTallyBankLedger(workspaceSlug: String, ledgerName: String)

    /**
     * The user's decision on downloading the on-device AI assistant model.
     * `null` = not asked yet (show the consent prompt), `true` = consented (auto-download),
     * `false` = declined (stay rule-based, never re-prompt). App-wide (not per-workspace).
     */
    fun getLlmModelDownloadConsent(): Flow<Boolean?>
    suspend fun setLlmModelDownloadConsent(granted: Boolean)

    /**
     * Whether the user opted in to uploading assistant chat transcripts to the backend for quality
     * improvement. Defaults **false** (opt-in). App-wide (not per-workspace).
     */
    fun getChatTelemetryEnabled(): Flow<Boolean>
    suspend fun setChatTelemetryEnabled(enabled: Boolean)

    /**
     * Whether the assistant shows the on-device model's transient "thinking" (reasoning) text while
     * it reasons. Defaults **true** (current behaviour). App-wide (not per-workspace).
     */
    fun getAssistantReasoningEnabled(): Flow<Boolean>
    suspend fun setAssistantReasoningEnabled(enabled: Boolean)

    /**
     * The on-device AI model the user explicitly chose in the model manager, by
     * `ModelDescriptor.id`. `null` = no explicit choice; selection falls back to the RAM-gated
     * auto-pick. App-wide (not per-workspace).
     */
    fun getSelectedLlmModelId(): Flow<String?>
    suspend fun setSelectedLlmModelId(modelId: String?)

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

    /**
     * Generic per-engine selected-model id, namespaced (e.g. "vosk"). Lets any downloadable-model STT
     * engine persist its model choice without a dedicated key — the pluggable counterpart to the
     * Whisper-specific keys above. `null` = use the catalog default (first entry). App-wide.
     */
    fun getSelectedModelId(namespace: String): Flow<String?>
    suspend fun setSelectedModelId(namespace: String, id: String?)

    /**
     * Selected microphone input device id (Desktop only — the mixer name). Null → system default.
     * Other platforms ignore it (the OS picks the mic).
     */
    fun getSelectedAudioInputDeviceId(): Flow<String?>
    suspend fun setSelectedAudioInputDeviceId(id: String?)

    // ---- Notification preferences (device-local; no backend API) ----

    /** Master notification switch. When false, no notifications are surfaced. Default true. */
    fun getNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /** Per-type: order updates. Default true. */
    fun getNotifyOrderUpdates(): Flow<Boolean>
    suspend fun setNotifyOrderUpdates(enabled: Boolean)

    /** Per-type: invoice updates. Default true. */
    fun getNotifyInvoiceUpdates(): Flow<Boolean>
    suspend fun setNotifyInvoiceUpdates(enabled: Boolean)

    /** Per-type: announcements (also toggles the FCM "announcements" topic subscription). Default true. */
    fun getNotifyAnnouncements(): Flow<Boolean>
    suspend fun setNotifyAnnouncements(enabled: Boolean)
}

/**
 * Default theme preference when none is stored
 */
const val DEFAULT_THEME_PREFERENCE = "SYSTEM"