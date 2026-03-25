package com.ampairs.customer.ui.components.location

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module for location services
 * Overrides commonMain module to provide Context injection
 */
actual val locationServiceModule = module {
    single<LocationService> { LocationService(androidContext()) }
}
