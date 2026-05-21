package com.ampairs.subscription.di

import com.ampairs.common.di.AppScope
import com.ampairs.subscription.db.SubscriptionDao
import com.ampairs.subscription.db.SubscriptionDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Replaced Koin subscriptionModule and subscriptionPlatformModule expect.
// Injectable classes are annotated with @Inject directly:
// - SubscriptionApiImpl: @Inject @SingleIn(AppScope) @ContributesBinding
// - InvoiceApiImpl: @Inject @SingleIn(AppScope) @ContributesBinding
// - SubscriptionRepository, InvoiceRepository: @Inject
// - FeatureGate, LimitChecker, SubscriptionEnforcement: @Inject
// - SubscriptionOnboardingManager: @Inject @SingleIn(AppScope)
// - SubscriptionViewModel, InvoiceViewModel: @Inject
// Platform DB providers are in SubscriptionPlatformModule.android/ios/desktop.kt

@ContributesTo(AppScope::class)
interface SubscriptionDaoModule {
    companion object {
        @Provides
        fun provideSubscriptionDao(db: SubscriptionDatabase): SubscriptionDao = db.subscriptionDao()
    }
}

fun subscriptionModule() = Unit
