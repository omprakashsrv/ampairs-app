@file:OptIn(ExperimentalTime::class)

package com.ampairs.subscription.db

import com.ampairs.subscription.domain.model.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the subscription domain models.

fun SubscriptionState.toEntity(): SubscriptionEntity = SubscriptionEntity(
    uid = uid,
    workspace_id = workspaceId,
    plan_code = planCode,
    status = status.name,
    billing_cycle = billingCycle.name,
    payment_provider = paymentProvider?.name,
    currency = currency,
    current_period_start = currentPeriodStart,
    current_period_end = currentPeriodEnd,
    trial_ends_at = trialEndsAt,
    cancel_at_period_end = cancelAtPeriodEnd,
    cancelled_at = cancelledAt,
    next_billing_amount = nextBillingAmount,
    last_payment_status = lastPaymentStatus?.name,
    last_payment_at = lastPaymentAt,
    is_free = isFree,
    days_remaining = daysRemaining,
    created_at = createdAt,
    updated_at = updatedAt,
    synced = true,
    last_sync = Clock.System.now().toEpochMilliseconds()
)

fun SubscriptionEntity.toDomain(): SubscriptionState = SubscriptionState(
    uid = uid,
    workspaceId = workspace_id,
    planCode = plan_code,
    plan = null, // Plan details fetched separately if needed
    status = SubscriptionStatus.fromCode(status),
    billingCycle = BillingCycle.fromCode(billing_cycle),
    paymentProvider = payment_provider?.let { PaymentProvider.fromCode(it) },
    currency = currency,
    currentPeriodStart = current_period_start,
    currentPeriodEnd = current_period_end,
    trialEndsAt = trial_ends_at,
    cancelAtPeriodEnd = cancel_at_period_end,
    cancelledAt = cancelled_at,
    nextBillingAmount = next_billing_amount,
    lastPaymentStatus = last_payment_status?.let {
        try { PaymentStatus.valueOf(it) } catch (_: Exception) { null }
    },
    lastPaymentAt = last_payment_at,
    isFree = is_free,
    daysRemaining = days_remaining,
    activeAddons = emptyList(), // Addons fetched separately
    createdAt = created_at,
    updatedAt = updated_at
)

fun SubscriptionPlanDefinition.toEntity(): SubscriptionPlanEntity {
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    return SubscriptionPlanEntity(
        uid = uid,
        plan_code = planCode,
        display_name = displayName,
        description = description,
        monthly_price_inr = monthlyPriceInr,
        monthly_price_usd = monthlyPriceUsd,
        max_workspaces = limits.maxWorkspaces,
        max_members_per_workspace = limits.maxMembersPerWorkspace,
        max_storage_gb = limits.maxStorageGb,
        max_customers = limits.maxCustomers,
        max_products = limits.maxProducts,
        max_invoices_per_month = limits.maxInvoicesPerMonth,
        max_devices = limits.maxDevices,
        data_retention_years = limits.dataRetentionYears,
        available_modules = features.availableModules.joinToString(","),
        api_access_enabled = features.apiAccessEnabled,
        custom_branding_enabled = features.customBrandingEnabled,
        sso_enabled = features.ssoEnabled,
        audit_logs_enabled = features.auditLogsEnabled,
        priority_support = features.prioritySupport,
        trial_days = trialDays,
        multi_workspace_discount_json = try {
            json.encodeToString(MultiWorkspaceDiscount.serializer(), multiWorkspaceDiscount)
        } catch (_: Exception) { null },
        seasonal_discount_json = try {
            json.encodeToString(SeasonalDiscount.serializer(), seasonalDiscount)
        } catch (_: Exception) { null },
        pre_launch_discount_json = try {
            json.encodeToString(PreLaunchDiscount.serializer(), preLaunchDiscount)
        } catch (_: Exception) { null },
        google_play_product_id_monthly = googlePlayProductIdMonthly,
        google_play_product_id_annual = googlePlayProductIdAnnual,
        app_store_product_id_monthly = appStoreProductIdMonthly,
        app_store_product_id_annual = appStoreProductIdAnnual,
        is_active = true, // Plans from API are always active
        display_order = displayOrder,
        last_sync = Clock.System.now().toEpochMilliseconds()
    )
}

fun SubscriptionPlanEntity.toDomain(): SubscriptionPlanDefinition {
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    return SubscriptionPlanDefinition(
        uid = uid,
        planCode = plan_code,
        displayName = display_name,
        description = description,
        monthlyPriceInr = monthly_price_inr,
        monthlyPriceUsd = monthly_price_usd,
        limits = SubscriptionLimits(
            maxWorkspaces = max_workspaces,
            maxMembersPerWorkspace = max_members_per_workspace,
            maxStorageGb = max_storage_gb,
            maxCustomers = max_customers,
            maxProducts = max_products,
            maxInvoicesPerMonth = max_invoices_per_month,
            maxDevices = max_devices,
            dataRetentionYears = data_retention_years
        ),
        features = SubscriptionFeatures(
            availableModules = available_modules.split(",").filter { it.isNotBlank() },
            apiAccessEnabled = api_access_enabled,
            customBrandingEnabled = custom_branding_enabled,
            ssoEnabled = sso_enabled,
            auditLogsEnabled = audit_logs_enabled,
            prioritySupport = priority_support
        ),
        trialDays = trial_days,
        multiWorkspaceDiscount = multi_workspace_discount_json?.let {
            try {
                json.decodeFromString(MultiWorkspaceDiscount.serializer(), it)
            } catch (_: Exception) { MultiWorkspaceDiscount() }
        } ?: MultiWorkspaceDiscount(),
        seasonalDiscount = seasonal_discount_json?.let {
            try {
                json.decodeFromString(SeasonalDiscount.serializer(), it)
            } catch (_: Exception) { SeasonalDiscount() }
        } ?: SeasonalDiscount(),
        preLaunchDiscount = pre_launch_discount_json?.let {
            try {
                json.decodeFromString(PreLaunchDiscount.serializer(), it)
            } catch (_: Exception) { PreLaunchDiscount() }
        } ?: PreLaunchDiscount(),
        googlePlayProductIdMonthly = google_play_product_id_monthly,
        googlePlayProductIdAnnual = google_play_product_id_annual,
        appStoreProductIdMonthly = app_store_product_id_monthly,
        appStoreProductIdAnnual = app_store_product_id_annual,
        displayOrder = display_order
    )
}

fun DeviceRegistration.toEntity(workspaceId: String): DeviceRegistrationEntity = DeviceRegistrationEntity(
    uid = uid,
    device_id = deviceId,
    workspace_id = workspaceId,
    device_name = deviceName,
    platform = platform.name,
    device_model = deviceModel,
    os_version = osVersion,
    app_version = appVersion,
    token_expires_at = tokenExpiresAt,
    last_sync_at = lastSyncAt,
    last_activity_at = lastActivityAt,
    is_active = isActive,
    access_mode = accessMode.name,
    created_at = createdAt,
    last_sync = Clock.System.now().toEpochMilliseconds()
)

fun DeviceRegistrationEntity.toDomain(): DeviceRegistration = DeviceRegistration(
    uid = uid,
    deviceId = device_id,
    deviceName = device_name,
    platform = try { DevicePlatform.valueOf(platform) } catch (_: Exception) { DevicePlatform.ANDROID },
    deviceModel = device_model,
    osVersion = os_version,
    appVersion = app_version,
    tokenExpiresAt = token_expires_at,
    lastSyncAt = last_sync_at,
    lastActivityAt = last_activity_at,
    isActive = is_active,
    accessMode = try { SubscriptionAccessMode.valueOf(access_mode) } catch (_: Exception) { SubscriptionAccessMode.FULL_ACCESS },
    createdAt = created_at
)

fun UsageMetrics.toEntity(): UsageMetricsEntity = UsageMetricsEntity(
    workspace_id = workspaceId,
    period_year = periodYear,
    period_month = periodMonth,
    customer_count = usage.customerCount,
    product_count = usage.productCount,
    invoice_count = usage.invoiceCount,
    order_count = usage.orderCount,
    member_count = usage.memberCount,
    device_count = usage.deviceCount,
    storage_used_gb = usage.storageUsedGb,
    api_calls = usage.apiCalls,
    sms_count = usage.smsCount,
    email_count = usage.emailCount,
    max_customers = limits.maxCustomers,
    max_products = limits.maxProducts,
    max_invoices_per_month = limits.maxInvoicesPerMonth,
    max_members = limits.maxMembers,
    max_devices = limits.maxDevices,
    max_storage_gb = limits.maxStorageGb,
    customer_limit_exceeded = exceeded.customerLimitExceeded,
    product_limit_exceeded = exceeded.productLimitExceeded,
    invoice_limit_exceeded = exceeded.invoiceLimitExceeded,
    storage_limit_exceeded = exceeded.storageLimitExceeded,
    member_limit_exceeded = exceeded.memberLimitExceeded,
    device_limit_exceeded = exceeded.deviceLimitExceeded,
    has_any_exceeded = exceeded.hasAnyExceeded,
    last_calculated_at = lastCalculatedAt,
    last_sync = Clock.System.now().toEpochMilliseconds()
)

fun UsageMetricsEntity.toDomain(): UsageMetrics = UsageMetrics(
    workspaceId = workspace_id,
    periodYear = period_year,
    periodMonth = period_month,
    usage = UsageDetails(
        customerCount = customer_count,
        productCount = product_count,
        invoiceCount = invoice_count,
        orderCount = order_count,
        memberCount = member_count,
        deviceCount = device_count,
        storageUsedGb = storage_used_gb,
        apiCalls = api_calls,
        smsCount = sms_count,
        emailCount = email_count
    ),
    limits = UsageLimits(
        maxCustomers = max_customers,
        maxProducts = max_products,
        maxInvoicesPerMonth = max_invoices_per_month,
        maxMembers = max_members,
        maxDevices = max_devices,
        maxStorageGb = max_storage_gb
    ),
    exceeded = ExceededLimits(
        customerLimitExceeded = customer_limit_exceeded,
        productLimitExceeded = product_limit_exceeded,
        invoiceLimitExceeded = invoice_limit_exceeded,
        storageLimitExceeded = storage_limit_exceeded,
        memberLimitExceeded = member_limit_exceeded,
        deviceLimitExceeded = device_limit_exceeded,
        hasAnyExceeded = has_any_exceeded
    ),
    lastCalculatedAt = last_calculated_at
)
