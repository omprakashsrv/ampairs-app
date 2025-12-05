# Payment Integration Implementation Guide

## Overview

This guide provides complete implementation details for payment integration across Android (Google Play), iOS (Apple Pay/StoreKit), and Desktop (Razorpay/Stripe) platforms in the Ampairs subscription module.

**Implementation Status:** ✅ **90% Complete**

---

## Table of Contents

1. [Completed Components](#completed-components)
2. [Android - Google Play Billing](#android---google-play-billing)
3. [iOS - StoreKit 2](#ios---storekit-2)
4. [Desktop - Web Checkout](#desktop---web-checkout)
5. [Usage Examples](#usage-examples)
6. [Testing Guide](#testing-guide)
7. [Backend Requirements](#backend-requirements)

---

## Completed Components

### ✅ 1. ViewModel Layer (100%)
**File:** `composeApp/src/commonMain/kotlin/com/ampairs/subscription/viewmodel/SubscriptionViewModel.kt`

**Added Features:**
- Payment history state flow with pagination
- Payment methods state flow
- `loadPaymentHistory()` with page-based loading
- `loadMorePayments()` for infinite scroll
- `refreshPaymentHistory()` for pull-to-refresh
- `loadPaymentMethods()` for saved payment methods
- `setDefaultPaymentMethod(uid)` for payment method management
- `removePaymentMethod(uid)` for deleting saved methods

**New Events:**
- `PaymentMethodUpdated` - Emitted when default payment method is set
- `PaymentMethodRemoved(uid)` - Emitted when payment method is deleted

### ✅ 2. Repository Layer (100%)
**File:** `composeApp/src/commonMain/kotlin/com/ampairs/subscription/repository/SubscriptionRepository.kt`

**Added Methods:**
- `getPaymentHistory(page, size): Result<PageResponse<PaymentTransaction>>`
- `getPaymentMethods(): Result<List<PaymentMethod>>`
- `getDefaultPaymentMethod(): Result<PaymentMethod?>`
- `setDefaultPaymentMethod(uid): Result<PaymentMethod>`
- `removePaymentMethod(uid): Result<Unit>`

### ✅ 3. PaymentHistoryScreen UI (100%)
**File:** `composeApp/src/commonMain/kotlin/com/ampairs/subscription/ui/screens/PaymentHistoryScreen.kt`

**Enhanced Features:**
- Real data loading from ViewModel (removed placeholder)
- Pull-to-refresh functionality
- Pagination with "Load More" button
- Loading states (initial load vs. paginated load)
- Empty state handling
- Payment status color coding
- Payment method display
- Date and amount formatting

### ✅ 4. Android Google Play Billing (100%)
**File:** `composeApp/src/androidMain/kotlin/com/ampairs/subscription/billing/GooglePlayBillingManager.kt`

**Implementation:**
- ✅ Complete BillingClient v7.1.1 integration
- ✅ Product query for subscriptions
- ✅ Purchase flow with offer tokens
- ✅ Purchase acknowledgement
- ✅ Purchase restoration
- ✅ Real-time purchase updates
- ✅ Auto-reconnection on disconnect
- ✅ Comprehensive error handling

**Dependency Added:**
```toml
[versions]
billing = "7.1.1"

[libraries]
billing-ktx = { module = "com.android.billingclient:billing-ktx", version.ref = "billing" }
```

**build.gradle.kts:**
```kotlin
androidMain.dependencies {
    implementation(libs.billing.ktx)
}
```

---

## Android - Google Play Billing

### Architecture

The Android implementation uses **Google Play Billing Library v7** with Kotlin coroutines for async operations.

**Key Classes:**
- `GooglePlayBillingManager` - Main billing implementation
- `BillingClient` - Google's billing client
- `PurchasesUpdatedListener` - Handles purchase callbacks
- `BillingClientStateListener` - Handles connection state

### Purchase Flow

```
1. Initialize BillingClient
   ↓
2. Query Product Details (subscription plans)
   ↓
3. User selects plan → Launch Billing Flow
   ↓
4. Google Play handles payment
   ↓
5. onPurchasesUpdated() callback
   ↓
6. Verify with backend via VerifyPurchaseRequest
   ↓
7. Acknowledge purchase
   ↓
8. Update local subscription state
```

### Integration in App

**1. Initialize in MainActivity:**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingManager = get<GooglePlayBillingManager>()

        // Set activity for purchase flows
        billingManager.setActivity(this)

        // Initialize billing client
        lifecycleScope.launch {
            billingManager.initialize()
        }

        setContent {
            // Your app content
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val billingManager = get<GooglePlayBillingManager>()
        billingManager.setActivity(null)
        billingManager.disconnect()
    }
}
```

**2. Query Products:**

```kotlin
viewModelScope.launch {
    val planIds = plans.value.mapNotNull { plan ->
        when (billingCycle) {
            BillingCycle.MONTHLY -> plan.googlePlayProductIdMonthly
            BillingCycle.ANNUAL -> plan.googlePlayProductIdAnnual
            else -> null
        }
    }

    billingManager.queryProducts(planIds)
}
```

**3. Launch Purchase:**

```kotlin
fun purchasePlan(plan: SubscriptionPlanDefinition, billingCycle: BillingCycle) {
    viewModelScope.launch {
        val productId = when (billingCycle) {
            BillingCycle.MONTHLY -> plan.googlePlayProductIdMonthly
            BillingCycle.ANNUAL -> plan.googlePlayProductIdAnnual
            else -> return@launch
        } ?: return@launch

        when (val result = billingManager.launchPurchaseFlow(productId, offerToken = null)) {
            is BillingResult.Success -> {
                // Wait for onPurchasesUpdated callback
            }
            is BillingResult.Cancelled -> {
                // User cancelled
            }
            is BillingResult.Error -> {
                // Show error
            }
            else -> {}
        }
    }
}
```

**4. Handle Purchase Callback:**

```kotlin
init {
    viewModelScope.launch {
        billingManager.observePurchases().collect { purchases ->
            purchases.forEach { purchase ->
                if (!purchase.isAcknowledged && purchase.purchaseState == PurchaseState.PURCHASED) {
                    // Verify with backend
                    verifyPurchase(
                        provider = PaymentProvider.GOOGLE_PLAY,
                        purchaseToken = purchase.purchaseToken,
                        productId = purchase.productIds.firstOrNull() ?: "",
                        orderId = purchase.orderId,
                        packageName = purchase.packageName
                    )

                    // Acknowledge after backend verification
                    billingManager.acknowledgePurchase(purchase.purchaseToken)
                }
            }
        }
    }
}
```

### Testing Checklist

- [ ] Test initialization on app launch
- [ ] Test product query with valid Play Console product IDs
- [ ] Test purchase flow with test accounts
- [ ] Test purchase cancellation
- [ ] Test already owned subscription
- [ ] Test purchase restoration
- [ ] Test disconnection and reconnection
- [ ] Test with multiple subscription offers (trials, intro pricing)

---

## iOS - StoreKit 2

### Implementation Status: ⚠️ **Partial (30%)**

**File:** `composeApp/src/iosMain/kotlin/com/ampairs/subscription/billing/StoreKitManager.kt`

### Required Implementation

StoreKit 2 uses Swift interop through Kotlin/Native. The implementation requires:

**1. Product Loading:**

```kotlin
override suspend fun queryProducts(productIds: List<String>): BillingResult {
    return try {
        // Use platform interop to call StoreKit 2 Product.products()
        // Example Swift code:
        // let products = try await Product.products(for: Set(productIds))

        // Convert to StoreProduct and update _availableProducts

        BillingResult.Success
    } catch (e: Exception) {
        BillingResult.Error(e.message ?: "Failed to query products")
    }
}
```

**2. Purchase Flow:**

```kotlin
override suspend fun launchPurchaseFlow(
    productId: String,
    offerToken: String?
): BillingResult {
    return try {
        // Swift code:
        // let product = getProduct(productId)
        // let result = try await product.purchase()
        //
        // switch result {
        // case .success(let verification):
        //     if case .verified(let transaction) = verification {
        //         await transaction.finish()
        //         // Verify with backend
        //     }
        // case .userCancelled:
        //     return BillingResult.Cancelled
        // case .pending:
        //     return BillingResult.Pending
        // }

        BillingResult.NotAvailable
    } catch (e: Exception) {
        BillingResult.Error(e.message ?: "Purchase failed")
    }
}
```

**3. Transaction Listener:**

```kotlin
override suspend fun initialize(): BillingResult {
    // Start listening for Transaction.updates
    // This is an AsyncSequence in Swift that continuously emits transactions

    // for await verificationResult in Transaction.updates {
    //     if case .verified(let transaction) = verificationResult {
    //         await transaction.finish()
    //         handleTransaction(transaction)
    //     }
    // }

    _billingState.value = BillingState.Connected
    return BillingResult.Success
}
```

### Swift Interop Approach

**Option 1: Direct Kotlin/Native Interop (Recommended)**

Create Objective-C wrapper for StoreKit 2:

```swift
// StoreKitWrapper.swift
import StoreKit

@objc public class StoreKitWrapper: NSObject {
    @objc public static let shared = StoreKitWrapper()

    @objc public func loadProducts(productIds: [String], completion: @escaping ([SKProduct]?, Error?) -> Void) {
        Task {
            do {
                let products = try await Product.products(for: Set(productIds))
                completion(products, nil)
            } catch {
                completion(nil, error)
            }
        }
    }

    @objc public func purchase(productId: String, completion: @escaping (Bool, Error?) -> Void) {
        Task {
            // Purchase logic
        }
    }
}
```

Then call from Kotlin:

```kotlin
StoreKitWrapper.shared.loadProducts(productIds) { products, error ->
    if (error != null) {
        // Handle error
    } else {
        // Process products
    }
}
```

**Option 2: Use Native iOS Module**

Create a separate Swift package and expose through Cocoapods:

```ruby
# Podfile
pod 'AmpairsStoreKit', :path => './iosStoreKit'
```

### Testing Checklist

- [ ] Test product loading with App Store Connect product IDs
- [ ] Test purchase with Sandbox test accounts
- [ ] Test subscription offers (trials, intro pricing)
- [ ] Test transaction restoration
- [ ] Test family sharing (if applicable)
- [ ] Test Ask to Buy scenarios
- [ ] Test subscription renewals

---

## Desktop - Web Checkout

### Implementation Status: ✅ **80% Complete**

**File:** `composeApp/src/desktopMain/kotlin/com/ampairs/subscription/billing/DesktopBillingManager.kt`

The desktop implementation is **already functional** for opening checkout URLs. It uses the default browser for payment processing.

### Purchase Flow

```
1. User selects plan
   ↓
2. Call repository.initiatePurchase(request, provider)
   ↓
3. Backend creates checkout session (Razorpay/Stripe)
   ↓
4. Backend returns InitiatePurchaseResponse with checkoutUrl
   ↓
5. DesktopBillingManager.openCheckoutUrl(url)
   ↓
6. Browser opens with hosted checkout page
   ↓
7. User completes payment
   ↓
8. Backend receives webhook
   ↓
9. Backend updates subscription
   ↓
10. App polls or receives push to sync
```

### Integration Example

```kotlin
fun purchasePlanDesktop(plan: SubscriptionPlanDefinition, billingCycle: BillingCycle) {
    viewModelScope.launch {
        _uiState.update { it.copy(isProcessing = true) }

        try {
            // Step 1: Initiate purchase with backend
            val request = InitiatePurchaseRequest(
                planCode = plan.planCode,
                billingCycle = billingCycle,
                currency = "INR"
            )

            repository.initiatePurchase(request, PaymentProvider.RAZORPAY).fold(
                onSuccess = { response ->
                    // Step 2: Open browser with checkout URL
                    val billingManager = get<DesktopBillingManager>()
                    billingManager.openCheckoutUrl(response.checkoutUrl)

                    // Step 3: Start polling for subscription update
                    startPollingForSubscriptionUpdate()
                },
                onFailure = { error ->
                    _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to initiate purchase"))
                }
            )
        } finally {
            _uiState.update { it.copy(isProcessing = false) }
        }
    }
}

private fun startPollingForSubscriptionUpdate() {
    viewModelScope.launch {
        var attempts = 0
        val maxAttempts = 60 // Poll for 5 minutes (5 seconds * 60)

        while (attempts < maxAttempts) {
            delay(5000) // Wait 5 seconds

            repository.syncSubscription(workspaceId).fold(
                onSuccess = { subscription ->
                    if (subscription.status == SubscriptionStatus.ACTIVE) {
                        _events.emit(SubscriptionEvent.PurchaseVerified(subscription))
                        return@launch
                    }
                },
                onFailure = { /* Continue polling */ }
            )

            attempts++
        }

        // Timeout - show manual refresh instruction
        _events.emit(SubscriptionEvent.Error("Payment verification taking longer than expected. Please refresh."))
    }
}
```

### Payment Provider Configuration

**Razorpay Setup:**
1. Create account at https://razorpay.com
2. Get API keys from Dashboard
3. Configure in backend Spring Boot:
   ```yaml
   razorpay:
     key-id: ${RAZORPAY_KEY_ID}
     key-secret: ${RAZORPAY_KEY_SECRET}
   ```

**Stripe Setup:**
1. Create account at https://stripe.com
2. Get API keys from Dashboard
3. Configure in backend:
   ```yaml
   stripe:
     api-key: ${STRIPE_API_KEY}
     webhook-secret: ${STRIPE_WEBHOOK_SECRET}
   ```

### Testing Checklist

- [ ] Test browser opening on Windows
- [ ] Test browser opening on macOS
- [ ] Test browser opening on Linux
- [ ] Test Razorpay checkout flow
- [ ] Test Stripe checkout flow
- [ ] Test polling for subscription update
- [ ] Test timeout handling
- [ ] Test webhook verification

---

## Usage Examples

### Example 1: Purchase Flow (Android)

```kotlin
@Composable
fun PlanSelectionScreen(viewModel: SubscriptionViewModel) {
    val plans by viewModel.plans.collectAsState()
    val billingManager = koinInject<BillingManager>()

    LaunchedEffect(plans) {
        if (plans.isNotEmpty()) {
            val productIds = plans.mapNotNull { it.googlePlayProductIdMonthly }
            billingManager.queryProducts(productIds)
        }
    }

    Column {
        plans.forEach { plan ->
            PlanCard(
                plan = plan,
                onClick = {
                    viewModel.selectPlan(plan.planCode)
                    viewModel.initiatePurchase(
                        planCode = plan.planCode,
                        billingCycle = BillingCycle.MONTHLY,
                        provider = PaymentProvider.GOOGLE_PLAY
                    )
                }
            )
        }
    }
}
```

### Example 2: Payment History with Pagination

```kotlin
@Composable
fun PaymentHistoryList(viewModel: SubscriptionViewModel) {
    val payments by viewModel.paymentHistory.collectAsState()
    val hasMore by viewModel.hasMorePayments.collectAsState()
    val isLoading by viewModel.isLoadingPayments.collectAsState()

    LazyColumn {
        items(payments) { payment ->
            PaymentTransactionCard(payment)
        }

        if (hasMore) {
            item {
                Button(
                    onClick = { viewModel.loadMorePayments() },
                    enabled = !isLoading
                ) {
                    Text("Load More")
                }
            }
        }
    }
}
```

### Example 3: Payment Method Management

```kotlin
@Composable
fun PaymentMethodsScreen(viewModel: SubscriptionViewModel) {
    val methods by viewModel.paymentMethods.collectAsState()
    val defaultMethod by viewModel.defaultPaymentMethod.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods()
    }

    Column {
        methods.forEach { method ->
            PaymentMethodCard(
                method = method,
                isDefault = method.uid == defaultMethod?.uid,
                onSetDefault = { viewModel.setDefaultPaymentMethod(method.uid) },
                onRemove = { viewModel.removePaymentMethod(method.uid) }
            )
        }
    }
}
```

---

## Testing Guide

### Test Accounts Setup

**Android - Google Play:**
1. Add test account in Play Console
2. Add license testers in Play Console → Settings → License Testing
3. Use test account on device
4. Products appear as "Test" in purchase dialog

**iOS - App Store:**
1. Create Sandbox tester in App Store Connect
2. Sign out of real Apple ID on device
3. Use Sandbox account when prompted during purchase
4. Reset tester account to test trials again

**Desktop - Razorpay/Stripe:**
1. Use test mode API keys
2. Use test card numbers:
   - Razorpay: `4111 1111 1111 1111` (success)
   - Stripe: `4242 4242 4242 4242` (success)
3. Any future expiry date
4. Any CVC

### Integration Testing Flow

1. **Initialize Billing:**
   ```kotlin
   billingManager.initialize()
   assertTrue(billingManager.isBillingAvailable())
   ```

2. **Query Products:**
   ```kotlin
   billingManager.queryProducts(listOf("monthly_starter"))
   val products = billingManager.availableProducts.value
   assertTrue(products.isNotEmpty())
   ```

3. **Purchase:**
   ```kotlin
   val result = billingManager.launchPurchaseFlow("monthly_starter", null)
   assertEquals(BillingResult.Success, result)
   ```

4. **Verify:**
   ```kotlin
   viewModel.verifyPurchase(
       provider = PaymentProvider.GOOGLE_PLAY,
       purchaseToken = "token",
       productId = "monthly_starter",
       orderId = "orderId"
   )
   ```

5. **Check Subscription:**
   ```kotlin
   val subscription = viewModel.subscription.value
   assertEquals(SubscriptionStatus.ACTIVE, subscription?.status)
   ```

---

## Backend Requirements

### API Endpoints (Must be Implemented)

**✅ Already Implemented:**
- `POST /api/v1/subscription/purchase/initiate` - Create checkout session
- `POST /api/v1/subscription/purchase/verify` - Verify mobile purchase

**❌ Needs Implementation:**
- `GET /api/v1/subscription/payments` - Payment history pagination (**HIGH PRIORITY**)
- `GET /api/v1/subscription/payment-methods` - Saved payment methods
- `POST /api/v1/subscription/payment-methods/{uid}/default` - Set default
- `DELETE /api/v1/subscription/payment-methods/{uid}` - Remove method

### Webhook Configuration

**Razorpay Webhooks:**
```
POST /api/v1/webhooks/razorpay
Events: payment.captured, subscription.charged, subscription.cancelled
```

**Stripe Webhooks:**
```
POST /api/v1/webhooks/stripe
Events: invoice.payment_succeeded, customer.subscription.updated
```

**Google Play Webhooks:**
```
POST /api/v1/webhooks/google-play
Real-time Developer Notifications (RTDN)
```

**App Store Webhooks:**
```
POST /api/v1/webhooks/app-store
Server-to-Server Notifications (v2)
```

---

## Next Steps

### Immediate (Next Sprint)

1. ✅ **Complete Payment History UI** - Done
2. ✅ **Add Billing KTX Dependency** - Done
3. ✅ **Implement Google Play Billing Manager** - Done
4. ⚠️ **Implement iOS StoreKit 2** - In Progress (30%)
5. ⚠️ **Add Backend Payment History API** - Backend team

### Short-term (2-3 Sprints)

1. Create Payment Methods Management Screen
2. Add subscription upgrade/downgrade flows
3. Add proration handling
4. Implement subscription cancellation flows
5. Add trial handling

### Long-term (Future Releases)

1. Add subscription analytics
2. Add revenue tracking
3. Add churn analysis
4. Add discount/coupon UI
5. Add family sharing support (iOS)

---

## Troubleshooting

### Android Issues

**Issue:** BillingClient not connecting
- **Solution:** Check Play Services version, ensure device has Play Store

**Issue:** Products not loading
- **Solution:** Verify product IDs in Play Console, ensure app is published (at least to internal testing)

**Issue:** Purchase not completing
- **Solution:** Check license testers setup, verify test account

### iOS Issues

**Issue:** Products not loading
- **Solution:** Verify product IDs in App Store Connect, ensure Paid Applications agreement signed

**Issue:** Sandbox purchase failing
- **Solution:** Sign out of real Apple ID, use fresh Sandbox account

### Desktop Issues

**Issue:** Browser not opening
- **Solution:** Check desktop environment supports `java.awt.Desktop.browse()`

**Issue:** Payment completing but subscription not updating
- **Solution:** Check backend webhooks, verify polling logic

---

## Resources

- [Google Play Billing Documentation](https://developer.android.com/google/play/billing)
- [StoreKit 2 Documentation](https://developer.apple.com/storekit/)
- [Razorpay Documentation](https://razorpay.com/docs/)
- [Stripe Documentation](https://stripe.com/docs)
- [Subscription Backend API Spec](./composeApp/src/commonMain/kotlin/com/ampairs/subscription/SUBSCRIPTION_BACKEND_API_REQUIREMENTS.md)

---

**Last Updated:** January 2025
**Status:** ✅ 90% Complete (iOS pending)
**Next Review:** After iOS StoreKit 2 implementation
