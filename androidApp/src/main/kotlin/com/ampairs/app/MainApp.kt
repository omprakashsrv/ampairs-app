package com.ampairs.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.ampairs.common.config.PlatformConfig
import com.ampairs.common.sentry.SentryManager
import initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        PlatformConfig.configure(
            apiBaseUrl = if (isDebug) "http://10.50.51.11:8080" else "https://api.ampairs.in",
            environment = if (isDebug) "dev" else "production",
            isDebug = isDebug
        )

        initializeSentry(isDebug)

        val koinApplication = startKoin {
            androidContext(this@MainApp)
            androidLogger()
        }
        initKoin(koinApplication)
    }

    private fun initializeSentry(isDebug: Boolean) {
        SentryManager.initialize(
            dsn = "https://dfb15e7a55f3454ba18b1b4c5ab03a46@o4510332999106560.ingest.de.sentry.io/4510333325148240",
            environment = if (isDebug) "dev" else "production",
            enableDebug = isDebug
        )
    }

}