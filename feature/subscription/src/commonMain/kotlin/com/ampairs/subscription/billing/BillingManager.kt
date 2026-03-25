package com.ampairs.subscription.billing

import com.ampairs.subscription.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for platform-specific billing managers.
 *
 * Implementations:
 * - Android: Google Play Billing Library v7
 * - iOS: StoreKit 2
 * - Desktop: Browser-based checkout (Razorpay/Stripe)
 */
interface BillingManager {

    /**
     * Current billing state
     */
    val billingState: StateFlow<BillingState>

    /**
     * Available products from the store
     */
    val availableProducts: StateFlow<List<StoreProduct>>

    /**
     * Initialize billing connection
     */
    suspend fun initialize(): BillingResult

    /**
     * Query available products for given plan codes
     */
    suspend fun queryProducts(productIds: List<String>): BillingResult

    /**
     * Start purchase flow for a product
     */
    suspend fun launchPurchaseFlow(
        productId: String,
        offerToken: String? = null
    ): BillingResult

    /**
     * Acknowledge a purchase (Android-specific, no-op on other platforms)
     */
    suspend fun acknowledgePurchase(purchaseToken: String): BillingResult

    /**
     * Consume a purchase (for consumable products)
     */
    suspend fun consumePurchase(purchaseToken: String): BillingResult

    /**
     * Query existing purchases
     */
    suspend fun queryPurchases(): List<StorePurchase>

    /**
     * Restore purchases (iOS/desktop)
     */
    suspend fun restorePurchases(): BillingResult

    /**
     * Handle purchase updates (for subscription renewals, etc.)
     */
    fun observePurchases(): Flow<List<StorePurchase>>

    /**
     * Check if billing is available on this platform/device
     */
    suspend fun isBillingAvailable(): Boolean

    /**
     * Clean up resources
     */
    fun disconnect()
}

/**
 * Billing state
 */
sealed class BillingState {
    data object Disconnected : BillingState()
    data object Connecting : BillingState()
    data object Connected : BillingState()
    data class Error(val message: String, val code: Int? = null) : BillingState()
}

/**
 * Result of billing operations
 */
sealed class BillingResult {
    data object Success : BillingResult()
    data class Pending(val purchaseToken: String) : BillingResult()
    data class Purchased(val purchase: StorePurchase) : BillingResult()
    data object Cancelled : BillingResult()
    data class Error(val message: String, val code: Int? = null) : BillingResult()
    data object NotAvailable : BillingResult()
}

/**
 * Product information from the store
 */
data class StoreProduct(
    val productId: String,
    val type: ProductType,
    val title: String,
    val description: String,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val subscriptionOffers: List<SubscriptionOffer> = emptyList(),
    val planCode: String? = null
)

/**
 * Subscription offer (pricing phases)
 */
data class SubscriptionOffer(
    val offerId: String,
    val offerToken: String,
    val pricingPhases: List<PricingPhase>
)

/**
 * Pricing phase (trial, intro, regular)
 */
data class PricingPhase(
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val billingPeriod: String, // ISO 8601 duration
    val billingCycleCount: Int,
    val recurrenceMode: RecurrenceMode
)

/**
 * Recurrence mode for pricing phases
 */
enum class RecurrenceMode {
    INFINITE_RECURRING,
    FINITE_RECURRING,
    NON_RECURRING
}

/**
 * Product type
 */
enum class ProductType {
    SUBSCRIPTION,
    IN_APP_PRODUCT
}

/**
 * Purchase information
 */
data class StorePurchase(
    val purchaseToken: String,
    val productId: String,
    val orderId: String?,
    val purchaseTime: Long,
    val purchaseState: PurchaseState,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
    val originalJson: String? = null
)

/**
 * Purchase state
 */
enum class PurchaseState {
    PENDING,
    PURCHASED,
    UNSPECIFIED
}

/**
 * Exception for billing errors
 */
class BillingException(
    message: String,
    val code: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)
