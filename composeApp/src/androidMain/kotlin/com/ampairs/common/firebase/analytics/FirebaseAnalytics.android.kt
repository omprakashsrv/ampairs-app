package com.ampairs.common.firebase.analytics

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Android implementation of FirebaseAnalytics
 */
actual class FirebaseAnalytics {
    private val analytics = Firebase.analytics

    actual fun logEvent(eventName: String, params: Map<String, Any>?) {
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is Float -> bundle.putFloat(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(eventName, bundle)
    }

    actual fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    actual fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    actual fun setCurrentScreen(screenName: String, screenClass: String?) {
        val params = mapOf(
            AnalyticsParams.SCREEN_NAME to screenName,
            AnalyticsParams.SCREEN_CLASS to (screenClass ?: screenName)
        )
        logEvent(AnalyticsEvents.SCREEN_VIEW, params)
    }

    actual fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }
}
