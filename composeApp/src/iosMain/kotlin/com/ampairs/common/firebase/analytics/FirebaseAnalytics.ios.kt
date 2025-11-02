package com.ampairs.common.firebase.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation of FirebaseAnalytics
 */
@OptIn(ExperimentalForeignApi::class)
actual class FirebaseAnalytics {
    actual fun logEvent(eventName: String, params: Map<String, Any>?) {
        val parameters = params?.mapValues { (_, value) ->
            when (value) {
                is String -> value
                is Int -> value
                is Long -> value
                is Double -> value
                is Boolean -> value
                is Float -> value
                else -> value.toString()
            }
        }
        FIRAnalytics.logEventWithName(eventName, parameters)
    }

    actual fun setUserProperty(name: String, value: String?) {
        FIRAnalytics.setUserPropertyString(value, name)
    }

    actual fun setUserId(userId: String?) {
        FIRAnalytics.setUserID(userId)
    }

    actual fun setCurrentScreen(screenName: String, screenClass: String?) {
        val params = mapOf(
            AnalyticsParams.SCREEN_NAME to screenName,
            AnalyticsParams.SCREEN_CLASS to (screenClass ?: screenName)
        )
        logEvent(AnalyticsEvents.SCREEN_VIEW, params)
    }

    actual fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        FIRAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
