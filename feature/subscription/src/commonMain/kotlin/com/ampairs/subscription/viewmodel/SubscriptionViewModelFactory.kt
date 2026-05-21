package com.ampairs.subscription.viewmodel

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.DeviceService
import com.ampairs.common.di.AppScope
import com.ampairs.subscription.feature.FeatureGate
import com.ampairs.subscription.feature.LimitChecker
import com.ampairs.subscription.repository.SubscriptionRepository
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.runBlocking

/**
 * Factory for SubscriptionViewModel. Wraps the lambda providers so Metro can inject
 * all deps without needing to bind the raw () -> String function types.
 */
@Inject
@SingleIn(AppScope::class)
class SubscriptionViewModelFactory(
    private val repository: SubscriptionRepository,
    private val featureGate: FeatureGate,
    private val limitChecker: LimitChecker,
    private val tokenRepository: TokenRepository,
    private val deviceService: DeviceService
) {
    fun create(): SubscriptionViewModel = SubscriptionViewModel(
        repository = repository,
        featureGate = featureGate,
        limitChecker = limitChecker,
        userIdProvider = { runBlocking { tokenRepository.getCurrentUserId() ?: "" } },
        deviceIdProvider = { deviceService.getDeviceId() }
    )
}
