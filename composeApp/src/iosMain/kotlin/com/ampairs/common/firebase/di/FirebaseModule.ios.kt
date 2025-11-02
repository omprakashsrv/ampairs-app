package com.ampairs.common.firebase.di

import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.ampairs.common.firebase.performance.FirebasePerformance
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS implementation of Firebase platform module
 */
actual val firebasePlatformModule: Module = module {
    single { FirebaseAnalytics() }
    single { FirebaseCrashlytics() }
    single { FirebasePerformance() }
    single { FirebaseMessaging() }
}
