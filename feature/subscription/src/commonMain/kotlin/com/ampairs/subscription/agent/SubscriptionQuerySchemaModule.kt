package com.ampairs.subscription.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema for the subscription module (FR-016). Curated read-only columns only.
 * The usage_metrics per-limit "exceeded" flags are summarized via `has_any_exceeded`.
 */
@ContributesTo(WorkspaceScope::class)
interface SubscriptionQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("subscription")
        fun provideSubscriptionQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "subscription",
            tables = listOf(
                TableSchema(
                    "subscriptions",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Subscription UID"),
                        ColumnSchema("plan_code", "TEXT", "Active plan code"),
                        ColumnSchema("status", "TEXT", "Active/expired/cancelled"),
                        ColumnSchema("billing_cycle", "TEXT", "Monthly/annual"),
                        ColumnSchema("currency", "TEXT", "Billing currency"),
                        ColumnSchema("current_period_start", "TEXT", "Period start (ISO-8601)"),
                        ColumnSchema("current_period_end", "TEXT", "Period end (ISO-8601)"),
                        ColumnSchema("trial_ends_at", "TEXT", "Trial end (ISO-8601)"),
                        ColumnSchema("next_billing_amount", "REAL", "Next billing amount"),
                        ColumnSchema("is_free", "INTEGER", "1 if free plan/trial"),
                        ColumnSchema("days_remaining", "INTEGER", "Days until period end"),
                    ),
                ),
                TableSchema(
                    "subscription_plans",
                    listOf(
                        ColumnSchema("plan_code", "TEXT", "Plan code"),
                        ColumnSchema("display_name", "TEXT", "Plan name"),
                        ColumnSchema("monthly_price_inr", "REAL", "Monthly price INR"),
                        ColumnSchema("monthly_price_usd", "REAL", "Monthly price USD"),
                        ColumnSchema("max_customers", "INTEGER", "Customer limit"),
                        ColumnSchema("max_products", "INTEGER", "Product limit"),
                        ColumnSchema("max_invoices_per_month", "INTEGER", "Monthly invoice limit"),
                        ColumnSchema("max_devices", "INTEGER", "Device limit"),
                        ColumnSchema("trial_days", "INTEGER", "Trial days"),
                        ColumnSchema("is_active", "INTEGER", "1 if plan available"),
                    ),
                ),
                TableSchema(
                    "usage_metrics",
                    listOf(
                        ColumnSchema("period_year", "INTEGER", "Year (YYYY)"),
                        ColumnSchema("period_month", "INTEGER", "Month (1-12)"),
                        ColumnSchema("customer_count", "INTEGER", "Customers used"),
                        ColumnSchema("product_count", "INTEGER", "Products used"),
                        ColumnSchema("invoice_count", "INTEGER", "Invoices issued"),
                        ColumnSchema("order_count", "INTEGER", "Orders placed"),
                        ColumnSchema("device_count", "INTEGER", "Devices registered"),
                        ColumnSchema("storage_used_gb", "REAL", "Storage used (GB)"),
                        ColumnSchema("has_any_exceeded", "INTEGER", "1 if any limit exceeded"),
                        ColumnSchema("last_calculated_at", "TEXT", "Calculated at (ISO-8601)"),
                    ),
                ),
                TableSchema(
                    "device_registrations",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Device registration UID"),
                        ColumnSchema("device_name", "TEXT", "Device name"),
                        ColumnSchema("platform", "TEXT", "Android/iOS/desktop"),
                        ColumnSchema("device_model", "TEXT", "Device model"),
                        ColumnSchema("app_version", "TEXT", "App version"),
                        ColumnSchema("last_activity_at", "TEXT", "Last activity (ISO-8601)"),
                        ColumnSchema("is_active", "INTEGER", "1 if active"),
                        ColumnSchema("access_mode", "TEXT", "Full/read-only"),
                    ),
                ),
            ),
        )
    }
}
