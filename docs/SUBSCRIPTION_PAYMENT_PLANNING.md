# Ampairs Subscription & Payment System Planning

## Executive Summary

This document outlines the subscription model and payment integration strategy for Ampairs - a Kotlin Multiplatform business management application supporting Android, iOS, and Desktop platforms with multi-workspace, multi-user, and multi-device capabilities.

---

## 1. Recommended Subscription Models

### 1.1 Model Comparison for Multi-Workspace SaaS

| Model                               | Best For                                | Complexity | Revenue Predictability |
|-------------------------------------|-----------------------------------------|------------|------------------------|
| **Per-Workspace Tiered**            | Business tools with team collaboration  | Medium     | High                   |
| **Per-User (Seat-Based)**           | Enterprise software                     | Low        | High                   |
| **Per-Workspace + Per-User Hybrid** | Complex multi-tenant apps               | High       | Very High              |
| **Usage-Based (Metered)**           | API/Storage-heavy apps                  | High       | Variable               |
| **Feature-Based (Module)**          | Modular business apps                   | Medium     | Medium                 |
| **Flat Rate**                       | Simple apps                             | Low        | High                   |

### 1.2 Recommended Model: **Hybrid Workspace-Tier + Module Add-ons**

Given Ampairs' architecture with multi-workspace support and modular business features, the recommended approach is:

```
┌─────────────────────────────────────────────────────────────────┐
│                    SUBSCRIPTION ARCHITECTURE                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   User Account                                                   │
│   └── Workspace 1 (Pro Plan)                                     │
│       ├── Base Features (included in plan)                       │
│       ├── Member Seats (5 included, +$X per additional)          │
│       ├── Storage (10GB included, +$X per 10GB)                  │
│       └── Add-on Modules                                         │
│           ├── Tally Integration (+$X/month)                      │
│           └── Advanced Analytics (+$X/month)                     │
│                                                                  │
│   └── Workspace 2 (Starter Plan)                                 │
│       ├── Base Features (limited)                                │
│       ├── Member Seats (3 included)                              │
│       └── Storage (5GB included)                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Proposed Subscription Tiers

### 2.1 Tier Structure

#### **FREE TIER** - Try & Explore
| Feature               | Limit                                  |
|-----------------------|----------------------------------------|
| Workspaces            | 1                                      |
| Members per Workspace | 1                                      |
| Storage               | 500 MB                                 |
| Customers             | 50                                     |
| Products              | 50                                     |
| Invoices/month        | 20                                     |
| Devices               | 2                                      |
| Modules               | Core only (Customer, Product, Invoice) |
| Support               | Community                              |
| Data Retention        | 6 months                               |
| Trial Period          | -                                      |

#### **STARTER** - Small Business - ₹499/month or $7/month
| Feature               | Limit                   |
|-----------------------|-------------------------|
| Workspaces            | 2                       |
| Members per Workspace | 5                       |
| Storage               | 5 GB                    |
| Customers             | 500                     |
| Products              | 500                     |
| Invoices/month        | 200                     |
| Devices               | 5                       |
| Modules               | Core + Order Management |
| Support               | Email (48h response)    |
| Data Retention        | 2 years                 |
| Trial Period          | 14 days                 |

#### **PROFESSIONAL** - Growing Business - ₹1,499/month or $19/month
| Feature               | Limit                       |
|-----------------------|-----------------------------|
| Workspaces            | 5                           |
| Members per Workspace | 15                          |
| Storage               | 25 GB                       |
| Customers             | Unlimited                   |
| Products              | Unlimited                   |
| Invoices/month        | Unlimited                   |
| Devices               | 15                          |
| Modules               | All Standard Modules        |
| Support               | Priority Email (24h)        |
| Data Retention        | 5 years                     |
| Trial Period          | 14 days                     |
| Features              | Custom branding, API access |

#### **ENTERPRISE** - Large Organizations - ₹4,999/month or $59/month
| Feature               | Limit                                |
|-----------------------|--------------------------------------|
| Workspaces            | Unlimited                            |
| Members per Workspace | Unlimited                            |
| Storage               | 100 GB (expandable)                  |
| All Data Limits       | Unlimited                            |
| Devices               | Unlimited                            |
| Modules               | All + Premium Add-ons                |
| Support               | Phone + Dedicated Manager            |
| Data Retention        | Unlimited                            |
| Trial Period          | 30 days                              |
| Features              | SSO, Audit logs, Custom integrations |

### 2.2 Premium Add-on Modules (Available for Pro+ tiers)

| Module             | Price (Monthly) | Description                       |
|--------------------|-----------------|-----------------------------------|
| Tally Integration  | ₹299 / $4       | Sync with Tally ERP               |
| Advanced Analytics | ₹499 / $6       | Custom reports, dashboards        |
| Multi-Currency     | ₹199 / $3       | Support for multiple currencies   |
| E-Invoicing (GST)  | ₹399 / $5       | Government e-invoicing compliance |
| Inventory Pro      | ₹299 / $4       | Advanced stock management         |
| Custom Fields      | ₹199 / $3       | Unlimited custom fields           |

### 2.3 Usage-Based Overages

| Resource                | Overage Rate                  |
|-------------------------|-------------------------------|
| Additional Members      | ₹99 / $1.5 per seat/month     |
| Additional Storage      | ₹99 / $1.5 per 5GB/month      |
| Additional Workspaces   | ₹299 / $4 per workspace/month |
| SMS Notifications       | ₹0.25 / $0.003 per SMS        |
| Email Volume (>1000/mo) | ₹0.10 / $0.001 per email      |

---

## 3. Billing Frequency Options

### 3.1 Available Billing Cycles

| Cycle              | Discount | Best For               |
|--------------------|----------|------------------------|
| Monthly            | 0%       | Cash flow flexibility  |
| Quarterly          | 5%       | Short-term commitment  |
| Annual             | 20%      | Long-term savings      |
| Biennial (2 years) | 30%      | Maximum savings        |

### 3.2 Billing Model Considerations

```kotlin
// Existing workspace fields that need enhancement
data class Workspace(
    // Current fields
    val subscriptionPlan: String = "FREE",      // EXISTING
    val maxMembers: Int = 5,                    // EXISTING
    val storageLimitGb: Int = 1,                // EXISTING
    val trialExpiresAt: String? = null,         // EXISTING

    // Proposed additions for subscription management
    val subscriptionId: String? = null,          // Payment provider subscription ID
    val subscriptionStatus: String = "ACTIVE",   // ACTIVE, PAST_DUE, CANCELLED, PAUSED
    val billingCycle: String = "MONTHLY",        // MONTHLY, QUARTERLY, ANNUAL
    val currentPeriodStart: String? = null,
    val currentPeriodEnd: String? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val paymentProvider: String? = null,         // GOOGLE_PLAY, APP_STORE, STRIPE, RAZORPAY
    val currency: String = "INR",
    val lastPaymentStatus: String? = null,
    val nextBillingAmount: Double? = null,

    // Usage tracking
    val currentMemberCount: Int = 1,
    val currentStorageUsedGb: Double = 0.0,
    val invoicesThisMonth: Int = 0,

    // Add-on modules
    val activeAddons: List<String> = emptyList(),
)
```

---

## 4. Platform-Specific Payment Integration

### 4.1 Payment Provider Matrix

| Platform        | Primary Provider     | Region   | In-App Purchase            | External Payment     |
|-----------------|----------------------|----------|----------------------------|----------------------|
| Android         | Google Play Billing  | Global   | Required for digital goods | Physical goods only  |
| iOS             | Apple StoreKit 2     | Global   | Required (30% fee)         | Allowed in EU (DMA)  |
| Desktop (India) | Razorpay             | India    | N/A                        | Full control         |
| Desktop (Intl)  | Stripe               | Global   | N/A                        | Full control         |

### 4.2 Android - Google Play Billing Library v7

```kotlin
// Dependencies (build.gradle.kts)
implementation("com.android.billingclient:billing-ktx:7.0.0")

// Key Components
- BillingClient for purchase flow
- ProductDetails for subscription info
- Purchase for verification
- Google Play Console for subscription products

// Subscription Product IDs (configure in Play Console)
- com.ampairs.subscription.starter.monthly
- com.ampairs.subscription.starter.annual
- com.ampairs.subscription.professional.monthly
- com.ampairs.subscription.professional.annual
- com.ampairs.subscription.enterprise.monthly
- com.ampairs.subscription.enterprise.annual
- com.ampairs.addon.tally.monthly
- com.ampairs.addon.analytics.monthly
```

**Google Play Billing Flow:**
```
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  User taps   │────▶│  BillingClient  │────▶│  Google Play     │
│  Subscribe   │     │  launchBilling  │     │  Purchase Flow   │
└──────────────┘     └─────────────────┘     └──────────────────┘
                                                      │
                                                      ▼
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  Update      │◀────│  Backend        │◀────│  Purchase Token  │
│  Workspace   │     │  Verification   │     │  Callback        │
└──────────────┘     └─────────────────┘     └──────────────────┘
```

### 4.3 iOS - StoreKit 2

```swift
// Key Components
- Product for subscription details
- Transaction for purchase state
- AppStore.sync() for restoration
- Server-side validation with App Store Server API

// In-App Purchase IDs (configure in App Store Connect)
- com.ampairs.subscription.starter.monthly
- com.ampairs.subscription.starter.annual
- com.ampairs.subscription.professional.monthly
- com.ampairs.subscription.professional.annual
- com.ampairs.subscription.enterprise.monthly
- com.ampairs.subscription.enterprise.annual
```

**StoreKit 2 Flow:**
```
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  User taps   │────▶│  Product.       │────▶│  App Store       │
│  Subscribe   │     │  purchase()     │     │  Payment Sheet   │
└──────────────┘     └─────────────────┘     └──────────────────┘
                                                      │
                                                      ▼
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  Update      │◀────│  Server         │◀────│  Transaction     │
│  Workspace   │     │  Notification   │     │  Listener        │
└──────────────┘     └─────────────────┘     └──────────────────┘
```

### 4.4 Desktop - Razorpay (India) + Stripe (International)

```kotlin
// Razorpay Integration (India)
// - UPI, Cards, Net Banking, Wallets
// - Subscription API for recurring payments
// - Webhook for payment events

// Stripe Integration (International)
// - Cards, ACH, SEPA, and local payment methods
// - Stripe Billing for subscriptions
// - Customer Portal for self-service

// Desktop Payment Flow (WebView or System Browser)
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  User clicks │────▶│  Generate       │────▶│  Open Checkout   │
│  Subscribe   │     │  Checkout URL   │     │  (WebView/Browser)│
└──────────────┘     └─────────────────┘     └──────────────────┘
                                                      │
                                                      ▼
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  Update      │◀────│  Backend        │◀────│  Webhook/        │
│  Workspace   │     │  Verification   │     │  Redirect        │
└──────────────┘     └─────────────────┘     └──────────────────┘
```

---

## 5. Technical Architecture

### 5.1 High-Level Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                         AMPAIRS SUBSCRIPTION SYSTEM                     │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                        CLIENT LAYER                              │   │
│  │  ┌───────────┐  ┌───────────┐  ┌──────────────────────────────┐ │   │
│  │  │  Android  │  │    iOS    │  │         Desktop              │ │   │
│  │  │  ───────  │  │  ───────  │  │  ────────────────────────    │ │   │
│  │  │  Google   │  │  StoreKit │  │  Razorpay    │    Stripe    │ │   │
│  │  │  Play     │  │     2     │  │  (India)     │    (Intl)    │ │   │
│  │  │  Billing  │  │           │  │              │               │ │   │
│  │  └─────┬─────┘  └─────┬─────┘  └──────┬───────┴───────┬───────┘ │   │
│  └────────┼──────────────┼───────────────┼───────────────┼─────────┘   │
│           │              │               │               │             │
│           ▼              ▼               ▼               ▼             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    COMMON SUBSCRIPTION LAYER                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐│   │
│  │  │              SubscriptionManager (expect/actual)            ││   │
│  │  │  - purchaseSubscription(plan: SubscriptionPlan)             ││   │
│  │  │  - cancelSubscription()                                     ││   │
│  │  │  - restorePurchases()                                       ││   │
│  │  │  - getActiveSubscription(): Flow<SubscriptionState>         ││   │
│  │  │  - purchaseAddon(addon: AddonModule)                        ││   │
│  │  └─────────────────────────────────────────────────────────────┘│   │
│  │  ┌─────────────────────────────────────────────────────────────┐│   │
│  │  │              SubscriptionRepository                         ││   │
│  │  │  - syncSubscriptionStatus()                                 ││   │
│  │  │  - updateWorkspaceLimits()                                  ││   │
│  │  │  - checkFeatureAccess(feature: Feature): Boolean            ││   │
│  │  └─────────────────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                   │
│                                    ▼                                   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     BACKEND LAYER (Spring Boot)                  │   │
│  │  ┌─────────────────────────────────────────────────────────────┐│   │
│  │  │              SubscriptionService                            ││   │
│  │  │  - processPaymentWebhook(provider, payload)                 ││   │
│  │  │  - verifyPurchase(token, provider)                          ││   │
│  │  │  - updateSubscription(workspaceId, plan)                    ││   │
│  │  │  - handleRenewal/Cancellation/Expiry                        ││   │
│  │  └─────────────────────────────────────────────────────────────┘│   │
│  │  ┌─────────────────────────────────────────────────────────────┐│   │
│  │  │              Payment Provider Integrations                  ││   │
│  │  │  - GooglePlayVerifier (Real-time Developer Notifications)   ││   │
│  │  │  - AppStoreVerifier (App Store Server Notifications V2)     ││   │
│  │  │  - RazorpayWebhookHandler                                   ││   │
│  │  │  - StripeWebhookHandler                                     ││   │
│  │  └─────────────────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Data Models

```kotlin
// Common Subscription Models (commonMain)

enum class SubscriptionPlan {
    FREE,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE
}

enum class BillingCycle {
    MONTHLY,
    QUARTERLY,
    ANNUAL,
    BIENNIAL
}

enum class SubscriptionStatus {
    ACTIVE,
    TRIALING,
    PAST_DUE,
    PAUSED,
    CANCELLED,
    EXPIRED
}

enum class PaymentProvider {
    GOOGLE_PLAY,
    APP_STORE,
    RAZORPAY,
    STRIPE
}

@Serializable
data class SubscriptionState(
    val workspaceId: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val billingCycle: BillingCycle,
    val provider: PaymentProvider?,
    val currentPeriodStart: String?,
    val currentPeriodEnd: String?,
    val trialEndsAt: String?,
    val cancelAtPeriodEnd: Boolean = false,
    val limits: SubscriptionLimits,
    val activeAddons: List<AddonModule> = emptyList(),
)

@Serializable
data class SubscriptionLimits(
    val maxWorkspaces: Int,
    val maxMembersPerWorkspace: Int,
    val maxStorageGb: Int,
    val maxCustomers: Int,           // -1 for unlimited
    val maxProducts: Int,
    val maxInvoicesPerMonth: Int,
    val maxDevices: Int,
    val availableModules: List<String>,
    val apiAccessEnabled: Boolean,
    val customBrandingEnabled: Boolean,
    val dataRetentionYears: Int,
)

@Serializable
data class AddonModule(
    val moduleCode: String,
    val name: String,
    val monthlyPrice: Double,
    val currency: String,
    val status: String,  // ACTIVE, CANCELLED
    val activatedAt: String?,
)

@Serializable
data class PaymentMethod(
    val id: String,
    val type: String,       // CARD, UPI, BANK_ACCOUNT
    val last4: String?,     // Last 4 digits for cards
    val brand: String?,     // VISA, MASTERCARD, etc.
    val isDefault: Boolean,
    val expiryMonth: Int?,
    val expiryYear: Int?,
)

@Serializable
data class Invoice(
    val id: String,
    val workspaceId: String,
    val invoiceNumber: String,
    val amount: Double,
    val currency: String,
    val status: String,     // PAID, PENDING, FAILED
    val billingPeriodStart: String,
    val billingPeriodEnd: String,
    val lineItems: List<InvoiceLineItem>,
    val pdfUrl: String?,
    val createdAt: String,
    val paidAt: String?,
)

@Serializable
data class InvoiceLineItem(
    val description: String,
    val quantity: Int,
    val unitAmount: Double,
    val totalAmount: Double,
)
```

### 5.3 Platform-Specific Implementation

```kotlin
// commonMain - expect declarations
expect class SubscriptionManager {
    suspend fun getAvailableProducts(): List<SubscriptionProduct>
    suspend fun purchaseSubscription(product: SubscriptionProduct): PurchaseResult
    suspend fun purchaseAddon(addon: AddonProduct): PurchaseResult
    suspend fun restorePurchases(): RestoreResult
    fun observePurchases(): Flow<PurchaseUpdate>
}

// androidMain - Google Play Billing
actual class SubscriptionManager(
    private val context: Context,
    private val billingClient: BillingClient,
) {
    // Implementation using Google Play Billing Library v7
}

// iosMain - StoreKit 2
actual class SubscriptionManager {
    // Implementation using StoreKit 2 via Kotlin/Native
}

// desktopMain - Web-based checkout
actual class SubscriptionManager(
    private val httpClient: HttpClient,
    private val config: DesktopPaymentConfig,
) {
    // Implementation using Razorpay/Stripe checkout URLs
}
```

---

## 6. Feature Gating & Entitlements

### 6.1 Feature Access Control

```kotlin
// Feature Gating System
object FeatureGate {

    fun canAccess(feature: Feature, subscription: SubscriptionState): Boolean {
        return when (feature) {
            Feature.CUSTOMER_MANAGEMENT -> true  // All tiers
            Feature.PRODUCT_MANAGEMENT -> true   // All tiers
            Feature.INVOICE_BASIC -> true        // All tiers

            Feature.ORDER_MANAGEMENT ->
                subscription.plan >= SubscriptionPlan.STARTER

            Feature.TALLY_INTEGRATION ->
                subscription.hasAddon("tally-integration")

            Feature.ADVANCED_ANALYTICS ->
                subscription.hasAddon("advanced-analytics")

            Feature.API_ACCESS ->
                subscription.plan >= SubscriptionPlan.PROFESSIONAL

            Feature.CUSTOM_BRANDING ->
                subscription.plan >= SubscriptionPlan.PROFESSIONAL

            Feature.SSO ->
                subscription.plan == SubscriptionPlan.ENTERPRISE

            Feature.AUDIT_LOGS ->
                subscription.plan == SubscriptionPlan.ENTERPRISE
        }
    }

    fun checkLimit(limit: Limit, current: Int, subscription: SubscriptionState): LimitStatus {
        val max = when (limit) {
            Limit.WORKSPACES -> subscription.limits.maxWorkspaces
            Limit.MEMBERS -> subscription.limits.maxMembersPerWorkspace
            Limit.CUSTOMERS -> subscription.limits.maxCustomers
            Limit.PRODUCTS -> subscription.limits.maxProducts
            Limit.INVOICES_PER_MONTH -> subscription.limits.maxInvoicesPerMonth
            Limit.STORAGE_GB -> subscription.limits.maxStorageGb
            Limit.DEVICES -> subscription.limits.maxDevices
        }

        return when {
            max == -1 -> LimitStatus.UNLIMITED
            current < max * 0.8 -> LimitStatus.OK
            current < max -> LimitStatus.WARNING(remaining = max - current)
            else -> LimitStatus.EXCEEDED
        }
    }
}
```

### 6.2 UI Integration

```kotlin
// Usage in Composables
@Composable
fun CustomerListScreen(viewModel: CustomerListViewModel = koinInject()) {
    val subscription by viewModel.subscriptionState.collectAsState()
    val customers by viewModel.customers.collectAsState()

    // Check limit before showing "Add" button
    val customerLimit = FeatureGate.checkLimit(
        Limit.CUSTOMERS,
        customers.size,
        subscription
    )

    when (customerLimit) {
        is LimitStatus.EXCEEDED -> {
            UpgradePromptBanner(
                message = "You've reached your customer limit. Upgrade to add more.",
                targetPlan = SubscriptionPlan.PROFESSIONAL
            )
        }
        is LimitStatus.WARNING -> {
            LimitWarningBanner(
                message = "Only ${customerLimit.remaining} customer slots remaining"
            )
        }
        else -> { /* Normal UI */ }
    }
}
```

---

## 7. Offline-First Subscription Enforcement

### 7.1 Implementation Complexity Overview

| Component | Server-Side | App-Side (with Offline) |
|-----------|-------------|-------------------------|
| Subscription CRUD | Easy | Medium |
| Payment webhooks | Medium | N/A |
| Feature gating | Easy | Medium |
| Usage metering | Medium | Challenging |
| Device tracking | Medium | Challenging |
| Limit enforcement | Easy | Challenging |

**Core Challenge**: How do you enforce subscription limits when the app can't reach the server?

### 7.2 Device Limit Enforcement

#### Problem Scenario
```
┌─────────────────────────────────────────────────────────────┐
│ User has Free tier (2 devices allowed)                      │
│                                                             │
│  Device A (Phone)     Device B (Tablet)    Device C (New)   │
│  ✅ Registered        ✅ Registered         ❓ Wants access  │
│  [OFFLINE]            [OFFLINE]             [ONLINE]        │
│                                                             │
│ Server can't revoke A or B because they're offline!         │
└─────────────────────────────────────────────────────────────┘
```

#### Solution Approaches

| Approach | Pros | Cons | Recommendation |
|----------|------|------|----------------|
| **Token Expiry** | Simple, forces periodic sync | User must go online periodically | ✅ Recommended |
| **Device Priority Queue** | FIFO deregistration | Complex, confusing UX | ❌ Not recommended |
| **Soft Limit + Grace** | Good UX | Can be abused | ⚠️ Secondary option |

#### Recommended Implementation

```kotlin
// Device Registration with Token Expiry
@Serializable
data class DeviceRegistration(
    val deviceId: String,
    val workspaceId: String,
    val deviceName: String,
    val platform: String,              // ANDROID, IOS, DESKTOP
    val registeredAt: String,
    val tokenExpiresAt: String,        // Must sync before this (e.g., 7 days)
    val lastSyncAt: String,
    val isActive: Boolean,
)

// App startup check
class DeviceAuthManager(
    private val deviceRegistration: DeviceRegistration,
    private val subscriptionRepository: SubscriptionRepository,
) {
    fun canUseApp(): DeviceAccessResult {
        val now = Clock.System.now()
        val expiresAt = Instant.parse(deviceRegistration.tokenExpiresAt)

        return when {
            now < expiresAt -> DeviceAccessResult.ALLOWED
            now < expiresAt.plus(3.days) -> DeviceAccessResult.GRACE_PERIOD
            else -> DeviceAccessResult.EXPIRED_MUST_SYNC
        }
    }

    suspend fun refreshDeviceToken(): Result<DeviceRegistration> {
        // Call server to extend token and verify device is still registered
        return subscriptionRepository.refreshDeviceRegistration(deviceRegistration.deviceId)
    }
}

sealed class DeviceAccessResult {
    object ALLOWED : DeviceAccessResult()
    object GRACE_PERIOD : DeviceAccessResult()  // Show warning, allow use
    object EXPIRED_MUST_SYNC : DeviceAccessResult()  // Force online sync
}
```

#### Server-Side Device Management

```kotlin
// Backend: Device Registration Service
class DeviceRegistrationService {

    fun registerDevice(
        userId: String,
        workspaceId: String,
        deviceInfo: DeviceInfo
    ): Result<DeviceRegistration> {
        val subscription = subscriptionRepository.getByWorkspaceId(workspaceId)
        val currentDevices = deviceRepository.getActiveDevices(userId, workspaceId)

        if (currentDevices.size >= subscription.limits.maxDevices) {
            return Result.failure(DeviceLimitExceededException(
                current = currentDevices.size,
                limit = subscription.limits.maxDevices,
                devices = currentDevices  // Allow user to deregister one
            ))
        }

        val registration = DeviceRegistration(
            deviceId = generateDeviceId(),
            workspaceId = workspaceId,
            tokenExpiresAt = Clock.System.now().plus(7.days).toString(),
            // ... other fields
        )

        return Result.success(deviceRepository.save(registration))
    }

    fun refreshToken(deviceId: String): Result<DeviceRegistration> {
        val device = deviceRepository.findById(deviceId)
            ?: return Result.failure(DeviceNotFoundException())

        // Extend token by 7 days
        val updated = device.copy(
            tokenExpiresAt = Clock.System.now().plus(7.days).toString(),
            lastSyncAt = Clock.System.now().toString()
        )

        return Result.success(deviceRepository.save(updated))
    }
}
```

### 7.3 Data Limit Enforcement (Offline)

#### Problem Scenario
```
┌─────────────────────────────────────────────────────────────┐
│ Free tier: 50 customers allowed                             │
│                                                             │
│ User creates customers while OFFLINE:                       │
│   - Already has 48 customers                                │
│   - Creates 5 more offline (now has 53)                     │
│   - Goes online → Server rejects? Deletes? Syncs?           │
└─────────────────────────────────────────────────────────────┘
```

#### Solution: Client-Side Primary + Server Graceful Handling

| Layer | Enforcement | Behavior |
|-------|-------------|----------|
| **Client (Primary)** | Check local count before create | Prevent exceeding limit |
| **Server (Secondary)** | Accept sync, flag for upgrade | No data loss |

#### Client-Side Enforcement

```kotlin
// Repository layer - check before any create operation
class CustomerRepository(
    private val customerDao: CustomerDao,
    private val subscriptionState: Flow<SubscriptionState>,
) {
    suspend fun createCustomer(customer: Customer): Result<Customer> {
        val subscription = subscriptionState.first()
        val currentCount = customerDao.getCount()
        val limit = subscription.limits.maxCustomers

        // Enforce limit locally
        if (limit != -1 && currentCount >= limit) {
            return Result.failure(LimitExceededException(
                resource = "customers",
                current = currentCount,
                limit = limit,
                suggestedPlan = getSuggestedUpgradePlan(subscription.plan)
            ))
        }

        // Proceed with offline-first creation
        val unsyncedCustomer = customer.copy(synced = false)
        customerDao.insert(unsyncedCustomer.toEntity())

        // Background sync will handle server communication
        return Result.success(unsyncedCustomer)
    }
}

// ViewModel - handle limit errors gracefully
class CustomerFormViewModel : ViewModel() {

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.createCustomer(customer)
                .onSuccess {
                    _uiState.value = UiState.Success(it)
                }
                .onFailure { error ->
                    when (error) {
                        is LimitExceededException -> {
                            _uiState.value = UiState.LimitReached(
                                message = "You've reached your ${error.resource} limit (${error.limit})",
                                suggestedPlan = error.suggestedPlan
                            )
                        }
                        else -> _uiState.value = UiState.Error(error.message)
                    }
                }
        }
    }
}
```

#### Server-Side Graceful Handling

```kotlin
// Backend: Accept over-limit data but flag workspace
class CustomerSyncService {

    fun syncCustomers(
        workspaceId: String,
        customers: List<Customer>
    ): SyncResult {
        val subscription = subscriptionRepository.getByWorkspaceId(workspaceId)
        val existingCount = customerRepository.countByWorkspace(workspaceId)
        val newCount = existingCount + customers.size

        // Always accept the data (no data loss)
        customerRepository.saveAll(customers)

        // But flag if over limit
        if (subscription.limits.maxCustomers != -1 &&
            newCount > subscription.limits.maxCustomers) {

            workspaceRepository.flagOverLimit(workspaceId, "customers", newCount)

            return SyncResult(
                success = true,
                syncedCount = customers.size,
                warning = OverLimitWarning(
                    resource = "customers",
                    current = newCount,
                    limit = subscription.limits.maxCustomers,
                    action = "upgrade_required_for_new_creates"
                )
            )
        }

        return SyncResult(success = true, syncedCount = customers.size)
    }
}
```

### 7.4 Subscription Expiry While Offline

#### Problem Scenario
```
┌─────────────────────────────────────────────────────────────┐
│ User has STARTER plan, goes offline for 2 weeks             │
│ Subscription expires/fails to renew during offline period   │
│                                                             │
│ Options:                                                    │
│  A) App continues working (abuse potential)                 │
│  B) App locks immediately when token expires (harsh)        │
│  C) App works in read-only mode (balanced)                  │
└─────────────────────────────────────────────────────────────┘
```

#### Recommended: Graceful Degradation Model

```kotlin
enum class SubscriptionAccessMode {
    FULL_ACCESS,      // Active subscription, recently verified
    OFFLINE_GRACE,    // Token valid but can't verify (allow full access)
    READ_ONLY,        // Subscription likely expired (view only, no creates)
    LOCKED,           // Extended non-payment (must go online)
}

class SubscriptionAccessManager(
    private val subscriptionState: SubscriptionState,
    private val deviceRegistration: DeviceRegistration,
) {
    fun determineAccessMode(): SubscriptionAccessMode {
        val now = Clock.System.now()
        val tokenExpiry = Instant.parse(deviceRegistration.tokenExpiresAt)
        val subscriptionEnd = subscriptionState.currentPeriodEnd?.let { Instant.parse(it) }

        return when {
            // Token fresh and subscription active
            now < tokenExpiry && subscriptionState.status == SubscriptionStatus.ACTIVE ->
                SubscriptionAccessMode.FULL_ACCESS

            // Token expired but within grace period (3 days)
            now < tokenExpiry.plus(3.days) ->
                SubscriptionAccessMode.OFFLINE_GRACE

            // Token expired, subscription end date passed (if known)
            subscriptionEnd != null && now > subscriptionEnd.plus(7.days) ->
                SubscriptionAccessMode.READ_ONLY

            // Extended offline (> 14 days without sync)
            now > tokenExpiry.plus(14.days) ->
                SubscriptionAccessMode.LOCKED

            // Default to grace period for edge cases
            else -> SubscriptionAccessMode.OFFLINE_GRACE
        }
    }
}
```

#### UI Behavior Per Access Mode

```kotlin
@Composable
fun AppContent(accessMode: SubscriptionAccessMode) {
    when (accessMode) {
        SubscriptionAccessMode.FULL_ACCESS -> {
            // Normal app experience
            MainAppContent()
        }

        SubscriptionAccessMode.OFFLINE_GRACE -> {
            // Show subtle warning banner
            Column {
                OfflineGraceBanner(
                    message = "You're offline. Some features may be limited.",
                    onSyncClick = { /* trigger sync */ }
                )
                MainAppContent()
            }
        }

        SubscriptionAccessMode.READ_ONLY -> {
            // Disable create/edit actions
            Column {
                ReadOnlyModeBanner(
                    message = "Please connect to the internet to verify your subscription.",
                    onConnectClick = { /* open settings or retry */ }
                )
                MainAppContent(readOnly = true)
            }
        }

        SubscriptionAccessMode.LOCKED -> {
            // Show lock screen with sync option
            SubscriptionLockedScreen(
                message = "Your subscription needs verification. Please connect to the internet.",
                onRetryClick = { /* attempt sync */ }
            )
        }
    }
}
```

### 7.5 Subscription State Synchronization

#### Sync Strategy

```kotlin
class SubscriptionSyncManager(
    private val subscriptionRepository: SubscriptionRepository,
    private val deviceManager: DeviceAuthManager,
) {
    // Sync triggers
    enum class SyncTrigger {
        APP_LAUNCH,           // Every app start
        BACKGROUND_PERIODIC,  // Every 6 hours when online
        USER_INITIATED,       // Pull-to-refresh or manual
        PAYMENT_CALLBACK,     // After payment provider callback
        TOKEN_NEAR_EXPIRY,    // When device token < 24h remaining
    }

    suspend fun syncSubscriptionState(trigger: SyncTrigger): SyncResult {
        return try {
            // 1. Refresh device token
            val deviceResult = deviceManager.refreshDeviceToken()

            // 2. Get latest subscription state from server
            val subscription = subscriptionRepository.fetchFromServer()

            // 3. Update local cache
            subscriptionRepository.updateLocalCache(subscription)

            // 4. Return updated state
            SyncResult.Success(subscription)
        } catch (e: NetworkException) {
            // Offline - use cached state
            SyncResult.OfflineUsingCache(subscriptionRepository.getCached())
        } catch (e: Exception) {
            SyncResult.Error(e)
        }
    }
}
```

### 7.6 Complete Enforcement Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    SUBSCRIPTION ENFORCEMENT                     │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    CLIENT (Offline-First)                │   │
│  │                                                          │   │
│  │   SubscriptionState (cached locally)                     │   │
│  │   ├── plan: FREE/STARTER/PRO/ENTERPRISE                  │   │
│  │   ├── limits: { maxCustomers: 50, maxDevices: 2, ... }   │   │
│  │   ├── tokenExpiresAt: "2025-11-30T00:00:00Z"            │   │
│  │   └── accessMode: FULL_ACCESS / GRACE / READ_ONLY       │   │
│  │                                                          │   │
│  │   Enforcement Points:                                    │   │
│  │   ├── 1. App Launch → Check token expiry & access mode   │   │
│  │   ├── 2. Create/Edit → Check limits locally FIRST        │   │
│  │   ├── 3. Module Access → Check feature gate              │   │
│  │   └── 4. Sync → Refresh subscription state & token       │   │
│  │                                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼ (when online)                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    SERVER (Source of Truth)              │   │
│  │                                                          │   │
│  │   On Every Sync:                                         │   │
│  │   ├── 1. Validate device is registered & within limit    │   │
│  │   ├── 2. Return fresh subscription state                 │   │
│  │   ├── 3. Extend device token (7 days)                    │   │
│  │   ├── 4. Check for payment issues                        │   │
│  │   └── 5. Accept over-limit data, flag for follow-up      │   │
│  │                                                          │   │
│  │   Webhooks: Handle payment events, update subscription   │   │
│  │                                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### 7.7 Implementation Effort Estimate

| Component | Server | App (Common) | App (Platform-Specific) |
|-----------|--------|--------------|-------------------------|
| Device registration & token management | 2-3 days | 2 days | 1 day each |
| Subscription state caching | 1 day | 2 days | - |
| Access mode determination | 1 day | 2 days | - |
| Client-side limit enforcement | - | 3 days | - |
| Graceful degradation UI | - | 2 days | - |
| Sync manager | 2 days | 3 days | - |
| **Total** | **~7 days** | **~14 days** | **~3 days** |

### 7.8 Key Design Decisions

| Decision | Recommendation | Rationale |
|----------|----------------|-----------|
| Token expiry period | 7 days | Balance between security and UX |
| Grace period | 3 days | Allow short offline periods |
| Read-only mode trigger | 7 days after subscription end | Give user time to renew |
| Lock mode trigger | 14 days without sync | Prevent extended abuse |
| Over-limit sync handling | Accept data, flag workspace | No data loss, encourage upgrade |
| Client-side enforcement | Primary | Better UX, instant feedback |
| Server-side enforcement | Secondary | Prevent bypass, source of truth |

---

## 8. Backend Integration Requirements

### 8.1 API Endpoints (Spring Boot Backend)

```
# Subscription Management
GET    /api/v1/subscriptions/plans              # Get available plans
GET    /api/v1/subscriptions/current            # Get current subscription
POST   /api/v1/subscriptions/purchase           # Initiate purchase (Desktop)
POST   /api/v1/subscriptions/verify             # Verify mobile purchase
POST   /api/v1/subscriptions/cancel             # Cancel subscription
POST   /api/v1/subscriptions/change-plan        # Upgrade/Downgrade

# Add-ons
GET    /api/v1/subscriptions/addons             # Get available add-ons
POST   /api/v1/subscriptions/addons/purchase    # Purchase add-on
DELETE /api/v1/subscriptions/addons/{code}      # Cancel add-on

# Billing
GET    /api/v1/billing/invoices                 # Get billing history
GET    /api/v1/billing/invoices/{id}/pdf        # Download invoice PDF
GET    /api/v1/billing/payment-methods          # Get payment methods
POST   /api/v1/billing/payment-methods          # Add payment method
DELETE /api/v1/billing/payment-methods/{id}     # Remove payment method

# Webhooks
POST   /webhooks/google-play                    # Google Play RTDN
POST   /webhooks/app-store                      # App Store Server Notifications
POST   /webhooks/razorpay                       # Razorpay webhooks
POST   /webhooks/stripe                         # Stripe webhooks

# Usage Tracking
GET    /api/v1/usage/current                    # Get current usage
GET    /api/v1/usage/history                    # Get usage history
```

### 8.2 Webhook Event Handling

```kotlin
// Backend Webhook Event Types to Handle

// Google Play
- SUBSCRIPTION_PURCHASED
- SUBSCRIPTION_RENEWED
- SUBSCRIPTION_RECOVERED
- SUBSCRIPTION_CANCELED
- SUBSCRIPTION_ON_HOLD
- SUBSCRIPTION_PAUSED
- SUBSCRIPTION_EXPIRED

// App Store
- DID_CHANGE_RENEWAL_PREF
- DID_CHANGE_RENEWAL_STATUS
- DID_RENEW
- EXPIRED
- GRACE_PERIOD_EXPIRED
- SUBSCRIBED
- REFUND

// Razorpay
- subscription.charged
- subscription.activated
- subscription.pending
- subscription.halted
- subscription.cancelled
- subscription.completed

// Stripe
- customer.subscription.created
- customer.subscription.updated
- customer.subscription.deleted
- invoice.paid
- invoice.payment_failed
- customer.subscription.trial_will_end
```

---

## 9. Implementation Roadmap

### Phase 1: Foundation (Sprint 1-2)
- [ ] Define subscription domain models in commonMain
- [ ] Create SubscriptionRepository with offline-first pattern
- [ ] Implement feature gating system
- [ ] Add subscription-related fields to Workspace entity
- [ ] Create UI components (Upgrade banners, Plan comparison, etc.)

### Phase 2: Android Integration (Sprint 3-4)
- [ ] Integrate Google Play Billing Library v7
- [ ] Implement BillingClient lifecycle management
- [ ] Create subscription purchase flow UI
- [ ] Handle purchase verification with backend
- [ ] Implement subscription state synchronization

### Phase 3: iOS Integration (Sprint 5-6)
- [ ] Integrate StoreKit 2 via Kotlin/Native
- [ ] Create iOS-specific purchase flow
- [ ] Handle transaction listeners
- [ ] Implement App Store receipt validation
- [ ] Test in sandbox environment

### Phase 4: Desktop Integration (Sprint 7-8)
- [ ] Create checkout URL generation API
- [ ] Implement WebView/browser-based checkout
- [ ] Handle Razorpay integration (India)
- [ ] Handle Stripe integration (International)
- [ ] Implement webhook handling

### Phase 5: Backend Services (Sprint 9-10)
- [ ] Create SubscriptionService in Spring Boot
- [ ] Implement webhook handlers for all providers
- [ ] Build invoice generation system
- [ ] Create admin dashboard for subscription management
- [ ] Implement usage metering

### Phase 6: Testing & Launch (Sprint 11-12)
- [ ] End-to-end testing on all platforms
- [ ] Payment failure recovery testing
- [ ] Upgrade/downgrade flow testing
- [ ] Production environment setup
- [ ] Staged rollout

---

## 10. Revenue Optimization Strategies

### 10.1 Conversion Optimization
- **Free Trial**: 14-day trial for paid plans (no credit card required)
- **Onboarding Flow**: Guide users to key features during trial
- **Usage Notifications**: Alert users when approaching limits
- **Upgrade Prompts**: Context-aware upgrade suggestions

### 10.2 Retention Strategies
- **Annual Discount**: 20% off for annual commitment
- **Cancellation Flow**: Offer pause option before cancellation
- **Win-back Campaigns**: Re-engagement for churned users
- **Feature Announcements**: Highlight new features for upgrades

### 10.3 Expansion Revenue
- **Seat Expansion**: Easy addition of team members
- **Storage Upsells**: Clear storage upgrade path
- **Add-on Modules**: Targeted module recommendations
- **Multi-Workspace**: Encourage additional workspace creation

---

## 11. Compliance & Security

### 11.1 Payment Compliance
- **PCI DSS**: All card handling through payment providers (no card storage)
- **Strong Customer Authentication (SCA)**: 3D Secure for European cards
- **RBI Guidelines**: Tokenization for Indian cards via Razorpay

### 11.2 Platform Requirements
- **Google Play**: Must use Play Billing for digital goods
- **App Store**: 30% fee (15% for Small Business Program)
- **DMA Compliance**: Alternative payment in EU (iOS)

### 11.3 Data Privacy
- **GDPR**: Subscription data handling in compliance
- **Data Portability**: Export subscription and billing data
- **Retention**: Clear data retention policies per tier

---

## 12. Pricing Considerations

### 12.1 Indian Market (INR)
- Consider purchasing power parity
- UPI as primary payment method
- GST compliance (18% on software services)
- Annual pricing more popular

### 12.2 International Market (USD)
- Competitive with similar SaaS products
- Card payments primary
- Support for local payment methods via Stripe

### 12.3 Fee Considerations
| Provider | Fee |
|----------|-----|
| Google Play | 15-30% |
| App Store | 15-30% |
| Razorpay | 2% |
| Stripe | 2.9% + $0.30 |

---

## 13. Decision Points Requiring Input

### 13.1 Pricing Strategy
- [ ] Confirm tier pricing for Indian vs International markets
- [ ] Decide on add-on module pricing
- [ ] Determine trial duration per tier
- [ ] Set overage pricing model

### 13.2 Feature Allocation
- [ ] Finalize which modules are included in which tier
- [ ] Decide on API rate limits per tier
- [ ] Determine storage tiers

### 13.3 Technical Decisions
- [ ] Choose primary payment provider for desktop (Razorpay vs Stripe split)
- [ ] Decide on web dashboard for subscription management
- [ ] Determine grace period for failed payments

### 13.4 Business Decisions
- [ ] Refund policy
- [ ] Plan change proration
- [ ] Team/family plans consideration
- [ ] Reseller/partner program

---

## Appendix A: Platform SDK References

### Android
- [Google Play Billing Library](https://developer.android.com/google/play/billing)
- [Real-time Developer Notifications](https://developer.android.com/google/play/billing/rtdn-reference)

### iOS
- [StoreKit 2](https://developer.apple.com/documentation/storekit/in-app_purchase)
- [App Store Server Notifications V2](https://developer.apple.com/documentation/appstoreservernotifications)

### Payment Providers
- [Razorpay Subscriptions](https://razorpay.com/docs/payments/subscriptions/)
- [Stripe Billing](https://stripe.com/docs/billing)

---

## Appendix B: Existing Codebase Integration Points

### Files to Modify
- `Workspace.kt` - Add subscription fields
- `WorkspaceEntity.kt` - Add database columns
- `WorkspaceApiModel.kt` - Add API fields
- `WorkspaceContextManager.kt` - Add subscription state

### New Files to Create
```
composeApp/src/commonMain/kotlin/com/ampairs/subscription/
├── domain/
│   ├── SubscriptionPlan.kt
│   ├── SubscriptionState.kt
│   └── SubscriptionLimits.kt
├── api/
│   ├── SubscriptionApi.kt
│   └── SubscriptionApiImpl.kt
├── db/
│   ├── SubscriptionRepository.kt
│   └── SubscriptionEntity.kt
├── store/
│   └── SubscriptionStoreFactory.kt
├── ui/
│   ├── SubscriptionScreen.kt
│   ├── PlanComparisonScreen.kt
│   ├── BillingHistoryScreen.kt
│   └── components/
│       ├── UpgradeBanner.kt
│       ├── PlanCard.kt
│       └── FeatureGate.kt
└── viewmodel/
    └── SubscriptionViewModel.kt

composeApp/src/androidMain/kotlin/com/ampairs/subscription/
├── GooglePlayBillingManager.kt
└── SubscriptionManager.android.kt

composeApp/src/iosMain/kotlin/com/ampairs/subscription/
├── StoreKitManager.kt
└── SubscriptionManager.ios.kt

composeApp/src/desktopMain/kotlin/com/ampairs/subscription/
├── DesktopPaymentManager.kt
└── SubscriptionManager.desktop.kt
```

---

*Document Version: 1.0*
*Last Updated: November 2025*
*Author: Claude (AI Assistant)*
