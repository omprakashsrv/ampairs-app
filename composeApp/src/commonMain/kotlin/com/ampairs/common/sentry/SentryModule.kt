package com.ampairs.common.sentry

import org.koin.dsl.module

/**
 * Sentry module for dependency injection.
 * SentryManager is a singleton object available across all platforms.
 */
val sentryModule = module {
    // SentryManager is an object, no need to provide it through Koin
    // It's accessible globally as SentryManager.initialize(), etc.
    // This module is kept for potential future Sentry-related dependencies
}
