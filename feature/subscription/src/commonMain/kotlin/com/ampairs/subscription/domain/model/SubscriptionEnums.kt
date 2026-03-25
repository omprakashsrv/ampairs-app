package com.ampairs.subscription.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subscription plan tiers
 */
@Serializable
enum class SubscriptionPlan(val displayName: String, val order: Int) {
    @SerialName("FREE")
    FREE("Free", 0),

    @SerialName("STARTER")
    STARTER("Starter", 1),

    @SerialName("PROFESSIONAL")
    PROFESSIONAL("Professional", 2),

    @SerialName("ENTERPRISE")
    ENTERPRISE("Enterprise", 3);

    companion object {
        fun fromCode(code: String): SubscriptionPlan {
            return entries.find { it.name.equals(code, ignoreCase = true) } ?: FREE
        }
    }

    /** Compare plans by their order (tier level) */
    fun isHigherThan(other: SubscriptionPlan): Boolean = this.order > other.order
    fun isLowerThan(other: SubscriptionPlan): Boolean = this.order < other.order
    fun isAtLeast(other: SubscriptionPlan): Boolean = this.order >= other.order
}

/**
 * Billing cycle options for subscriptions
 */
@Serializable
enum class BillingCycle(
    val months: Int,
    val discountPercent: Int,
    val displayName: String
) {
    @SerialName("MONTHLY")
    MONTHLY(1, 0, "Monthly"),

    @SerialName("QUARTERLY")
    QUARTERLY(3, 5, "Quarterly"),

    @SerialName("ANNUAL")
    ANNUAL(12, 20, "Annual"),

    @SerialName("BIENNIAL")
    BIENNIAL(24, 30, "2 Years");

    /**
     * Calculate discounted price for this billing cycle
     */
    fun calculateDiscountedPrice(monthlyPrice: Double): Double {
        val basePrice = monthlyPrice * months
        return basePrice * (1 - discountPercent / 100.0)
    }

    companion object {
        fun fromCode(code: String): BillingCycle {
            return entries.find { it.name.equals(code, ignoreCase = true) } ?: MONTHLY
        }
    }
}

/**
 * Subscription status representing lifecycle states
 */
@Serializable
enum class SubscriptionStatus(val displayName: String) {
    @SerialName("ACTIVE")
    ACTIVE("Active"),

    @SerialName("TRIALING")
    TRIALING("Trial"),

    @SerialName("PAST_DUE")
    PAST_DUE("Past Due"),

    @SerialName("PAUSED")
    PAUSED("Paused"),

    @SerialName("CANCELLED")
    CANCELLED("Cancelled"),

    @SerialName("EXPIRED")
    EXPIRED("Expired"),

    @SerialName("PENDING")
    PENDING("Pending");

    fun isActive(): Boolean = this == ACTIVE || this == TRIALING

    companion object {
        fun fromCode(code: String): SubscriptionStatus {
            return entries.find { it.name.equals(code, ignoreCase = true) } ?: ACTIVE
        }
    }
}

/**
 * Payment providers supported by the system
 */
@Serializable
enum class PaymentProvider(val displayName: String) {
    @SerialName("GOOGLE_PLAY")
    GOOGLE_PLAY("Google Play"),

    @SerialName("APP_STORE")
    APP_STORE("Apple App Store"),

    @SerialName("RAZORPAY")
    RAZORPAY("Razorpay"),

    @SerialName("STRIPE")
    STRIPE("Stripe"),

    @SerialName("MANUAL")
    MANUAL("Manual");

    companion object {
        fun fromCode(code: String): PaymentProvider? {
            return entries.find { it.name.equals(code, ignoreCase = true) }
        }
    }
}

/**
 * Payment transaction status
 */
@Serializable
enum class PaymentStatus {
    @SerialName("PENDING")
    PENDING,

    @SerialName("PROCESSING")
    PROCESSING,

    @SerialName("SUCCEEDED")
    SUCCEEDED,

    @SerialName("FAILED")
    FAILED,

    @SerialName("REFUNDED")
    REFUNDED,

    @SerialName("DISPUTED")
    DISPUTED,

    @SerialName("CANCELLED")
    CANCELLED
}

/**
 * Payment method types
 */
@Serializable
enum class PaymentMethodType {
    @SerialName("CARD")
    CARD,

    @SerialName("UPI")
    UPI,

    @SerialName("NET_BANKING")
    NET_BANKING,

    @SerialName("WALLET")
    WALLET,

    @SerialName("BANK_TRANSFER")
    BANK_TRANSFER,

    @SerialName("IN_APP_PURCHASE")
    IN_APP_PURCHASE
}

/**
 * Add-on module codes
 */
@Serializable
enum class AddonModuleCode(
    val displayName: String,
    val monthlyPriceInr: Double,
    val monthlyPriceUsd: Double,
    val description: String
) {
    @SerialName("TALLY_INTEGRATION")
    TALLY_INTEGRATION(
        displayName = "Tally Integration",
        monthlyPriceInr = 299.0,
        monthlyPriceUsd = 4.0,
        description = "Sync with Tally ERP"
    ),

    @SerialName("ADVANCED_ANALYTICS")
    ADVANCED_ANALYTICS(
        displayName = "Advanced Analytics",
        monthlyPriceInr = 499.0,
        monthlyPriceUsd = 6.0,
        description = "Custom reports and dashboards"
    ),

    @SerialName("MULTI_CURRENCY")
    MULTI_CURRENCY(
        displayName = "Multi-Currency",
        monthlyPriceInr = 199.0,
        monthlyPriceUsd = 3.0,
        description = "Support for multiple currencies"
    ),

    @SerialName("E_INVOICING_GST")
    E_INVOICING_GST(
        displayName = "E-Invoicing (GST)",
        monthlyPriceInr = 399.0,
        monthlyPriceUsd = 5.0,
        description = "Government e-invoicing compliance"
    ),

    @SerialName("INVENTORY_PRO")
    INVENTORY_PRO(
        displayName = "Inventory Pro",
        monthlyPriceInr = 299.0,
        monthlyPriceUsd = 4.0,
        description = "Advanced stock management"
    ),

    @SerialName("CUSTOM_FIELDS")
    CUSTOM_FIELDS(
        displayName = "Custom Fields",
        monthlyPriceInr = 199.0,
        monthlyPriceUsd = 3.0,
        description = "Unlimited custom fields"
    );

    fun getPrice(currency: String): Double {
        return when (currency.uppercase()) {
            "INR" -> monthlyPriceInr
            "USD" -> monthlyPriceUsd
            else -> monthlyPriceUsd
        }
    }
}

/**
 * Device platform types
 */
@Serializable
enum class DevicePlatform {
    @SerialName("ANDROID")
    ANDROID,

    @SerialName("IOS")
    IOS,

    @SerialName("DESKTOP_WINDOWS")
    DESKTOP_WINDOWS,

    @SerialName("DESKTOP_MAC")
    DESKTOP_MAC,

    @SerialName("DESKTOP_LINUX")
    DESKTOP_LINUX,

    @SerialName("WEB")
    WEB
}

/**
 * Access mode for offline enforcement
 */
@Serializable
enum class SubscriptionAccessMode {
    /** Full access - subscription active and recently verified */
    @SerialName("FULL_ACCESS")
    FULL_ACCESS,

    /** Offline grace period - token valid but can't verify */
    @SerialName("OFFLINE_GRACE")
    OFFLINE_GRACE,

    /** Read-only mode - subscription likely expired */
    @SerialName("READ_ONLY")
    READ_ONLY,

    /** Locked - extended non-payment, must sync */
    @SerialName("LOCKED")
    LOCKED
}
