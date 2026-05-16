# feature:subscription

Subscription and billing management. Handles plan selection, feature gating, usage limits, payment history, and device management tied to the active subscription.

## Responsibilities

- Display available subscription plans with feature comparison
- Gate UI features based on active plan tier
- Enforce usage limits (customers, products, invoices, users)
- Process payments and track payment history
- Manage subscription invoices and billing cycles
- Handle Google Play Billing on Android

## Key Classes

| Class | Purpose |
|---|---|
| `SubscriptionApi` / `SubscriptionApiImpl` | REST endpoints |
| `SubscriptionRepository` | Data access for plans, usage, history |
| `FeatureGate` | Check if a feature is enabled for the current plan |
| `LimitChecker` | Check if usage is within plan limits |
| `SubscriptionEnforcement` | Composable gate: show upgrade prompt if limit hit |
| `SubscriptionViewModel` | Plan list and current subscription state |
| `GooglePlayBillingManager` | Android in-app purchase integration |

## Domain Screens

`SubscriptionOnboardingScreen`, `PlanComparisonScreen`, `PaymentHistoryScreen`, `PaymentMethodsScreen`, `CheckoutScreen`, `UsageScreen`, `DevicesScreen`

## Koin Module

```kotlin
subscriptionModule          // in com.ampairs.subscription.di
subscriptionPlatformModule  // platform-specific (DB factory)
```

## Navigation Routes

```kotlin
SubscriptionRoute.Root
SubscriptionRoute.Plans
SubscriptionRoute.PlanDetails(planCode)
SubscriptionRoute.Checkout(planCode, billingCycle)
SubscriptionRoute.Usage
SubscriptionRoute.PaymentHistory
SubscriptionRoute.PaymentMethods
SubscriptionRoute.Devices
SubscriptionRoute.Invoices
SubscriptionRoute.InvoiceDetail(invoiceUid)
```

## Platform-Specific

| Platform | Feature |
|---|---|
| Android | Google Play Billing (`GooglePlayBillingManager`) |
| iOS / Desktop | Web-based checkout fallback |

## Database

`SubscriptionDatabase` — workspace-scoped (`factory` scope).
