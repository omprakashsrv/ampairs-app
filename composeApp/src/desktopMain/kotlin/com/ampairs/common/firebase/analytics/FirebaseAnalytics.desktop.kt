package com.ampairs.common.firebase.analytics

/**
 * Desktop stub implementation of FirebaseAnalytics
 * Note: Firebase is not supported on Desktop platforms
 * All methods are no-ops
 */
class FirebaseAnalyticsImpl : FirebaseAnalytics {
    override fun logEvent(eventName: String, params: Map<String, Any>?) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseAnalytics (Desktop stub): logEvent($eventName, $params)")
    }

    override fun setUserProperty(name: String, value: String?) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseAnalytics (Desktop stub): setUserProperty($name, $value)")
    }

    override fun setUserId(userId: String?) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseAnalytics (Desktop stub): setUserId($userId)")
    }

    override fun setCurrentScreen(screenName: String, screenClass: String?) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseAnalytics (Desktop stub): setCurrentScreen($screenName, $screenClass)")
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseAnalytics (Desktop stub): setAnalyticsCollectionEnabled($enabled)")
    }
}
