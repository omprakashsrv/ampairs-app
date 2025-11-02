package com.ampairs.common.firebase.di

import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.ampairs.common.firebase.performance.FirebasePerformance
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop implementation of Firebase platform module
 * Note: Firebase is not supported on Desktop, so these are stub implementations
 */
actual val firebasePlatformModule: Module = module {
    // Stub implementations for Desktop (all no-ops with console logging)
    single { FirebaseAnalytics() }
    single { FirebaseCrashlytics() }
    single { FirebasePerformance() }
    single { FirebaseMessaging() }
}
