package com.ampairs.subscription.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for cached subscription state
 */
@Entity(
    tableName = "subscriptions",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["workspace_id"]),
        Index(value = ["status"])
    ]
)
data class SubscriptionEntity(
    @PrimaryKey val uid: String,
    val workspace_id: String,
    val plan_code: String,
    val status: String,
    val billing_cycle: String,
    val payment_provider: String?,
    val currency: String,
    val current_period_start: String?,
    val current_period_end: String?,
    val trial_ends_at: String?,
    val cancel_at_period_end: Boolean,
    val cancelled_at: String?,
    val next_billing_amount: Double?,
    val last_payment_status: String?,
    val last_payment_at: String?,
    val is_free: Boolean,
    val days_remaining: Long,
    val created_at: String?,
    val updated_at: String?,
    val synced: Boolean = false,
    val last_sync: Long = 0
)

/**
 * Room entity for subscription plan definitions
 */
@Entity(
    tableName = "subscription_plans",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["plan_code"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class SubscriptionPlanEntity(
    @PrimaryKey val uid: String,
    val plan_code: String,
    val display_name: String,
    val description: String?,
    val monthly_price_inr: Double,
    val monthly_price_usd: Double,
    // Limits
    val max_workspaces: Int,
    val max_members_per_workspace: Int,
    val max_storage_gb: Int,
    val max_customers: Int,
    val max_products: Int,
    val max_invoices_per_month: Int,
    val max_devices: Int,
    val data_retention_years: Int,
    // Features (stored as JSON)
    val available_modules: String, // JSON array
    val api_access_enabled: Boolean,
    val custom_branding_enabled: Boolean,
    val sso_enabled: Boolean,
    val audit_logs_enabled: Boolean,
    val priority_support: Boolean,
    // Trial
    val trial_days: Int,
    // Discounts (stored as JSON)
    val multi_workspace_discount_json: String?, // JSON object
    val seasonal_discount_json: String?, // JSON object
    val pre_launch_discount_json: String?, // JSON object
    // Product IDs
    val google_play_product_id_monthly: String?,
    val google_play_product_id_annual: String?,
    val app_store_product_id_monthly: String?,
    val app_store_product_id_annual: String?,
    // Metadata
    val is_active: Boolean = true, // Track if plan is currently available
    val display_order: Int,
    val last_sync: Long = 0
)

/**
 * Room entity for device registration
 */
@Entity(
    tableName = "device_registrations",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["device_id"]),
        Index(value = ["workspace_id"])
    ]
)
data class DeviceRegistrationEntity(
    @PrimaryKey val uid: String,
    val device_id: String,
    val workspace_id: String,
    val device_name: String?,
    val platform: String,
    val device_model: String?,
    val os_version: String?,
    val app_version: String?,
    val token_expires_at: String,
    val last_sync_at: String?,
    val last_activity_at: String?,
    val is_active: Boolean,
    val access_mode: String,
    val created_at: String?,
    val last_sync: Long = 0
)

/**
 * Room entity for usage metrics cache
 */
@Entity(
    tableName = "usage_metrics",
    indices = [
        Index(value = ["workspace_id", "period_year", "period_month"], unique = true)
    ]
)
data class UsageMetricsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspace_id: String,
    val period_year: Int,
    val period_month: Int,
    // Usage counts
    val customer_count: Int,
    val product_count: Int,
    val invoice_count: Int,
    val order_count: Int,
    val member_count: Int,
    val device_count: Int,
    val storage_used_gb: Double,
    val api_calls: Long,
    val sms_count: Int,
    val email_count: Int,
    // Limits
    val max_customers: Int,
    val max_products: Int,
    val max_invoices_per_month: Int,
    val max_members: Int,
    val max_devices: Int,
    val max_storage_gb: Int,
    // Exceeded flags
    val customer_limit_exceeded: Boolean,
    val product_limit_exceeded: Boolean,
    val invoice_limit_exceeded: Boolean,
    val storage_limit_exceeded: Boolean,
    val member_limit_exceeded: Boolean,
    val device_limit_exceeded: Boolean,
    val has_any_exceeded: Boolean,
    // Metadata
    val last_calculated_at: String?,
    val last_sync: Long = 0
)
