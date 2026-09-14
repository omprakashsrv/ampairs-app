package com.ampairs.storefront.di

import com.ampairs.common.firebase.analytics.FirebaseAnalytics

/**
 * No-op iOS [FirebaseAnalytics] for the Storefront app.
 *
 * The storefront apps ship with Firebase **Auth** (phone sign-in, linked at the app link step via the
 * app Podfiles), but not the Firebase Analytics pod — keeping that cinterop out of `:shared-ecom`.
 * `LoginViewModel` (and other reused screens) inject a [FirebaseAnalytics], so we satisfy the binding
 * with this no-op. Swap this for a real `FIRAnalytics`-backed impl (add the `FirebaseAnalytics` pod to
 * the cocoapods block, mirror `FirebaseAnalyticsImpl` in :shared) if storefront analytics is wanted.
 */
class StorefrontNoopFirebaseAnalytics : FirebaseAnalytics {
    override fun logEvent(eventName: String, params: Map<String, Any>?) {}
    override fun setUserProperty(name: String, value: String?) {}
    override fun setUserId(userId: String?) {}
    override fun setCurrentScreen(screenName: String, screenClass: String?) {}
    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {}
}
