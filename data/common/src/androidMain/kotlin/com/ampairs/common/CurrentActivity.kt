package com.ampairs.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Tracks the foreground Activity automatically via Application.ActivityLifecycleCallbacks.
 * Call [init] once in Application.onCreate() — no manual set/clear in Activity needed.
 */
object CurrentActivity {

    @Volatile
    private var ref: WeakReference<ComponentActivity>? = null

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is ComponentActivity) ref = WeakReference(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (ref?.get() === activity) ref = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
    }

    fun get(): ComponentActivity? = ref?.get()
}
