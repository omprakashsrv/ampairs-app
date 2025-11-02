package com.ampairs.customer.ui.components.location

import org.koin.dsl.module

/**
 * Desktop-specific Koin module for location services
 */
actual val locationServiceModule = module {
    single<LocationService> { LocationService() }
}
