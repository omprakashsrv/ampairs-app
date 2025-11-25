package com.ampairs.subscription.di

import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.subscription.api.SubscriptionApi
import com.ampairs.subscription.api.SubscriptionApiImpl
import com.ampairs.subscription.db.SubscriptionDao
import com.ampairs.subscription.db.SubscriptionDatabase
import com.ampairs.subscription.feature.FeatureGate
import com.ampairs.subscription.feature.LimitChecker
import com.ampairs.subscription.repository.SubscriptionRepository
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Common Koin module for subscription (platform-independent)
 */
val subscriptionModule = module {
    includes(subscriptionPlatformModule)

    // API
    factory<SubscriptionApi> {
        SubscriptionApiImpl(
            engine = get(),
            tokenRepository = get()
        )
    }

    // DAO - from database provided by platform
    factory<SubscriptionDao> {
        get<SubscriptionDatabase>().subscriptionDao()
    }

    // Repository
    factory {
        SubscriptionRepository(
            api = get(),
            dao = get()
        )
    }

    // Feature Gate
    factory {
        FeatureGate(
            repository = get()
        )
    }

    // Limit Checker
    factory {
        LimitChecker(
            repository = get()
        )
    }

    // ViewModel - uses AppHeaderStateManager for user context (synchronous access)
    viewModel {
        SubscriptionViewModel(
            repository = get(),
            featureGate = get(),
            limitChecker = get(),
            userIdProvider = { AppHeaderStateManager.instance.headerState.value.currentUser?.id ?: "" },
            deviceIdProvider = { getDeviceId() }
        )
    }
}

/**
 * Get device ID - platform-specific implementation expected
 */
expect fun getDeviceId(): String

/**
 * Platform-specific module for subscription database
 */
expect val subscriptionPlatformModule: Module
