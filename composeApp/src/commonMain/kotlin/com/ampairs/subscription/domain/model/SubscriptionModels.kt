package com.ampairs.subscription.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subscription plan definition with limits and features
 */
@Serializable
data class SubscriptionPlanDefinition(
    val uid: String,
    @SerialName("plan_code")
    val planCode: String,
    @SerialName("display_name")
    val displayName: String,
    val description: String? = null,
    @SerialName("monthly_price_inr")
    val monthlyPriceInr: Double = 0.0,
    @SerialName("monthly_price_usd")
    val monthlyPriceUsd: Double = 0.0,
    val limits: SubscriptionLimits,
    val features: SubscriptionFeatures,
    @SerialName("trial_days")
    val trialDays: Int = 0,
    @SerialName("google_play_product_id_monthly")
    val googlePlayProductIdMonthly: String? = null,
    @SerialName("google_play_product_id_annual")
    val googlePlayProductIdAnnual: String? = null,
    @SerialName("app_store_product_id_monthly")
    val appStoreProductIdMonthly: String? = null,
    @SerialName("app_store_product_id_annual")
    val appStoreProductIdAnnual: String? = null,
    @SerialName("display_order")
    val displayOrder: Int = 0
) {
    /**
     * Get monthly price for given currency
     */
    fun getMonthlyPrice(currency: String): Double {
        return when (currency.uppercase()) {
            "INR" -> monthlyPriceInr
            "USD" -> monthlyPriceUsd
            else -> monthlyPriceUsd
        }
    }

    /**
     * Get product ID for platform and billing cycle
     */
    fun getProductId(provider: PaymentProvider, billingCycle: BillingCycle): String? {
        return when (provider) {
            PaymentProvider.GOOGLE_PLAY -> when (billingCycle) {
                BillingCycle.ANNUAL, BillingCycle.BIENNIAL -> googlePlayProductIdAnnual
                else -> googlePlayProductIdMonthly
            }
            PaymentProvider.APP_STORE -> when (billingCycle) {
                BillingCycle.ANNUAL, BillingCycle.BIENNIAL -> appStoreProductIdAnnual
                else -> appStoreProductIdMonthly
            }
            else -> null
        }
    }
}

/**
 * Subscription limits per plan
 */
@Serializable
data class SubscriptionLimits(
    @SerialName("max_workspaces")
    val maxWorkspaces: Int = 1,
    @SerialName("max_members_per_workspace")
    val maxMembersPerWorkspace: Int = 1,
    @SerialName("max_storage_gb")
    val maxStorageGb: Int = 1,
    @SerialName("max_customers")
    val maxCustomers: Int = 50,
    @SerialName("max_products")
    val maxProducts: Int = 50,
    @SerialName("max_invoices_per_month")
    val maxInvoicesPerMonth: Int = 20,
    @SerialName("max_devices")
    val maxDevices: Int = 2,
    @SerialName("data_retention_years")
    val dataRetentionYears: Int = 1
) {
    companion object {
        const val UNLIMITED = -1

        val FREE = SubscriptionLimits(
            maxWorkspaces = 1,
            maxMembersPerWorkspace = 1,
            maxStorageGb = 1,
            maxCustomers = 50,
            maxProducts = 50,
            maxInvoicesPerMonth = 20,
            maxDevices = 2,
            dataRetentionYears = 1
        )

        val STARTER = SubscriptionLimits(
            maxWorkspaces = 2,
            maxMembersPerWorkspace = 5,
            maxStorageGb = 5,
            maxCustomers = 500,
            maxProducts = 500,
            maxInvoicesPerMonth = 200,
            maxDevices = 5,
            dataRetentionYears = 2
        )

        val PROFESSIONAL = SubscriptionLimits(
            maxWorkspaces = 5,
            maxMembersPerWorkspace = 15,
            maxStorageGb = 25,
            maxCustomers = UNLIMITED,
            maxProducts = UNLIMITED,
            maxInvoicesPerMonth = UNLIMITED,
            maxDevices = 15,
            dataRetentionYears = 5
        )

        val ENTERPRISE = SubscriptionLimits(
            maxWorkspaces = UNLIMITED,
            maxMembersPerWorkspace = UNLIMITED,
            maxStorageGb = 100,
            maxCustomers = UNLIMITED,
            maxProducts = UNLIMITED,
            maxInvoicesPerMonth = UNLIMITED,
            maxDevices = UNLIMITED,
            dataRetentionYears = UNLIMITED
        )
    }

    fun isUnlimited(limit: Int): Boolean = limit == UNLIMITED
}

/**
 * Subscription features per plan
 */
@Serializable
data class SubscriptionFeatures(
    @SerialName("available_modules")
    val availableModules: List<String> = listOf("CUSTOMER", "PRODUCT", "INVOICE"),
    @SerialName("api_access_enabled")
    val apiAccessEnabled: Boolean = false,
    @SerialName("custom_branding_enabled")
    val customBrandingEnabled: Boolean = false,
    @SerialName("sso_enabled")
    val ssoEnabled: Boolean = false,
    @SerialName("audit_logs_enabled")
    val auditLogsEnabled: Boolean = false,
    @SerialName("priority_support")
    val prioritySupport: Boolean = false
) {
    companion object {
        val FREE = SubscriptionFeatures(
            availableModules = listOf("CUSTOMER", "PRODUCT", "INVOICE"),
            apiAccessEnabled = false,
            customBrandingEnabled = false,
            ssoEnabled = false,
            auditLogsEnabled = false,
            prioritySupport = false
        )

        val STARTER = SubscriptionFeatures(
            availableModules = listOf("CUSTOMER", "PRODUCT", "INVOICE", "ORDER"),
            apiAccessEnabled = false,
            customBrandingEnabled = false,
            ssoEnabled = false,
            auditLogsEnabled = false,
            prioritySupport = false
        )

        val PROFESSIONAL = SubscriptionFeatures(
            availableModules = listOf("CUSTOMER", "PRODUCT", "INVOICE", "ORDER", "ANALYTICS", "REPORTS"),
            apiAccessEnabled = true,
            customBrandingEnabled = true,
            ssoEnabled = false,
            auditLogsEnabled = false,
            prioritySupport = true
        )

        val ENTERPRISE = SubscriptionFeatures(
            availableModules = listOf("CUSTOMER", "PRODUCT", "INVOICE", "ORDER", "ANALYTICS", "REPORTS", "INTEGRATIONS", "CUSTOM"),
            apiAccessEnabled = true,
            customBrandingEnabled = true,
            ssoEnabled = true,
            auditLogsEnabled = true,
            prioritySupport = true
        )
    }

    fun hasModule(moduleCode: String): Boolean {
        return availableModules.any { it.equals(moduleCode, ignoreCase = true) }
    }
}

/**
 * Current subscription state for a workspace
 */
@Serializable
data class SubscriptionState(
    val uid: String,
    @SerialName("workspace_id")
    val workspaceId: String,
    @SerialName("plan_code")
    val planCode: String,
    val plan: SubscriptionPlanDefinition? = null,
    val status: SubscriptionStatus,
    @SerialName("billing_cycle")
    val billingCycle: BillingCycle,
    @SerialName("payment_provider")
    val paymentProvider: PaymentProvider? = null,
    val currency: String = "INR",
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
    val isFree: Boolean = true,
    @SerialName("days_remaining")
    val daysRemaining: Long = 0,
    @SerialName("active_addons")
    val activeAddons: List<SubscriptionAddon> = emptyList(),
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    /**
     * Check if subscription is currently active
     */
    fun isActive(): Boolean = status.isActive()

    /**
     * Check if subscription is in trial
     */
    fun isInTrial(): Boolean = status == SubscriptionStatus.TRIALING

    /**
     * Get the effective limits (from plan or defaults)
     */
    fun getLimits(): SubscriptionLimits {
        return plan?.limits ?: when (SubscriptionPlan.fromCode(planCode)) {
            SubscriptionPlan.FREE -> SubscriptionLimits.FREE
            SubscriptionPlan.STARTER -> SubscriptionLimits.STARTER
            SubscriptionPlan.PROFESSIONAL -> SubscriptionLimits.PROFESSIONAL
            SubscriptionPlan.ENTERPRISE -> SubscriptionLimits.ENTERPRISE
        }
    }

    /**
     * Get the effective features (from plan or defaults)
     */
    fun getFeatures(): SubscriptionFeatures {
        return plan?.features ?: when (SubscriptionPlan.fromCode(planCode)) {
            SubscriptionPlan.FREE -> SubscriptionFeatures.FREE
            SubscriptionPlan.STARTER -> SubscriptionFeatures.STARTER
            SubscriptionPlan.PROFESSIONAL -> SubscriptionFeatures.PROFESSIONAL
            SubscriptionPlan.ENTERPRISE -> SubscriptionFeatures.ENTERPRISE
        }
    }

    /**
     * Check if subscription has a specific add-on
     */
    fun hasAddon(addonCode: String): Boolean {
        return activeAddons.any {
            it.addonCode.name.equals(addonCode, ignoreCase = true) && it.isActive()
        }
    }

    companion object {
        /**
         * Create a default FREE subscription state
         */
        fun createFree(workspaceId: String): SubscriptionState {
            return SubscriptionState(
                uid = "",
                workspaceId = workspaceId,
                planCode = "FREE",
                status = SubscriptionStatus.ACTIVE,
                billingCycle = BillingCycle.MONTHLY,
                isFree = true
            )
        }
    }
}

/**
 * Subscription add-on
 */
@Serializable
data class SubscriptionAddon(
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
    fun isActive(): Boolean = status == SubscriptionStatus.ACTIVE
}

/**
 * Usage metrics for a workspace
 */
@Serializable
data class UsageMetrics(
    @SerialName("workspace_id")
    val workspaceId: String,
    @SerialName("period_year")
    val periodYear: Int,
    @SerialName("period_month")
    val periodMonth: Int,
    val usage: UsageDetails,
    val limits: UsageLimits,
    val exceeded: ExceededLimits,
    @SerialName("last_calculated_at")
    val lastCalculatedAt: String? = null
)

@Serializable
data class UsageDetails(
    @SerialName("customer_count")
    val customerCount: Int = 0,
    @SerialName("product_count")
    val productCount: Int = 0,
    @SerialName("invoice_count")
    val invoiceCount: Int = 0,
    @SerialName("order_count")
    val orderCount: Int = 0,
    @SerialName("member_count")
    val memberCount: Int = 0,
    @SerialName("device_count")
    val deviceCount: Int = 0,
    @SerialName("storage_used_gb")
    val storageUsedGb: Double = 0.0,
    @SerialName("api_calls")
    val apiCalls: Long = 0,
    @SerialName("sms_count")
    val smsCount: Int = 0,
    @SerialName("email_count")
    val emailCount: Int = 0
)

@Serializable
data class UsageLimits(
    @SerialName("max_customers")
    val maxCustomers: Int = 50,
    @SerialName("max_products")
    val maxProducts: Int = 50,
    @SerialName("max_invoices_per_month")
    val maxInvoicesPerMonth: Int = 20,
    @SerialName("max_members")
    val maxMembers: Int = 1,
    @SerialName("max_devices")
    val maxDevices: Int = 2,
    @SerialName("max_storage_gb")
    val maxStorageGb: Int = 1
)

@Serializable
data class ExceededLimits(
    @SerialName("customer_limit_exceeded")
    val customerLimitExceeded: Boolean = false,
    @SerialName("product_limit_exceeded")
    val productLimitExceeded: Boolean = false,
    @SerialName("invoice_limit_exceeded")
    val invoiceLimitExceeded: Boolean = false,
    @SerialName("storage_limit_exceeded")
    val storageLimitExceeded: Boolean = false,
    @SerialName("member_limit_exceeded")
    val memberLimitExceeded: Boolean = false,
    @SerialName("device_limit_exceeded")
    val deviceLimitExceeded: Boolean = false,
    @SerialName("has_any_exceeded")
    val hasAnyExceeded: Boolean = false
)
