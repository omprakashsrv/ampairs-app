package com.ampairs.storefront.di

import android.os.Bundle
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.AnalyticsParams
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Android [FirebaseAnalytics] binding for the Storefront app. The full-app impl lives in :shared (not a
 * dependency here), so we re-declare the same thin adapter over the Firebase SDK.
 */
class StorefrontFirebaseAnalytics : FirebaseAnalytics {
    private val analytics = Firebase.analytics

    override fun logEvent(eventName: String, params: Map<String, Any>?) {
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

    override fun setUserProperty(name: String, value: String?) = analytics.setUserProperty(name, value)

    override fun setUserId(userId: String?) = analytics.setUserId(userId)

    override fun setCurrentScreen(screenName: String, screenClass: String?) = logEvent(
        AnalyticsEvents.SCREEN_VIEW,
        mapOf(
            AnalyticsParams.SCREEN_NAME to screenName,
            AnalyticsParams.SCREEN_CLASS to (screenClass ?: screenName),
        ),
    )

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) =
        analytics.setAnalyticsCollectionEnabled(enabled)
}
