package com.ampairs.subscription.domain.dto

import com.ampairs.subscription.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API Response wrapper matching backend ApiResponse<T>
 */
@Serializable
data class SubscriptionApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val message: String? = null,
    val success: Boolean = data != null && error == null
)

/**
 * Paginated response wrapper
 */
@Serializable
data class PagedResponse<T>(
    val content: List<T>,
    @SerialName("total_elements")
    val totalElements: Long,
    @SerialName("total_pages")
    val totalPages: Int,
    val page: Int,
    val size: Int,
    @SerialName("has_next")
    val hasNext: Boolean,
    @SerialName("has_previous")
    val hasPrevious: Boolean
)

// ======================
// Plan DTOs
// ======================

/**
 * Plan response from API
 */
@Serializable
data class PlanResponseDto(
    val uid: String,
    @SerialName("plan_code")
    val planCode: String,
    @SerialName("display_name")
    val displayName: String,
    val description: String? = null,
    @SerialName("monthly_price_inr")
    val monthlyPriceInr: Double,
    @SerialName("monthly_price_usd")
    val monthlyPriceUsd: Double,
    val limits: PlanLimitsDto,
    val features: PlanFeaturesDto,
    @SerialName("trial_days")
    val trialDays: Int,
    @SerialName("google_play_product_id_monthly")
    val googlePlayProductIdMonthly: String? = null,
    @SerialName("google_play_product_id_annual")
    val googlePlayProductIdAnnual: String? = null,
    @SerialName("app_store_product_id_monthly")
    val appStoreProductIdMonthly: String? = null,
    @SerialName("app_store_product_id_annual")
    val appStoreProductIdAnnual: String? = null,
    @SerialName("display_order")
    val displayOrder: Int
) {
    fun toModel(): SubscriptionPlanDefinition {
        return SubscriptionPlanDefinition(
            uid = uid,
            planCode = planCode,
            displayName = displayName,
            description = description,
            monthlyPriceInr = monthlyPriceInr,
            monthlyPriceUsd = monthlyPriceUsd,
            limits = limits.toModel(),
            features = features.toModel(),
            trialDays = trialDays,
            googlePlayProductIdMonthly = googlePlayProductIdMonthly,
            googlePlayProductIdAnnual = googlePlayProductIdAnnual,
            appStoreProductIdMonthly = appStoreProductIdMonthly,
            appStoreProductIdAnnual = appStoreProductIdAnnual,
            displayOrder = displayOrder
        )
    }
}

@Serializable
data class PlanLimitsDto(
    @SerialName("max_workspaces")
    val maxWorkspaces: Int,
    @SerialName("max_members_per_workspace")
    val maxMembersPerWorkspace: Int,
    @SerialName("max_storage_gb")
    val maxStorageGb: Int,
    @SerialName("max_customers")
    val maxCustomers: Int,
    @SerialName("max_products")
    val maxProducts: Int,
    @SerialName("max_invoices_per_month")
    val maxInvoicesPerMonth: Int,
    @SerialName("max_devices")
    val maxDevices: Int,
    @SerialName("data_retention_years")
    val dataRetentionYears: Int
) {
    fun toModel(): SubscriptionLimits {
        return SubscriptionLimits(
            maxWorkspaces = maxWorkspaces,
            maxMembersPerWorkspace = maxMembersPerWorkspace,
            maxStorageGb = maxStorageGb,
            maxCustomers = maxCustomers,
            maxProducts = maxProducts,
            maxInvoicesPerMonth = maxInvoicesPerMonth,
            maxDevices = maxDevices,
            dataRetentionYears = dataRetentionYears
        )
    }
}

@Serializable
data class PlanFeaturesDto(
    @SerialName("available_modules")
    val availableModules: List<String>,
    @SerialName("api_access_enabled")
    val apiAccessEnabled: Boolean,
    @SerialName("custom_branding_enabled")
    val customBrandingEnabled: Boolean,
    @SerialName("sso_enabled")
    val ssoEnabled: Boolean,
    @SerialName("audit_logs_enabled")
    val auditLogsEnabled: Boolean,
    @SerialName("priority_support")
    val prioritySupport: Boolean
) {
    fun toModel(): SubscriptionFeatures {
        return SubscriptionFeatures(
            availableModules = availableModules,
            apiAccessEnabled = apiAccessEnabled,
            customBrandingEnabled = customBrandingEnabled,
            ssoEnabled = ssoEnabled,
            auditLogsEnabled = auditLogsEnabled,
            prioritySupport = prioritySupport
        )
    }
}

// ======================
// Subscription DTOs
// ======================

/**
 * Subscription response from API
 */
@Serializable
data class SubscriptionResponseDto(
    val uid: String,
    @SerialName("workspace_id")
    val workspaceId: String,
    @SerialName("plan_code")
    val planCode: String,
    val plan: PlanResponseDto? = null,
    val status: SubscriptionStatus,
    @SerialName("billing_cycle")
    val billingCycle: BillingCycle,
    @SerialName("payment_provider")
    val paymentProvider: PaymentProvider? = null,
    val currency: String,
    @SerialName("current_period_start")
    val currentPeriodStart: String? = null,
    @SerialName("current_period_end")
    val currentPeriodEnd: String? = null,
    @SerialName("trial_ends_at")
    val trialEndsAt: String? = null,
    @SerialName("cancel_at_period_end")
    val cancelAtPeriodEnd: Boolean = false,
    @SerialName("cancelled_at")
    val cancelledAt: String? = null,
    @SerialName("next_billing_amount")
    val nextBillingAmount: Double? = null,
    @SerialName("last_payment_status")
    val lastPaymentStatus: PaymentStatus? = null,
    @SerialName("last_payment_at")
    val lastPaymentAt: String? = null,
    @SerialName("is_free")
    val isFree: Boolean,
    @SerialName("days_remaining")
    val daysRemaining: Long,
    @SerialName("active_addons")
    val activeAddons: List<AddonResponseDto> = emptyList(),
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toModel(): SubscriptionState {
        return SubscriptionState(
            uid = uid,
            workspaceId = workspaceId,
            planCode = planCode,
            plan = plan?.toModel(),
            status = status,
            billingCycle = billingCycle,
            paymentProvider = paymentProvider,
            currency = currency,
            currentPeriodStart = currentPeriodStart,
            currentPeriodEnd = currentPeriodEnd,
            trialEndsAt = trialEndsAt,
            cancelAtPeriodEnd = cancelAtPeriodEnd,
            cancelledAt = cancelledAt,
            nextBillingAmount = nextBillingAmount,
            lastPaymentStatus = lastPaymentStatus,
            lastPaymentAt = lastPaymentAt,
            isFree = isFree,
            daysRemaining = daysRemaining,
            activeAddons = activeAddons.map { it.toModel() },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

@Serializable
data class AddonResponseDto(
    val uid: String,
    @SerialName("addon_code")
    val addonCode: AddonModuleCode,
    @SerialName("display_name")
    val displayName: String,
    val description: String,
    val status: SubscriptionStatus,
    val price: Double,
    val currency: String,
    @SerialName("activated_at")
    val activatedAt: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null
) {
    fun toModel(): SubscriptionAddon {
        return SubscriptionAddon(
            uid = uid,
            addonCode = addonCode,
            displayName = displayName,
            description = description,
            status = status,
            price = price,
            currency = currency,
            activatedAt = activatedAt,
            expiresAt = expiresAt
        )
    }
}

// ======================
// Usage DTOs
// ======================

@Serializable
data class UsageResponseDto(
    @SerialName("workspace_id")
    val workspaceId: String,
    @SerialName("period_year")
    val periodYear: Int,
    @SerialName("period_month")
    val periodMonth: Int,
    val usage: UsageDetailsDto,
    val limits: UsageLimitsDto,
    val exceeded: ExceededLimitsDto,
    @SerialName("last_calculated_at")
    val lastCalculatedAt: String? = null
) {
    fun toModel(): UsageMetrics {
        return UsageMetrics(
            workspaceId = workspaceId,
            periodYear = periodYear,
            periodMonth = periodMonth,
            usage = usage.toModel(),
            limits = limits.toModel(),
            exceeded = exceeded.toModel(),
            lastCalculatedAt = lastCalculatedAt
        )
    }
}

@Serializable
data class UsageDetailsDto(
    @SerialName("customer_count")
    val customerCount: Int,
    @SerialName("product_count")
    val productCount: Int,
    @SerialName("invoice_count")
    val invoiceCount: Int,
    @SerialName("order_count")
    val orderCount: Int,
    @SerialName("member_count")
    val memberCount: Int,
    @SerialName("device_count")
    val deviceCount: Int,
    @SerialName("storage_used_gb")
    val storageUsedGb: Double,
    @SerialName("api_calls")
    val apiCalls: Long,
    @SerialName("sms_count")
    val smsCount: Int,
    @SerialName("email_count")
    val emailCount: Int
) {
    fun toModel(): UsageDetails {
        return UsageDetails(
            customerCount = customerCount,
            productCount = productCount,
            invoiceCount = invoiceCount,
            orderCount = orderCount,
            memberCount = memberCount,
            deviceCount = deviceCount,
            storageUsedGb = storageUsedGb,
            apiCalls = apiCalls,
            smsCount = smsCount,
            emailCount = emailCount
        )
    }
}

@Serializable
data class UsageLimitsDto(
    @SerialName("max_customers")
    val maxCustomers: Int,
    @SerialName("max_products")
    val maxProducts: Int,
    @SerialName("max_invoices_per_month")
    val maxInvoicesPerMonth: Int,
    @SerialName("max_members")
    val maxMembers: Int,
    @SerialName("max_devices")
    val maxDevices: Int,
    @SerialName("max_storage_gb")
    val maxStorageGb: Int
) {
    fun toModel(): UsageLimits {
        return UsageLimits(
            maxCustomers = maxCustomers,
            maxProducts = maxProducts,
            maxInvoicesPerMonth = maxInvoicesPerMonth,
            maxMembers = maxMembers,
            maxDevices = maxDevices,
            maxStorageGb = maxStorageGb
        )
    }
}

@Serializable
data class ExceededLimitsDto(
    @SerialName("customer_limit_exceeded")
    val customerLimitExceeded: Boolean,
    @SerialName("product_limit_exceeded")
    val productLimitExceeded: Boolean,
    @SerialName("invoice_limit_exceeded")
    val invoiceLimitExceeded: Boolean,
    @SerialName("storage_limit_exceeded")
    val storageLimitExceeded: Boolean,
    @SerialName("member_limit_exceeded")
    val memberLimitExceeded: Boolean,
    @SerialName("device_limit_exceeded")
    val deviceLimitExceeded: Boolean,
    @SerialName("has_any_exceeded")
    val hasAnyExceeded: Boolean
) {
    fun toModel(): ExceededLimits {
        return ExceededLimits(
            customerLimitExceeded = customerLimitExceeded,
            productLimitExceeded = productLimitExceeded,
            invoiceLimitExceeded = invoiceLimitExceeded,
            storageLimitExceeded = storageLimitExceeded,
            memberLimitExceeded = memberLimitExceeded,
            deviceLimitExceeded = deviceLimitExceeded,
            hasAnyExceeded = hasAnyExceeded
        )
    }
}

// ======================
// Device DTOs
// ======================

@Serializable
data class DeviceRegistrationResponseDto(
    val uid: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String? = null,
    val platform: DevicePlatform,
    @SerialName("device_model")
    val deviceModel: String? = null,
    @SerialName("os_version")
    val osVersion: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
    @SerialName("token_expires_at")
    val tokenExpiresAt: String,
    @SerialName("last_sync_at")
    val lastSyncAt: String? = null,
    @SerialName("last_activity_at")
    val lastActivityAt: String? = null,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("access_mode")
    val accessMode: SubscriptionAccessMode,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toModel(): DeviceRegistration {
        return DeviceRegistration(
            uid = uid,
            deviceId = deviceId,
            deviceName = deviceName,
            platform = platform,
            deviceModel = deviceModel,
            osVersion = osVersion,
            appVersion = appVersion,
            tokenExpiresAt = tokenExpiresAt,
            lastSyncAt = lastSyncAt,
            lastActivityAt = lastActivityAt,
            isActive = isActive,
            accessMode = accessMode,
            createdAt = createdAt
        )
    }
}

// ======================
// Payment DTOs
// ======================

@Serializable
data class PaymentTransactionResponseDto(
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
    fun toModel(): PaymentTransaction {
        return PaymentTransaction(
            uid = uid,
            paymentProvider = paymentProvider,
            status = status,
            amount = amount,
            currency = currency,
            netAmount = netAmount,
            paymentMethodType = paymentMethodType,
            paymentMethodLast4 = paymentMethodLast4,
            cardBrand = cardBrand,
            description = description,
            billingPeriodStart = billingPeriodStart,
            billingPeriodEnd = billingPeriodEnd,
            paidAt = paidAt,
            failureReason = failureReason,
            receiptUrl = receiptUrl,
            invoicePdfUrl = invoicePdfUrl,
            createdAt = createdAt
        )
    }
}

@Serializable
data class PaymentMethodResponseDto(
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
    val isDefault: Boolean,
    @SerialName("is_expired")
    val isExpired: Boolean,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toModel(): PaymentMethod {
        return PaymentMethod(
            uid = uid,
            paymentProvider = paymentProvider,
            type = type,
            last4 = last4,
            brand = brand,
            expMonth = expMonth,
            expYear = expYear,
            cardholderName = cardholderName,
            upiId = upiId,
            bankName = bankName,
            isDefault = isDefault,
            isExpired = isExpired,
            displayName = displayName,
            createdAt = createdAt
        )
    }
}

// ======================
// Sync DTOs
// ======================

@Serializable
data class SyncSubscriptionResponseDto(
    val subscription: SubscriptionResponseDto,
    val device: DeviceRegistrationResponseDto,
    val usage: UsageResponseDto?,
    @SerialName("server_time")
    val serverTime: String
) {
    fun toModel(): SyncSubscriptionResponse {
        return SyncSubscriptionResponse(
            subscription = subscription.toModel(),
            device = device.toModel(),
            usage = usage?.toModel(),
            serverTime = serverTime
        )
    }
}

// ======================
// Change Plan DTOs
// ======================

@Serializable
data class ChangePlanResponseDto(
    val subscription: SubscriptionResponseDto,
    @SerialName("proration_amount")
    val prorationAmount: Double? = null,
    @SerialName("effective_at")
    val effectiveAt: String,
    @SerialName("is_immediate")
    val isImmediate: Boolean
) {
    fun toModel(): ChangePlanResponse {
        return ChangePlanResponse(
            subscription = subscription.toModel(),
            prorationAmount = prorationAmount,
            effectiveAt = effectiveAt,
            isImmediate = isImmediate
        )
    }
}

// ======================
// Purchase DTOs
// ======================

@Serializable
data class InitiatePurchaseResponseDto(
    @SerialName("checkout_url")
    val checkoutUrl: String? = null,
    @SerialName("checkout_session_id")
    val checkoutSessionId: String? = null,
    val provider: PaymentProvider,
    @SerialName("subscription_id")
    val subscriptionId: String? = null,
    @SerialName("razorpay_order_id")
    val razorpayOrderId: String? = null,
    @SerialName("razorpay_subscription_id")
    val razorpaySubscriptionId: String? = null,
    @SerialName("stripe_client_secret")
    val stripeClientSecret: String? = null,
    val amount: Double,
    val currency: String
) {
    fun toModel(): InitiatePurchaseResponse {
        return InitiatePurchaseResponse(
            checkoutUrl = checkoutUrl,
            checkoutSessionId = checkoutSessionId,
            provider = provider,
            subscriptionId = subscriptionId,
            razorpayOrderId = razorpayOrderId,
            razorpaySubscriptionId = razorpaySubscriptionId,
            stripeClientSecret = stripeClientSecret,
            amount = amount,
            currency = currency
        )
    }
}

// ======================
// Limit Check DTOs
// ======================

@Serializable
data class LimitCheckResultDto(
    val allowed: Boolean,
    val limit: Int,
    val current: Int,
    val remaining: Int,
    @SerialName("is_unlimited")
    val isUnlimited: Boolean = false,
    val exceeded: Boolean = false,
    val warning: Boolean = false
) {
    fun toModel(): LimitCheckResult {
        return LimitCheckResult(
            allowed = allowed,
            limit = limit,
            current = current,
            remaining = remaining,
            isUnlimited = isUnlimited,
            exceeded = exceeded,
            warning = warning
        )
    }
}
