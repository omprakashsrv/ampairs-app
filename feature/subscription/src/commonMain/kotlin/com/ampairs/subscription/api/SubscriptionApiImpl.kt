package com.ampairs.subscription.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.*
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of SubscriptionApi using Ktor HTTP client
 */
@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class SubscriptionApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : SubscriptionApi {

    private val client = httpClient(engine, tokenRepository)

    // ======================
    // Plan APIs
    // ======================

    override suspend fun getPlans(): List<PlanResponseDto> {
        val response: Response<List<PlanResponseDto>> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/plans")
        )
        return response.data ?: emptyList()
    }

    override suspend fun getPlan(planCode: String): PlanResponseDto {
        val response: Response<PlanResponseDto> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/plans/$planCode")
        )
        return response.data ?: throw SubscriptionApiException("Plan not found: $planCode")
    }

    // ======================
    // Subscription APIs
    // ======================

    override suspend fun getCurrentSubscription(): SubscriptionResponseDto {
        val response: Response<SubscriptionResponseDto> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/current")
        )
        return response.data ?: throw SubscriptionApiException("Subscription not found")
    }

    override suspend fun initiatePurchase(
        request: InitiatePurchaseRequest,
        provider: PaymentProvider
    ): InitiatePurchaseResponseDto {
        val response: Response<InitiatePurchaseResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/purchase/initiate?provider=${provider.name}"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to initiate purchase"
        )
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): SubscriptionResponseDto {
        val response: Response<SubscriptionResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/purchase/verify"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to verify purchase"
        )
    }

    override suspend fun changePlan(request: ChangePlanRequest): ChangePlanResponseDto {
        val response: Response<ChangePlanResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/change-plan"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to change plan"
        )
    }

    override suspend fun cancelSubscription(request: CancelSubscriptionRequest): SubscriptionResponseDto {
        val response: Response<SubscriptionResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/cancel"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to cancel subscription"
        )
    }

    override suspend fun pauseSubscription(request: PauseSubscriptionRequest): SubscriptionResponseDto {
        val response: Response<SubscriptionResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/pause"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to pause subscription"
        )
    }

    override suspend fun resumeSubscription(): SubscriptionResponseDto {
        val response: Response<SubscriptionResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/resume"),
            Unit
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to resume subscription"
        )
    }

    override suspend fun startTrial(planCode: String, trialDays: Int): SubscriptionResponseDto {
        val response: Response<SubscriptionResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/trial?planCode=$planCode&trialDays=$trialDays"),
            Unit
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to start trial"
        )
    }

    // ======================
    // Usage & Limits APIs
    // ======================

    override suspend fun getUsage(): UsageResponseDto {
        val response: Response<UsageResponseDto> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/usage")
        )
        return response.data ?: throw SubscriptionApiException("Failed to get usage")
    }

    override suspend fun checkLimit(resourceType: String, currentCount: Int): LimitCheckResultDto {
        val response: Response<LimitCheckResultDto> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/limits/check"),
            mapOf("resourceType" to resourceType, "currentCount" to currentCount)
        )
        return response.data ?: throw SubscriptionApiException("Failed to check limit")
    }

    override suspend fun hasFeature(feature: String): Boolean {
        val response: Response<Map<String, Boolean>> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/features/$feature")
        )
        return response.data?.get("available") ?: false
    }

    // ======================
    // Device APIs
    // ======================

    override suspend fun registerDevice(
        request: RegisterDeviceRequest,
        userId: String
    ): DeviceRegistrationResponseDto {
        val response: Response<DeviceRegistrationResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/devices/register?userId=$userId"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to register device"
        )
    }

    override suspend fun refreshDeviceToken(request: RefreshDeviceTokenRequest): DeviceRegistrationResponseDto {
        val response: Response<DeviceRegistrationResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/devices/refresh-token"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to refresh device token"
        )
    }

    override suspend fun getDevices(): List<DeviceRegistrationResponseDto> {
        val response: Response<List<DeviceRegistrationResponseDto>> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/devices")
        )
        return response.data ?: emptyList()
    }

    override suspend fun getDeviceAccessMode(deviceId: String): SubscriptionAccessMode {
        val response: Response<Map<String, SubscriptionAccessMode>> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/devices/$deviceId/access-mode")
        )
        return response.data?.get("accessMode") ?: SubscriptionAccessMode.FULL_ACCESS
    }

    override suspend fun updatePushToken(deviceId: String, pushToken: String, pushTokenType: String) {
        val response: Response<Map<String, Boolean>> = post(
            client,
            ApiUrlBuilder.subscriptionUrl(
                "v1/devices/$deviceId/push-token?pushToken=$pushToken&pushTokenType=$pushTokenType"
            ),
            emptyMap<String, String>(),
        )
        if (response.error != null) {
            throw SubscriptionApiException(response.error?.message ?: "Failed to update push token")
        }
    }

    override suspend fun deactivateDevice(deviceUid: String, reason: String?) {
        val url = if (reason != null) {
            ApiUrlBuilder.subscriptionUrl("v1/devices/$deviceUid?reason=$reason")
        } else {
            ApiUrlBuilder.subscriptionUrl("v1/devices/$deviceUid")
        }
        delete<Response<Map<String, Boolean>>>(client, url)
    }

    // ======================
    // Billing APIs
    // ======================

    override suspend fun getPaymentHistory(page: Int, size: Int): PageResponse<PaymentTransactionResponseDto> {
        val response: Response<PageResponse<PaymentTransactionResponseDto>> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/payments"),
            mapOf("page" to page, "size" to size)
        )
        return response.data ?: PageResponse(
            content = emptyList(),
            pageNumber = page,
            pageSize = size,
            totalPages = 0,
            totalElements = 0L,
            hasNext = false,
            hasPrevious = false,
            first = true,
            last = true
        )
    }

    override suspend fun getPaymentMethods(): List<PaymentMethodResponseDto> {
        val response: Response<List<PaymentMethodResponseDto>> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/payment-methods")
        )
        return response.data ?: emptyList()
    }

    override suspend fun getDefaultPaymentMethod(): PaymentMethodResponseDto? {
        val response: Response<PaymentMethodResponseDto> = get(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/payment-methods/default")
        )
        return response.data
    }

    override suspend fun setDefaultPaymentMethod(paymentMethodUid: String): PaymentMethodResponseDto {
        val response: Response<PaymentMethodResponseDto> = put(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/payment-methods/$paymentMethodUid/default"),
            emptyMap<String, String>()
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to set default payment method"
        )
    }

    override suspend fun removePaymentMethod(uid: String) {
        delete<Response<Map<String, Boolean>>>(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/payment-methods/$uid")
        )
    }

    // ======================
    // Sync APIs
    // ======================

    override suspend fun syncSubscription(
        request: SyncSubscriptionRequest,
        userId: String
    ): SyncSubscriptionResponseDto {
        val response: Response<SyncSubscriptionResponseDto> = post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscriptions/sync?userId=$userId"),
            request
        )
        return response.data ?: throw SubscriptionApiException(
            response.error?.message ?: "Failed to sync subscription"
        )
    }
}

/**
 * Exception for subscription API errors
 */
class SubscriptionApiException(message: String) : Exception(message)
