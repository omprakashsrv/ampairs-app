package com.ampairs.common.firebase.di

import com.ampairs.common.di.AppScope
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.analytics.FirebaseAnalyticsImpl
import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.ampairs.common.firebase.performance.FirebasePerformance
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface AndroidFirebaseModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideAnalytics(): FirebaseAnalytics = FirebaseAnalyticsImpl()

        @Provides @SingleIn(AppScope::class)
        fun provideCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics()

        @Provides @SingleIn(AppScope::class)
        fun providePerformance(): FirebasePerformance = FirebasePerformance()

        @Provides @SingleIn(AppScope::class)
        fun provideMessaging(): FirebaseMessaging = FirebaseMessaging()
    }
}
