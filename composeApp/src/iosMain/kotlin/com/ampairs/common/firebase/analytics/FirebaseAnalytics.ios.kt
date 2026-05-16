package com.ampairs.common.firebase.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation of FirebaseAnalytics
 */
@OptIn(ExperimentalForeignApi::class)
class FirebaseAnalyticsImpl : FirebaseAnalytics {
    override fun logEvent(eventName: String, params: Map<String, Any>?) {
        val parameters: Map<Any?, Any?>? = params?.mapKeys { it.key as Any? }?.mapValues { (_, value) ->
            when (value) {
                is String -> value
                is Int -> value
                is Long -> value
                is Double -> value
                is Boolean -> value
                is Float -> value
                else -> value.toString()
            } as Any?
        }
        FIRAnalytics.logEventWithName(eventName, parameters)
    }

    override fun setUserProperty(name: String, value: String?) {
        FIRAnalytics.setUserPropertyString(value, name)
    }

    override fun setUserId(userId: String?) {
        FIRAnalytics.setUserID(userId)
    }

    override fun setCurrentScreen(screenName: String, screenClass: String?) {
        val params = mapOf(
            AnalyticsParams.SCREEN_NAME to screenName,
            AnalyticsParams.SCREEN_CLASS to (screenClass ?: screenName)
        )
        logEvent(AnalyticsEvents.SCREEN_VIEW, params)
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        FIRAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
