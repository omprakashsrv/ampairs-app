package com.ampairs.common

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Provides access to the current Activity instance.
 *
 * This is needed for Firebase Phone Auth which requires Activity context for reCAPTCHA verification.
 * The Activity is stored as a WeakReference to avoid memory leaks.
 *
 * Usage:
 * - Call ActivityProvider.setActivity(this) in Activity.onCreate()
 * - Call ActivityProvider.clearActivity() in Activity.onDestroy()
 */
object ActivityProvider {
    private var activityRef: WeakReference<ComponentActivity>? = null

    fun setActivity(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
    }

    fun getActivity(): ComponentActivity? {
        return activityRef?.get()
    }

    fun clearActivity() {
        activityRef?.clear()
        activityRef = null
    }
}
