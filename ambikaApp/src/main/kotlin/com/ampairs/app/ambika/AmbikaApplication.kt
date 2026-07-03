package com.ampairs.app.ambika

import android.app.Application
import com.ampairs.common.CurrentActivity
import com.ampairs.common.config.PlatformConfig
import com.ampairs.storefront.di.StorefrontAppGraph
import com.ampairs.storefront.di.StorefrontGraphHolder
import dev.zacsweers.metro.createGraphFactory

/**
 * Application entry point for the customer-facing Ambika ecom app. Creates the slim
 * [StorefrontAppGraph] (auth + ecom + store only) and configures the API base URL from BuildConfig.
 */
class AmbikaApplication : Application() {

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
