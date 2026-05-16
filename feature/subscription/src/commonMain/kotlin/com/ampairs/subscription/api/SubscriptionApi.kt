package com.ampairs.subscription.api

import com.ampairs.common.model.PageResponse
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.*

/**
 * API interface for subscription operations
 */
interface SubscriptionApi {

    // ======================
    // Plan APIs
    // ======================

    /**
     * Get all available subscription plans
     */
    suspend fun getPlans(): List<PlanResponseDto>

    /**
     * Get a specific plan by code
     */
    suspend fun getPlan(planCode: String): PlanResponseDto

    // ======================
    // Subscription APIs
    // ======================

    /**
     * Get current subscription for the workspace
     */
    suspend fun getCurrentSubscription(): SubscriptionResponseDto

    /**
     * Initiate purchase (Desktop - returns checkout URL)
     */
    suspend fun initiatePurchase(
        request: InitiatePurchaseRequest,
        provider: PaymentProvider
    ): InitiatePurchaseResponseDto

    /**
     * Verify mobile in-app purchase
     */
    suspend fun verifyPurchase(request: VerifyPurchaseRequest): SubscriptionResponseDto

    /**
     * Change subscription plan
     */
    suspend fun changePlan(request: ChangePlanRequest): ChangePlanResponseDto

    /**
     * Cancel subscription
     */
    suspend fun cancelSubscription(request: CancelSubscriptionRequest): SubscriptionResponseDto

    /**
     * Pause subscription
     */
    suspend fun pauseSubscription(request: PauseSubscriptionRequest): SubscriptionResponseDto

    /**
     * Resume paused subscription
     */
    suspend fun resumeSubscription(): SubscriptionResponseDto

    /**
     * Start trial for a plan
     */
    suspend fun startTrial(planCode: String, trialDays: Int = 14): SubscriptionResponseDto

    // ======================
    // Usage & Limits APIs
    // ======================

    /**
     * Get current usage for the workspace
     */
    suspend fun getUsage(): UsageResponseDto

    /**
     * Check if a resource limit is exceeded
     */
    suspend fun checkLimit(resourceType: String, currentCount: Int): LimitCheckResultDto

    /**
     * Check if a feature is available
     */
    suspend fun hasFeature(feature: String): Boolean

    // ======================
    // Device APIs
    // ======================

    /**
     * Register a device
     */
    suspend fun registerDevice(
        request: RegisterDeviceRequest,
        userId: String
    ): DeviceRegistrationResponseDto

    /**
     * Refresh device token
     */
    suspend fun refreshDeviceToken(request: RefreshDeviceTokenRequest): DeviceRegistrationResponseDto

    /**
     * Get all registered devices
     */
    suspend fun getDevices(): List<DeviceRegistrationResponseDto>

    /**
     * Get device access mode
     */
    suspend fun getDeviceAccessMode(deviceId: String): SubscriptionAccessMode

    /**
     * Deactivate a device
     */
    suspend fun deactivateDevice(deviceUid: String, reason: String? = null)

    // ======================
    // Billing APIs
    // ======================

    /**
     * Get payment history
     */
    suspend fun getPaymentHistory(
        page: Int = 0,
        size: Int = 20
    ): PageResponse<PaymentTransactionResponseDto>

    /**
     * Get all payment methods
     */
    suspend fun getPaymentMethods(): List<PaymentMethodResponseDto>

    /**
     * Get default payment method
     */
    suspend fun getDefaultPaymentMethod(): PaymentMethodResponseDto?

    /**
     * Set default payment method
     */
    suspend fun setDefaultPaymentMethod(paymentMethodUid: String): PaymentMethodResponseDto

    /**
     * Remove a payment method
     */
    suspend fun removePaymentMethod(uid: String)

    // ======================
    // Sync APIs
    // ======================

    /**
     * Sync subscription state (for offline-first apps)
     */
    suspend fun syncSubscription(
        request: SyncSubscriptionRequest,
        userId: String
    ): SyncSubscriptionResponseDto
}
