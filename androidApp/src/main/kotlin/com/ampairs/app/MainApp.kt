package com.ampairs.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ampairs.app.BuildConfig
import com.ampairs.common.CurrentActivity
import com.ampairs.common.config.PlatformConfig
import com.ampairs.common.sentry.SentryManager
import com.ampairs.di.AndroidAppGraph
import com.ampairs.di.AppGraphHolder
import dev.zacsweers.metro.createGraphFactory

class MainApp : Application() {

    lateinit var appGraph: AndroidAppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        CurrentActivity.init(this)

        val isDebug = BuildConfig.DEBUG
        PlatformConfig.configure(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            environment = BuildConfig.ENVIRONMENT,
            isDebug = isDebug
        )

        initializeSentry(isDebug)

        appGraph = createGraphFactory<AndroidAppGraph.Factory>().create(this)
        AppGraphHolder.graph = appGraph

        createNotificationChannels()
    }

    /**
     * Creates the default FCM notification channel (required for Android 8.0+ to display
     * notifications). The id must match the `default_notification_channel_id` meta-data in the
     * manifest so FCM "notification" messages land here when the app is backgrounded.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun initializeSentry(isDebug: Boolean) {
        SentryManager.initialize(
            dsn = "https://dfb15e7a55f3454ba18b1b4c5ab03a46@o4510332999106560.ingest.de.sentry.io/4510333325148240",
            environment = if (isDebug) "dev" else "production",
            enableDebug = isDebug
        )
    }
}
