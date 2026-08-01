package com.ampairs.subscription.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.subscription.util.SubscriptionOnboardingLookup
import com.ampairs.subscription.util.SubscriptionOnboardingManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// The subscription DAO is provided by the consolidated workspace database module (:data:database
// for the main app). This module keeps only the feature's non-DAO service bindings.
@ContributesTo(WorkspaceScope::class)
interface SubscriptionServiceModule {
    companion object {
        @Provides
        fun provideSubscriptionOnboardingLookup(mgr: SubscriptionOnboardingManager): SubscriptionOnboardingLookup = mgr
    }
}
