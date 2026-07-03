package com.ampairs.clientapp

import android.app.Application
import com.ampairs.common.CurrentActivity
import com.ampairs.common.config.PlatformConfig
import com.ampairs.storefront.di.StorefrontAppGraph
import com.ampairs.storefront.di.StorefrontGraphHolder
import dev.zacsweers.metro.createGraphFactory

/**
 * Application entry point shared by every white-label client build. Creates the slim
 * [StorefrontAppGraph] (auth + ecom + store only) and configures the API base URL from BuildConfig.
 * All client-specific values (applicationId, name, icons, workspace slug) are injected at build time
 * from clients/<id>/ — this class is identical for every client.
 */
class ClientApplication : Application() {

    lateinit var appGraph: StorefrontAppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        CurrentActivity.init(this)

        PlatformConfig.configure(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            environment = BuildConfig.ENVIRONMENT,
            isDebug = BuildConfig.DEBUG,
        )

        appGraph = createGraphFactory<StorefrontAppGraph.Factory>().create(this)
        StorefrontGraphHolder.graph = appGraph
    }
}
