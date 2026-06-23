package com.ampairs

import com.ampairs.common.firebase.messaging.FirebaseMessaging

/**
 * Swift ⇄ Kotlin bridge for FCM on iOS (Phase 4 of push notifications).
 *
 * Unlike Android (where Firebase instantiates a `FirebaseMessagingService` that can read the root
 * graph via `AppGraphHolder`), iOS receives FCM token refreshes and notifications in `AppDelegate`
 * (Swift). Swift cannot reach the Metro-provided [FirebaseMessaging] singleton directly, so
 * `MainViewController` publishes the singleton here once the app graph is created, and `AppDelegate`
 * forwards events through [onFcmToken] / [onFcmMessage]. These then drive the same
 * `PushTokenRegistrar` refresh listener / message listener used on every platform.
 */
object FcmBridge {

    @Volatile
    private var messaging: FirebaseMessaging? = null

    /** Called from [MainViewController] after the app graph is built. */
    fun register(firebaseMessaging: FirebaseMessaging) {
        messaging = firebaseMessaging
    }

    /** Forward a new/refreshed FCM token from AppDelegate's `didReceiveRegistrationToken`. */
    fun onFcmToken(token: String) {
        messaging?.onTokenRefreshed(token)
    }

    /** Forward an incoming notification's userInfo from AppDelegate. */
    fun onFcmMessage(userInfo: Map<Any?, *>) {
        messaging?.onMessageReceived(userInfo)
    }
}
