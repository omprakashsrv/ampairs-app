package com.ampairs.subscription.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payment transaction record
 */
@Serializable
data class PaymentTransaction(
    val uid: String,
    @SerialName("payment_provider")
    val paymentProvider: PaymentProvider,
    val status: PaymentStatus,
    val amount: Double,
    val currency: String,
    @SerialName("net_amount")
    val netAmount: Double,
    @SerialName("payment_method_type")
    val paymentMethodType: PaymentMethodType? = null,
    @SerialName("payment_method_last4")
    val paymentMethodLast4: String? = null,
    @SerialName("card_brand")
    val cardBrand: String? = null,
    val description: String? = null,
    @SerialName("billing_period_start")
    val billingPeriodStart: String? = null,
    @SerialName("billing_period_end")
    val billingPeriodEnd: String? = null,
    @SerialName("paid_at")
    val paidAt: String? = null,
    @SerialName("failure_reason")
    val failureReason: String? = null,
    @SerialName("receipt_url")
    val receiptUrl: String? = null,
    @SerialName("invoice_pdf_url")
    val invoicePdfUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    /**
     * Get formatted amount with currency symbol
     */
    fun getFormattedAmount(): String {
        val symbol = when (currency.uppercase()) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> currency
        }
        return "$symbol%.2f".format(amount)
    }

    /**
     * Get display name for payment method
     */
    fun getPaymentMethodDisplay(): String {
        return when {
            paymentMethodType == PaymentMethodType.CARD && paymentMethodLast4 != null -> {
                "${cardBrand ?: "Card"} •••• $paymentMethodLast4"
            }
            paymentMethodType == PaymentMethodType.UPI -> "UPI"
            paymentMethodType == PaymentMethodType.IN_APP_PURCHASE -> {
                when (paymentProvider) {
                    PaymentProvider.GOOGLE_PLAY -> "Google Play"
                    PaymentProvider.APP_STORE -> "App Store"
                    else -> "In-App Purchase"
                }
            }
            else -> paymentProvider.displayName
        }
    }
}

/**
 * Payment method (card, UPI, etc.)
 */
@Serializable
data class PaymentMethod(
    val uid: String,
    @SerialName("payment_provider")
    val paymentProvider: PaymentProvider,
    val type: PaymentMethodType,
    val last4: String? = null,
    val brand: String? = null,
    @SerialName("exp_month")
    val expMonth: Int? = null,
    @SerialName("exp_year")
    val expYear: Int? = null,
    @SerialName("cardholder_name")
    val cardholderName: String? = null,
    @SerialName("upi_id")
    val upiId: String? = null,
    @SerialName("bank_name")
    val bankName: String? = null,
    @SerialName("is_default")
    val isDefault: Boolean = false,
    @SerialName("is_expired")
    val isExpired: Boolean = false,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    /**
     * Get card expiry string (MM/YY format)
     */
    fun getExpiryString(): String? {
        if (expMonth == null || expYear == null) return null
        val yearStr = (expYear % 100).toString().padStart(2, '0')
        val monthStr = expMonth.toString().padStart(2, '0')
        return "$monthStr/$yearStr"
    }
}

/**
 * Result of limit check
 */
@Serializable
data class LimitCheckResult(
    val allowed: Boolean,
    val limit: Int,
    val current: Int,
    val remaining: Int,
    @SerialName("is_unlimited")
    val isUnlimited: Boolean = false,
    val exceeded: Boolean = false,
    val warning: Boolean = false
) {
    companion object {
        fun unlimited(current: Int): LimitCheckResult {
            return LimitCheckResult(
                allowed = true,
                limit = -1,
                current = current,
                remaining = -1,
                isUnlimited = true
            )
        }

        fun allowed(current: Int, limit: Int): LimitCheckResult {
            val remaining = limit - current
            return LimitCheckResult(
                allowed = true,
                limit = limit,
                current = current,
                remaining = remaining,
                warning = current >= (limit * 0.8).toInt()
            )
        }

        fun exceeded(current: Int, limit: Int): LimitCheckResult {
            return LimitCheckResult(
                allowed = false,
                limit = limit,
                current = current,
                remaining = 0,
                exceeded = true
            )
        }
    }
}

/**
 * Plan change request
 */
@Serializable
data class ChangePlanRequest(
    @SerialName("new_plan_code")
    val newPlanCode: String,
    @SerialName("billing_cycle")
    val billingCycle: BillingCycle,
    val immediate: Boolean = false
)

/**
 * Plan change response
 */
@Serializable
data class ChangePlanResponse(
    val subscription: SubscriptionState,
    @SerialName("proration_amount")
    val prorationAmount: Double? = null,
    @SerialName("effective_at")
    val effectiveAt: String,
    @SerialName("is_immediate")
    val isImmediate: Boolean
)

/**
 * Cancel subscription request
 */
@Serializable
data class CancelSubscriptionRequest(
    val immediate: Boolean = false,
    val reason: String? = null,
    val feedback: String? = null
)

/**
 * Pause subscription request
 */
@Serializable
data class PauseSubscriptionRequest(
    @SerialName("pause_days")
    val pauseDays: Int = 30,
    val reason: String? = null
)

/**
 * Initiate purchase request (for desktop)
 */
@Serializable
data class InitiatePurchaseRequest(
    @SerialName("plan_code")
    val planCode: String,
    @SerialName("billing_cycle")
    val billingCycle: BillingCycle,
    val currency: String = "INR",
    @SerialName("coupon_code")
    val couponCode: String? = null
)

/**
 * Initiate purchase response
 */
@Serializable
data class InitiatePurchaseResponse(
    @SerialName("checkout_url")
    val checkoutUrl: String?,
    @SerialName("checkout_session_id")
    val checkoutSessionId: String?,
    val provider: PaymentProvider,
    @SerialName("subscription_id")
    val subscriptionId: String?,
    @SerialName("razorpay_order_id")
    val razorpayOrderId: String?,
    @SerialName("razorpay_subscription_id")
    val razorpaySubscriptionId: String?,
    @SerialName("stripe_client_secret")
    val stripeClientSecret: String?,
    val amount: Double,
    val currency: String
)

/**
 * Verify purchase request (for mobile in-app purchases)
 */
@Serializable
data class VerifyPurchaseRequest(
    val provider: PaymentProvider,
    @SerialName("purchase_token")
    val purchaseToken: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("package_name")
    val packageName: String? = null,
    @SerialName("existing_subscription_id")
    val existingSubscriptionId: String? = null
)

/**
 * Sync subscription request
 */
@Serializable
data class SyncSubscriptionRequest(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("last_sync_at")
    val lastSyncAt: String? = null
)

/**
 * Sync subscription response
 */
@Serializable
data class SyncSubscriptionResponse(
    val subscription: SubscriptionState,
    val device: DeviceRegistration,
    val usage: UsageMetrics?,
    @SerialName("server_time")
    val serverTime: String
)
