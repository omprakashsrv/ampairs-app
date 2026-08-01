package com.ampairs.marketplace

import android.app.Application
import com.ampairs.common.CurrentActivity
import com.ampairs.common.config.PlatformConfig
import com.ampairs.storefront.di.StorefrontAppGraph
import com.ampairs.storefront.di.StorefrontGraphHolder
import dev.zacsweers.metro.createGraphFactory

/**
 * Application entry point for the common (multi-store) Ampairs ecom app. Identical wiring to the
 * white-label [:clientApp] — it creates the same slim [StorefrontAppGraph] (auth + ecom + store) —
 * the only difference is that this app is not pinned to a single storefront (see [MarketMainActivity]).
 */
class MarketApplication : Application() {

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
