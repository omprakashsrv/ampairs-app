package com.ampairs.common.firebase.di

import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.ampairs.common.firebase.performance.FirebasePerformance
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common Firebase module that includes platform-specific implementations
 */
val firebaseModule = module {
    includes(firebasePlatformModule)
}

/**
 * Platform-specific Firebase module
 * Must be implemented in each platform (Android, iOS, Desktop)
 */
expect val firebasePlatformModule: Module
