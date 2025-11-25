# Subscription Backend API Requirements

This document outlines the backend API endpoints required for the subscription module to function completely. Some APIs are already implemented, while others need to be created.

## API Base URL
All endpoints are prefixed with: `/api/v1/subscription`

---

## 1. Plan APIs ✅ (Implemented)

### GET /plans
Returns all available subscription plans.

**Response:**
```json
[
  {
    "uid": "string",
    "plan_code": "FREE|STARTER|PROFESSIONAL|ENTERPRISE",
    "display_name": "string",
    "description": "string",
    "monthly_price_inr": 0.0,
    "monthly_price_usd": 0.0,
    "limits": {
      "max_workspaces": 1,
      "max_members_per_workspace": 1,
      "max_storage_gb": 1,
      "max_customers": 50,
      "max_products": 50,
      "max_invoices_per_month": 20,
      "max_devices": 2,
      "data_retention_years": 1
    },
    "features": {
      "available_modules": ["CUSTOMER", "PRODUCT", "INVOICE"],
      "api_access_enabled": false,
      "custom_branding_enabled": false,
      "sso_enabled": false,
      "audit_logs_enabled": false,
      "priority_support": false
    },
    "trial_days": 14,
    "google_play_product_id_monthly": "string",
    "google_play_product_id_annual": "string",
    "app_store_product_id_monthly": "string",
    "app_store_product_id_annual": "string",
    "display_order": 0
  }
]
```

### GET /plans/{planCode}
Returns a specific plan by code.

---

## 2. Subscription APIs ✅ (Implemented)

### GET /current
Returns current subscription for the workspace.

**Headers:**
- `X-Workspace-ID`: Workspace ID

**Response:**
```json
{
  "uid": "string",
  "workspace_id": "string",
  "plan_code": "string",
  "status": "ACTIVE|TRIALING|PAST_DUE|PAUSED|CANCELLED|EXPIRED",
  "billing_cycle": "MONTHLY|QUARTERLY|ANNUAL|BIENNIAL",
  "payment_provider": "RAZORPAY|STRIPE|GOOGLE_PLAY|APP_STORE",
  "currency": "INR",
  "current_period_start": "ISO8601",
  "current_period_end": "ISO8601",
  "trial_ends_at": "ISO8601",
  "cancel_at_period_end": false,
  "next_billing_amount": 0.0,
  "is_free": true,
  "days_remaining": 0
}
```

### POST /trial
Start a trial period.

**Request:**
```json
{
  "plan_code": "STARTER",
  "trial_days": 14
}
```

### POST /change-plan
Change subscription plan.

**Request:**
```json
{
  "new_plan_code": "PROFESSIONAL",
  "billing_cycle": "ANNUAL",
  "immediate": false
}
```

### POST /cancel
Cancel subscription.

**Request:**
```json
{
  "immediate": false,
  "reason": "string",
  "feedback": "string"
}
```

### POST /pause
Pause subscription.

**Request:**
```json
{
  "pause_days": 30,
  "reason": "string"
}
```

### POST /resume
Resume paused subscription.

---

## 3. Usage APIs ✅ (Implemented)

### GET /usage
Returns current usage metrics for the workspace.

**Response:**
```json
{
  "workspace_id": "string",
  "period_year": 2025,
  "period_month": 1,
  "usage": {
    "customer_count": 0,
    "product_count": 0,
    "invoice_count": 0,
    "order_count": 0,
    "member_count": 0,
    "device_count": 0,
    "storage_used_gb": 0.0,
    "api_calls": 0,
    "sms_count": 0,
    "email_count": 0
  },
  "limits": {
    "max_customers": 50,
    "max_products": 50,
    "max_invoices_per_month": 20,
    "max_members": 1,
    "max_devices": 2,
    "max_storage_gb": 1
  },
  "exceeded": {
    "customer_limit_exceeded": false,
    "product_limit_exceeded": false,
    "invoice_limit_exceeded": false,
    "storage_limit_exceeded": false,
    "member_limit_exceeded": false,
    "device_limit_exceeded": false,
    "has_any_exceeded": false
  },
  "last_calculated_at": "ISO8601"
}
```

### GET /limits/check
Check if a resource limit is exceeded.

**Query Parameters:**
- `resource_type`: CUSTOMER|PRODUCT|INVOICE|MEMBER|DEVICE|STORAGE_GB
- `current_count`: integer

**Response:**
```json
{
  "allowed": true,
  "limit": 50,
  "current": 10,
  "remaining": 40,
  "is_unlimited": false,
  "exceeded": false,
  "warning": false
}
```

### GET /features/check/{feature}
Check if a feature is available.

**Response:** `true` or `false`

---

## 4. Device APIs ✅ (Implemented)

### POST /devices/register
Register a new device.

**Request:**
```json
{
  "device_id": "string",
  "device_name": "string",
  "platform": "ANDROID|IOS|DESKTOP|WEB",
  "device_model": "string",
  "os_version": "string",
  "app_version": "string",
  "push_token": "string",
  "push_token_type": "FCM|APNS"
}
```

**Response:**
```json
{
  "uid": "string",
  "device_id": "string",
  "device_name": "string",
  "platform": "ANDROID",
  "device_model": "string",
  "os_version": "string",
  "app_version": "string",
  "token_expires_at": "ISO8601",
  "last_sync_at": "ISO8601",
  "last_activity_at": "ISO8601",
  "is_active": true,
  "access_mode": "FULL_ACCESS|OFFLINE_GRACE|READ_ONLY|LOCKED",
  "created_at": "ISO8601"
}
```

### POST /devices/refresh-token
Refresh device token.

**Request:**
```json
{
  "device_id": "string",
  "app_version": "string"
}
```

### GET /devices
Get all registered devices.

### GET /devices/{deviceId}/access-mode
Get device access mode.

### DELETE /devices/{deviceUid}
Deactivate a device.

**Query Parameters:**
- `reason`: optional string

---

## 5. Payment APIs ⚠️ (Partially Implemented)

### POST /purchase/initiate
Initiate a purchase (for desktop/web checkout).

**Request:**
```json
{
  "plan_code": "STARTER",
  "billing_cycle": "ANNUAL",
  "currency": "INR",
  "coupon_code": "string"
}
```

**Response:**
```json
{
  "checkout_url": "string",
  "checkout_session_id": "string",
  "provider": "RAZORPAY|STRIPE",
  "subscription_id": "string",
  "razorpay_order_id": "string",
  "razorpay_subscription_id": "string",
  "stripe_client_secret": "string",
  "amount": 0.0,
  "currency": "INR"
}
```

### POST /purchase/verify
Verify mobile in-app purchase.

**Request:**
```json
{
  "provider": "GOOGLE_PLAY|APP_STORE",
  "purchase_token": "string",
  "product_id": "string",
  "order_id": "string",
  "package_name": "string"
}
```

### GET /payments ❌ (NEEDS IMPLEMENTATION)
Get payment history with pagination.

**Query Parameters:**
- `page`: integer (default: 0)
- `size`: integer (default: 20)

**Response:**
```json
{
  "content": [
    {
      "uid": "string",
      "payment_provider": "RAZORPAY|STRIPE|GOOGLE_PLAY|APP_STORE",
      "status": "PENDING|PROCESSING|SUCCEEDED|FAILED|REFUNDED|CANCELLED",
      "amount": 0.0,
      "currency": "INR",
      "net_amount": 0.0,
      "payment_method_type": "CARD|UPI|NETBANKING|WALLET|IN_APP_PURCHASE",
      "payment_method_last4": "1234",
      "card_brand": "VISA",
      "description": "Subscription payment",
      "billing_period_start": "ISO8601",
      "billing_period_end": "ISO8601",
      "paid_at": "ISO8601",
      "failure_reason": "string",
      "receipt_url": "string",
      "invoice_pdf_url": "string",
      "created_at": "ISO8601"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

### GET /payment-methods ❌ (NEEDS IMPLEMENTATION)
Get saved payment methods.

**Response:**
```json
[
  {
    "uid": "string",
    "payment_provider": "RAZORPAY|STRIPE",
    "type": "CARD|UPI|NETBANKING",
    "last4": "1234",
    "brand": "VISA",
    "exp_month": 12,
    "exp_year": 2025,
    "cardholder_name": "string",
    "upi_id": "string",
    "bank_name": "string",
    "is_default": true,
    "is_expired": false,
    "display_name": "VISA •••• 1234",
    "created_at": "ISO8601"
  }
]
```

### PUT /payment-methods/{uid}/default ❌ (NEEDS IMPLEMENTATION)
Set default payment method.

### DELETE /payment-methods/{uid} ❌ (NEEDS IMPLEMENTATION)
Remove a payment method.

---

## 6. Sync APIs ✅ (Implemented)

### POST /sync
Full sync for offline-first apps.

**Request:**
```json
{
  "device_id": "string",
  "last_sync_at": "ISO8601"
}
```

**Response:**
```json
{
  "subscription": { /* SubscriptionState */ },
  "device": { /* DeviceRegistration */ },
  "usage": { /* UsageMetrics */ },
  "server_time": "ISO8601"
}
```

---

## Summary of Backend Work Required

### Already Implemented ✅
1. Plan APIs (GET /plans, GET /plans/{planCode})
2. Subscription APIs (GET /current, POST /trial, POST /change-plan, POST /cancel, POST /pause, POST /resume)
3. Usage APIs (GET /usage, GET /limits/check, GET /features/check)
4. Device APIs (all endpoints)
5. Purchase initiation and verification
6. Sync APIs

### Needs Implementation ❌
1. **GET /payments** - Payment history with pagination
2. **GET /payment-methods** - Saved payment methods list
3. **PUT /payment-methods/{uid}/default** - Set default payment method
4. **DELETE /payment-methods/{uid}** - Remove payment method

### Notes
- All endpoints require JWT authentication
- Workspace context is provided via `X-Workspace-ID` header
- All timestamps should be in ISO 8601 format
- Currency amounts are in the smallest unit or as decimal (based on provider)
