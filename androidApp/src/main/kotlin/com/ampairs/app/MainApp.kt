package com.ampairs.app

import android.app.Application
import com.ampairs.app.BuildConfig
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

        val isDebug = BuildConfig.DEBUG
        PlatformConfig.configure(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            environment = BuildConfig.ENVIRONMENT,
            isDebug = isDebug
        )

        initializeSentry(isDebug)

        appGraph = createGraphFactory<AndroidAppGraph.Factory>().create(this)
        AppGraphHolder.graph = appGraph
    }

    private fun initializeSentry(isDebug: Boolean) {
        SentryManager.initialize(
            dsn = "https://dfb15e7a55f3454ba18b1b4c5ab03a46@o4510332999106560.ingest.de.sentry.io/4510333325148240",
            environment = if (isDebug) "dev" else "production",
            enableDebug = isDebug
        )
    }
}
