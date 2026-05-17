# Google Play Payment Testing Guide

**App ID:** `com.ampairs.app`
**Last Updated:** December 2, 2025

---

## 🎯 Quick Answer: When Can You Test?

**You can test RIGHT NOW** using Google Play's sandbox environment - **NO REAL MONEY required!**

---

## 📋 Testing Environment Overview

### Test Tracks Available

| Track | When Available | Real Payments | Best For |
|-------|----------------|---------------|----------|
| **Internal Testing** | Immediately after upload | ❌ Test cards only | Development team testing |
| **Closed Testing** | After app review (~1-2 days) | ❌ Test cards only | Beta testers |
| **Open Testing** | After app review (~1-2 days) | ❌ Test cards only | Public beta |
| **Production** | After full review (~3-7 days) | ✅ Real payments | Live users |

**Recommendation:** Start with **Internal Testing** - you can test the complete payment flow TODAY.

---

## 🚀 Step-by-Step Testing Setup

### Step 1: Configure In-App Products in Google Play Console

1. **Go to Google Play Console**
   - Navigate to: https://play.google.com/console
   - Select your app: `Ampairs App (com.ampairs.app)`

2. **Create Subscription Products**
   - Go to: **Monetization** → **Products** → **Subscriptions**
   - Click **Create subscription**

3. **Product IDs to Create** (match your backend plan codes):
   ```
   Product ID: ampairs_free
   - Name: Free Plan
   - Description: 50 Customers, 50 Products, 20 Invoices/month
   - Price: Free (base plan)

   Product ID: ampairs_professional_monthly
   - Name: Professional Plan (Monthly)
   - Description: Unlimited customers, products, invoices
   - Base plan: Monthly
   - Price: ₹999/month (or your pricing)

   Product ID: ampairs_professional_yearly
   - Name: Professional Plan (Yearly)
   - Description: Unlimited customers, products, invoices
   - Base plan: Yearly
   - Price: ₹9,999/year (or your pricing)
   - Free trial: 14 days

   Product ID: ampairs_premium_monthly
   - Name: Premium Plan (Monthly)
   - Description: All features + priority support
   - Base plan: Monthly
   - Price: ₹1,999/month (or your pricing)

   Product ID: ampairs_premium_yearly
   - Name: Premium Plan (Yearly)
   - Description: All features + priority support
   - Base plan: Yearly
   - Price: ₹19,999/year (or your pricing)
   - Free trial: 14 days
   ```

4. **Activate Products**
   - Set status to **Active** for all products
   - Save changes

### Step 2: Add License Test Users

1. **Navigate to License Testing**
   - Go to: **Setup** → **License testing**

2. **Add Test Accounts** (Gmail accounts only):
   ```
   yourtestemail1@gmail.com
   yourtestemail2@gmail.com
   developer@yourcompany.com
   ```

3. **Configure Test Response**
   - Select: **License Test Response** = "RESPOND_NORMALLY"
   - This allows test purchases without charges

4. **Save Changes**

### Step 3: Build and Upload Signed APK/AAB

1. **Generate Signed Bundle**:
   ```bash
   cd /Users/omprakashsrv/StudioProjects/ampairs-app
   ./gradlew composeApp:bundleRelease
   ```

2. **Upload to Internal Testing Track**:
   - Go to: **Testing** → **Internal testing**
   - Click **Create new release**
   - Upload: `composeApp/build/outputs/bundle/release/composeApp-release.aab`
   - Add release notes: "Payment flow testing"
   - Click **Review release** → **Start rollout**

### Step 4: Add Testers to Internal Testing

1. **Create Tester List**:
   - In Internal testing, go to **Testers** tab
   - Click **Create email list**
   - Name: "Payment Testers"
   - Add the same email addresses from License Testing

2. **Get Opt-In Link**:
   - Copy the opt-in URL shown on screen
   - Example: `https://play.google.com/apps/internaltest/...`

### Step 5: Install and Test on Device

1. **On Your Test Device**:
   - Open the opt-in link in a browser
   - Sign in with your test account (must be in tester list)
   - Click "Become a tester"
   - Download and install the app from Play Store

2. **Verify Installation**:
   - Open the app
   - Check version number matches your upload
   - Sign in to your workspace

---

## 🧪 Testing the Payment Flow

### Complete Test Scenario

**Starting Point:** App installed from Internal Testing track, signed in with test user

#### Test 1: Query Products
```
1. Navigate to: Subscription Plans screen
2. Expected: See all subscription plans with prices
3. Verify: Prices match what you set in Play Console
4. Logs: Check for "Products loaded: 4" or similar
```

#### Test 2: Initiate Purchase (Test Card)
```
1. Select: Professional Monthly Plan
2. Tap: "Start 14-Day Trial" or "Subscribe"
3. Expected: Google Play payment sheet appears
4. IMPORTANT: You'll see "Test card, no charge will be made"
5. Complete: Tap "Subscribe" to complete test purchase
```

#### Test 3: Verify Purchase
```
1. Expected: Purchase success callback triggered
2. Backend: Webhook receives test purchase notification
3. App: Subscription status updates to TRIAL or ACTIVE
4. UI: Redirects to workspace modules screen
5. Logs: "Purchase successful, verifying with backend..."
```

#### Test 4: Check Subscription Status
```
1. Navigate to: Subscription screen
2. Expected: Shows active subscription
3. Details: Plan name, billing cycle, next renewal date
4. Trial: If free trial, shows "Trial ends on [date]"
```

#### Test 5: Query Purchases (Restore)
```
1. Uninstall the app
2. Reinstall from Internal Testing track
3. Sign in with same test account
4. Navigate to: Subscription screen
5. Expected: Subscription restored automatically
6. Verify: Same active subscription appears
```

#### Test 6: Cancel Subscription
```
1. Navigate to: Subscription screen
2. Tap: "Manage Subscription" (opens Play Store)
3. In Play Store: Tap "Cancel subscription"
4. Confirm cancellation
5. Back in app: Verify status shows "Cancels on [date]"
```

---

## 🎮 Test Account Behavior

### What Test Users Experience

**License Test Users:**
- See "Test card - no charge" on payment sheet
- Can complete purchases without real payment methods
- Purchases behave like real subscriptions (trial, renewal, cancel)
- **NO REAL MONEY CHARGED**

**Regular Users (Non-Test):**
- If they install from Internal Testing track: Also get test behavior
- If they install from Production track: Real payments required

### Test Purchase Lifecycle

```
Test Purchase Flow:
1. User selects plan → Google Play payment sheet
2. "Test card, no charge" message displayed
3. User confirms → Purchase token generated
4. App receives purchase → Sends to backend
5. Backend webhook receives test purchase notification
6. Backend verifies with Google Play API (test response)
7. Subscription activated in backend
8. App syncs subscription status
9. User gains access to features
```

**Trial Period:**
- Test accounts can activate 14-day trials
- Trial behaves like real trial (grace period, expiration)
- No payment method required for test trials

**Renewal:**
- Test subscriptions auto-renew on schedule
- Backend receives renewal notifications via webhook
- Can test renewal, cancellation, expiration flows

---

## 🔍 Debugging During Testing

### Check Android Logs

```bash
# Filter billing-related logs
adb logcat | grep -i "billing\|purchase\|subscription"

# Check app logs
adb logcat | grep "com.ampairs.app"
```

### Key Log Messages to Watch

**Successful Connection:**
```
Billing setup successful
Connected to Google Play Billing
```

**Product Query:**
```
Querying products: [ampairs_professional_monthly, ampairs_professional_yearly, ...]
Products loaded: 4
```

**Purchase Flow:**
```
Launching purchase flow for: ampairs_professional_monthly
Purchase successful: [purchase token]
Verifying purchase with backend...
Subscription activated
```

**Errors to Watch For:**
```
❌ Billing setup failed: BILLING_UNAVAILABLE
   → Device doesn't have Play Store or not signed in

❌ Product not found: ampairs_professional_monthly
   → Product ID not created in Play Console or not activated

❌ Purchase failed: ITEM_ALREADY_OWNED
   → Test account already owns this subscription (cancel first)

❌ Purchase failed: DEVELOPER_ERROR
   → APK signature doesn't match Play Console (use signed release build)
```

---

## 🐛 Common Issues & Solutions

### Issue 1: "Billing setup failed: BILLING_UNAVAILABLE"

**Cause:** Device not connected to Play Store or wrong account

**Solution:**
1. Ensure device has Google Play Store installed
2. Sign in to Play Store with test account
3. Verify account is in License Testing list
4. Try: Clear Play Store cache/data

### Issue 2: "Product not found"

**Cause:** Product not created or not active in Play Console

**Solution:**
1. Go to Play Console → Monetization → Subscriptions
2. Verify product ID matches exactly (case-sensitive)
3. Ensure product status is "Active"
4. Wait 5-10 minutes for changes to propagate

### Issue 3: "This version of the application is not configured for billing"

**Cause:** Using debug build or wrong signing certificate

**Solution:**
1. Use release build signed with upload key
2. Verify signing certificate fingerprint matches Play Console
3. Upload to Internal Testing track (debuggable builds don't work)

### Issue 4: "Item already owned"

**Cause:** Test account already has active subscription

**Solution:**
1. Open Play Store → Subscriptions
2. Cancel existing test subscription
3. Wait 5 minutes for cancellation to process
4. Try purchase again

### Issue 5: Backend webhook not receiving notifications

**Cause:** Webhook URL not configured or not accessible

**Solution:**
1. Go to Play Console → Monetization → Monetization setup
2. Configure Real-time developer notifications (Cloud Pub/Sub)
3. Verify your backend endpoint is publicly accessible
4. Check backend logs for incoming webhook calls

---

## 📊 Test Checklist

Before marking payment integration complete, verify:

- [ ] **Products Query**
  - [ ] All subscription products load correctly
  - [ ] Prices display in correct currency
  - [ ] Free trial information shows correctly

- [ ] **Purchase Flow**
  - [ ] Payment sheet appears on button tap
  - [ ] "Test card, no charge" message visible
  - [ ] Purchase completes successfully
  - [ ] Success callback triggered

- [ ] **Backend Integration**
  - [ ] Purchase token sent to backend
  - [ ] Backend webhook receives notification
  - [ ] Backend verifies with Google Play API
  - [ ] Subscription status updates in database

- [ ] **UI Updates**
  - [ ] Subscription screen shows active plan
  - [ ] Usage limits update based on plan
  - [ ] "Upgrade" button hidden for paid plans
  - [ ] Next billing date displays correctly

- [ ] **Subscription Management**
  - [ ] "Manage Subscription" button opens Play Store
  - [ ] Can cancel subscription from Play Store
  - [ ] App detects cancellation and updates UI
  - [ ] Access continues until end of billing period

- [ ] **Purchase Restoration**
  - [ ] Uninstall/reinstall preserves subscription
  - [ ] Signing in with same account restores purchases
  - [ ] Multiple devices sync subscription status

- [ ] **Edge Cases**
  - [ ] Handles network errors gracefully
  - [ ] Retries failed purchase verification
  - [ ] Shows appropriate error messages
  - [ ] Doesn't grant access without valid purchase

---

## 🚀 Moving to Production

### When You're Ready for Real Payments

1. **Complete Internal Testing** (✅ checklist above)

2. **Configure Production Webhook**:
   ```
   - Go to Play Console → Monetization → Monetization setup
   - Enable Real-time developer notifications
   - Configure Cloud Pub/Sub topic
   - Point to your production backend webhook
   ```

3. **Submit to Production Track**:
   ```
   - Go to Testing → Production
   - Create new release
   - Upload signed AAB
   - Complete app content questionnaire
   - Submit for review (3-7 days)
   ```

4. **After Approval**:
   - Regular users see real payment methods
   - Real money charged on purchase
   - Webhook receives production notifications
   - Backend processes real subscriptions

5. **Monitor**:
   - Play Console → Financial reports
   - Backend subscription metrics
   - User subscription status
   - Webhook delivery success rate

---

## 📞 Support Resources

**Google Play Billing Documentation:**
- https://developer.android.com/google/play/billing

**Billing Library v7 Migration:**
- https://developer.android.com/google/play/billing/migrate-gpblv7

**Testing Purchases:**
- https://developer.android.com/google/play/billing/test

**Webhook Configuration:**
- https://developer.android.com/google/play/billing/rtdn

**Play Console:**
- https://play.google.com/console

---

## 🎯 Summary

**To test Google Pay payment flow TODAY:**

1. ✅ Create subscription products in Play Console
2. ✅ Add your Gmail to License Testing
3. ✅ Upload signed APK to Internal Testing track
4. ✅ Add yourself to Internal Testing testers
5. ✅ Install app from opt-in link
6. ✅ Test payment flow with "Test card, no charge"
7. ✅ Verify backend receives webhook notifications
8. ✅ Complete checklist above

**Real payments available:** After Production track approval (3-7 days after submission)

**Current status:** You can start testing **immediately** with Internal Testing track.

---

**Need Help?**
- Check Android logs: `adb logcat | grep -i billing`
- Review backend webhook logs
- Verify product IDs match between app and Play Console
- Ensure test account is in License Testing list
