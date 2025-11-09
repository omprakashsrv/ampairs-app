package com.ampairs.app

import android.app.Application
import com.ampairs.common.sentry.SentryManager
import initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Sentry early in the application lifecycle
        initializeSentry()

        val koinApplication = startKoin {
            androidContext(this@MainApp)
            androidLogger()
        }
        initKoin(koinApplication)
    }

    private fun initializeSentry() {
        SentryManager.initialize(
            dsn = "https://dfb15e7a55f3454ba18b1b4c5ab03a46@o4510332999106560.ingest.de.sentry.io/4510333325148240",
            environment = if (BuildConfig.DEBUG) "dev" else "production",
            enableDebug = BuildConfig.DEBUG
        )
    }

}