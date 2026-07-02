package com.ampairs.push

import co.touchlab.kermit.Logger
import com.ampairs.common.DeviceService
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.firebase.messaging.FcmDataKeys
import com.ampairs.common.firebase.messaging.FcmMessageTypes
import com.ampairs.common.firebase.messaging.FcmTopics
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.ampairs.common.firebase.messaging.RemoteMessage
import com.ampairs.common.notification.NotificationPreferencesManager
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.notification.data.repository.NotificationRepository
import com.ampairs.notification.domain.model.AppNotification
import com.ampairs.subscription.domain.model.DevicePlatform
import com.ampairs.subscription.repository.SubscriptionRepository
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private val log = Logger.withTag("PushTokenRegistrar")

/**
 * Registers this device's FCM push token with the backend (Phase 4 of FCM push notifications).
 *
 * Placement rationale: this class lives in `shared` (not `feature/subscription`) because it needs
 * three types from different layers:
 *  - [SubscriptionRepository] (feature/subscription) — `updatePushToken(...)` REST call
 *  - [FirebaseMessaging]      (shared/common/firebase) — FCM token + refresh listener
 *  - [DeviceService]          (feature/auth-api)       — stable device id
 * `feature/subscription` does NOT depend on `shared`, so [FirebaseMessaging] is not visible there.
 * `shared` depends on both feature modules, so all three resolve here.
 *
 * Scope: [WorkspaceScope]. [SubscriptionRepository] resolves through the workspace child graph
 * (its `SubscriptionDao` is workspace-scoped) and the backend `updatePushToken` call is workspace
 * aware (X-Workspace-ID header). [FirebaseMessaging] and [DeviceService] are `AppScope` singletons
 * inherited by the workspace graph.
 */
@Inject
@SingleIn(WorkspaceScope::class)
class PushTokenRegistrar(
    private val subscriptionRepository: SubscriptionRepository,
    private val firebaseMessaging: FirebaseMessaging,
    private val deviceService: DeviceService,
    private val notificationRepository: NotificationRepository,
    private val centralSyncService: CentralSyncService,
    private val notificationPreferences: NotificationPreferencesManager,
    closableRegistry: WorkspaceClosableRegistry,
) {
    // FCM tokens always use the "FCM" type — even on iOS the token is the FCM registration token
    // (APNs is bridged to FCM by the Firebase SDK), so the backend receives a single token kind.
    private val scope = CoroutineScope(SupervisorJob() + DispatcherProvider.io)

    init {
        // Cancel the scope when the workspace graph is torn down (workspace switch / logout).
        closableRegistry.register { scope.cancel() }
    }

    /**
     * Idempotent. Wires the FCM token-refresh listener (so a freshly minted token is re-registered)
     * and immediately registers the current token. Safe to call on every workspace activation.
     *
     * Platform is resolved from [DeviceService] ("Android"/"iOS"); it is informational only — the
     * backend stores a single FCM token kind regardless of platform.
     */
    fun start() {
        val platform = currentPlatform()
        log.i { "Starting push-token registration (platform=$platform)" }
        firebaseMessaging.setOnTokenRefreshListener { token ->
            scope.launch { register(token) }
        }
        // Phase 5: when a push arrives while the app is foregrounded, persist the notification
        // locally (so the bell/list update instantly) and trigger a pull to reconcile with the feed.
        firebaseMessaging.setOnMessageReceivedListener { message ->
            scope.launch { onForegroundMessage(message) }
        }
        scope.launch { register() }

        // Phase 6: keep the FCM "announcements" topic subscription in sync with the device-local
        // preference. Opting out unsubscribes from the broadcast topic so the server stops sending
        // announcement pushes to this device entirely.
        notificationPreferences.notifyAnnouncements
            .distinctUntilChanged()
            .onEach { subscribed -> syncAnnouncementsTopic(subscribed) }
            .launchIn(scope)
    }

    private suspend fun syncAnnouncementsTopic(subscribed: Boolean) {
        runCatching {
            if (subscribed) {
                firebaseMessaging.subscribeToTopic(FcmTopics.ANNOUNCEMENTS)
            } else {
                firebaseMessaging.unsubscribeFromTopic(FcmTopics.ANNOUNCEMENTS)
            }
        }.onFailure { log.w(it) { "Failed to update announcements topic subscription" } }
    }

    /**
     * Map an FCM [RemoteMessage] into the local notification store and trigger a feed pull.
     *
     * The notification uid comes from the data payload (`notification_uid`); without it we can't key
     * the row, so we only trigger a pull (which fetches the authoritative server row). Inserted rows
     * are marked synced=true — the server is authoritative for content; only flag changes are pushed.
     */
    private suspend fun onForegroundMessage(message: RemoteMessage) {
        runCatching {
            val data = message.data
            // Phase 6: honour the device-local preferences before surfacing anything.
            if (!isTypeEnabled(data[FcmDataKeys.TYPE])) {
                log.d { "Notification suppressed by user preference (type=${data[FcmDataKeys.TYPE]})" }
                return@runCatching
            }
            val uid = data["notification_uid"] ?: data[FcmDataKeys.ENTITY_ID]
            if (!uid.isNullOrBlank()) {
                notificationRepository.upsertFromPush(
                    AppNotification(
                        uid = uid,
                        type = data[FcmDataKeys.TYPE] ?: "",
                        title = data[FcmDataKeys.TITLE] ?: message.notification?.title ?: "",
                        body = data[FcmDataKeys.MESSAGE] ?: message.notification?.body ?: "",
                        dataPayload = data[FcmDataKeys.DEEP_LINK],
                        read = false,
                        active = true,
                    ),
                )
            }
            // Always pull so the feed (and badge) converge on the server's view.
            centralSyncService.emit(SyncEvent.TriggerPull(SyncEntity.NOTIFICATION))
        }.onFailure { log.w(it) { "Failed to handle foreground push message" } }
    }

    /**
     * Gate display on device-local preferences. The master switch wins; per-type toggles then
     * filter order/invoice/announcement pushes. Unknown types fall through the master switch only.
     */
    private suspend fun isTypeEnabled(type: String?): Boolean {
        if (!notificationPreferences.notificationsEnabled.first()) return false
        return when (type) {
            FcmMessageTypes.ORDER_UPDATE -> notificationPreferences.notifyOrderUpdates.first()
            FcmMessageTypes.INVOICE_UPDATE -> notificationPreferences.notifyInvoiceUpdates.first()
            FcmMessageTypes.SYSTEM_ANNOUNCEMENT -> notificationPreferences.notifyAnnouncements.first()
            else -> true
        }
    }

    private fun currentPlatform(): DevicePlatform =
        when (deviceService.getDeviceInfo().platform.lowercase()) {
            "ios" -> DevicePlatform.IOS
            else -> DevicePlatform.ANDROID
        }

    /** Fetch the current FCM token and push it to the backend. Never throws. */
    suspend fun register() {
        val token = firebaseMessaging.getToken()
        if (token.isNullOrBlank()) {
            log.i { "No FCM token available yet; skipping push-token registration" }
            return
        }
        register(token)
    }

    private suspend fun register(token: String) {
        if (token.isBlank()) return
        runCatching {
            val deviceId = deviceService.getDeviceId()
            subscriptionRepository.updatePushToken(deviceId, token, PUSH_TOKEN_TYPE)
                .onFailure { log.w(it) { "Failed to register push token with backend" } }
        }.onFailure { log.w(it) { "Push-token registration threw" } }
    }

    private companion object {
        const val PUSH_TOKEN_TYPE = "FCM"
    }
}
