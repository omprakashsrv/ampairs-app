package com.ampairs.subscription.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.subscription.db.SubscriptionDao
import com.ampairs.subscription.db.SubscriptionDatabase
import com.ampairs.subscription.util.SubscriptionOnboardingLookup
import com.ampairs.subscription.util.SubscriptionOnboardingManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface SubscriptionDaoModule {
    companion object {
        @Provides
        fun provideSubscriptionOnboardingLookup(mgr: SubscriptionOnboardingManager): SubscriptionOnboardingLookup = mgr

        @Provides
        fun provideSubscriptionDao(db: SubscriptionDatabase): SubscriptionDao = db.subscriptionDao()
    }
}

fun subscriptionModule() = Unit
