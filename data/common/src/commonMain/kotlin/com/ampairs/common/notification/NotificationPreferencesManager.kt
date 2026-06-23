package com.ampairs.common.notification

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

/**
 * Device-local notification preferences (no backend API — Phase 6 of FCM push notifications).
 *
 * Reuses the single shared [AppPreferencesDataStore] (never a new DataStore instance). Exposes the
 * master switch plus per-type toggles as cold [Flow]s with suspend setters, mirroring
 * `ThemeRepository`/`ThemeManager`. App-scoped because these preferences are device-wide, not
 * per-workspace.
 *
 * The announcements toggle additionally drives the FCM "announcements" topic subscription — that
 * side-effect lives in `PushTokenRegistrar` (shared), which observes [notifyAnnouncements] (it can
 * see `FirebaseMessaging`, which this module cannot).
 */
@Inject
@SingleIn(AppScope::class)
class NotificationPreferencesManager(
    private val appPreferences: AppPreferencesDataStore,
) {
    /** Master switch — when false, no notification is surfaced/inserted. Default true. */
    val notificationsEnabled: Flow<Boolean> = appPreferences.getNotificationsEnabled()
    val notifyOrderUpdates: Flow<Boolean> = appPreferences.getNotifyOrderUpdates()
    val notifyInvoiceUpdates: Flow<Boolean> = appPreferences.getNotifyInvoiceUpdates()
    val notifyAnnouncements: Flow<Boolean> = appPreferences.getNotifyAnnouncements()

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        appPreferences.setNotificationsEnabled(enabled)

    suspend fun setNotifyOrderUpdates(enabled: Boolean) =
        appPreferences.setNotifyOrderUpdates(enabled)

    suspend fun setNotifyInvoiceUpdates(enabled: Boolean) =
        appPreferences.setNotifyInvoiceUpdates(enabled)

    suspend fun setNotifyAnnouncements(enabled: Boolean) =
        appPreferences.setNotifyAnnouncements(enabled)
}
