# Google Play Payment Testing - Quick Start

**⏱️ Time to first test: ~30 minutes**

---

## 🎯 Answer: You Can Test RIGHT NOW!

Use Google Play's **Internal Testing** track with **test cards** - **NO REAL MONEY needed**.

---

## 🚀 5-Minute Setup

### 1. Create Products in Play Console (5 min)

```
Go to: https://play.google.com/console
→ Select app: com.ampairs.app
→ Monetization → Subscriptions → Create subscription

Create these products (copy-paste ready):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Product ID: ampairs_professional_monthly
Name: Professional Plan (Monthly)
Base plan: Monthly
Price: ₹999/month
Status: Active
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Product ID: ampairs_professional_yearly
Name: Professional Plan (Yearly)
Base plan: Yearly
Price: ₹9,999/year
Free trial: 14 days
Status: Active
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Product ID: ampairs_premium_monthly
Name: Premium Plan (Monthly)
Base plan: Monthly
Price: ₹1,999/month
Status: Active
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Product ID: ampairs_premium_yearly
Name: Premium Plan (Yearly)
Base plan: Yearly
Price: ₹19,999/year
Free trial: 14 days
Status: Active
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 2. Add Test Users (2 min)

```
Go to: Setup → License testing

Add your Gmail addresses:
- yourname@gmail.com
- team@gmail.com

License test response: RESPOND_NORMALLY
```

### 3. Build & Upload (10 min)

```bash
# In terminal
cd /Users/omprakashsrv/StudioProjects/ampairs-app
./gradlew composeApp:bundleRelease

# Upload the AAB file to:
# Testing → Internal testing → Create new release
# File: composeApp/build/outputs/bundle/release/composeApp-release.aab
```

### 4. Add Testers & Get Link (2 min)

```
In Internal testing → Testers tab:
1. Create email list: "Payment Testers"
2. Add same emails from License testing
3. Copy opt-in link: https://play.google.com/apps/internaltest/...
```

### 5. Install & Test (5 min)

```
On your Android device:
1. Open opt-in link in browser
2. Sign in with test account
3. "Become a tester" → Install app
4. Open app → Sign in → Go to Plans
5. Select plan → See "Test card, no charge" ✅
6. Complete purchase → Verify success 🎉
```

---

## 📱 What You'll See During Testing

### Payment Sheet
```
┌─────────────────────────────────────┐
│  Professional Plan (Monthly)        │
│  ₹999.00/month                      │
│  14-day free trial                  │
│                                     │
│  🧪 Test card                       │
│  No charge will be made             │
│                                     │
│  [ Subscribe ]                      │
└─────────────────────────────────────┘
```

**✅ This means testing is working correctly!**

---

## 🐛 Common Issues (1-Minute Fixes)

### "Product not found"
```bash
# Fix: Wait 5 minutes after creating products, or:
- Verify product ID matches exactly
- Check product status is "Active"
```

### "Billing unavailable"
```bash
# Fix: Device setup issue
- Sign in to Play Store with test account
- Ensure test account is in License Testing list
```

### "App not configured for billing"
```bash
# Fix: Wrong build type
- Use RELEASE build (not debug)
- Upload to Internal Testing track
- Install from opt-in link (not direct APK)
```

---

## 🎯 Test Checklist

**5-Minute Smoke Test:**

```
✅ Products load on Plans screen
✅ Tap "Subscribe" → Payment sheet appears
✅ See "Test card, no charge" message
✅ Complete purchase → Success callback
✅ Subscription shows on Subscription screen
✅ Backend webhook received notification
```

**If all ✅ → Payment integration working! 🎉**

---

## 📊 Backend Webhook Verification

Your backend should receive POST requests like this:

```json
{
  "version": "1.0",
  "packageName": "com.ampairs.app",
  "eventTimeMillis": "1733145600000",
  "subscriptionNotification": {
    "version": "1.0",
    "notificationType": 4,  // SUBSCRIPTION_PURCHASED
    "purchaseToken": "test_purchase_token_123...",
    "subscriptionId": "ampairs_professional_monthly"
  }
}
```

**Webhook endpoint:** Check your backend logs for incoming POST requests.

---

## 🚀 Next Steps After Testing

Once testing is complete:

1. ✅ Complete full test checklist (see GOOGLE_PLAY_TESTING_GUIDE.md)
2. 📝 Submit to Production track for review
3. ⏳ Wait 3-7 days for approval
4. 💰 Real payments go live after approval

---

## 📞 Quick Help

**Products not showing?**
```
1. Check Play Console → Monetization → Subscriptions
2. Verify product status = "Active"
3. Wait 5-10 minutes for propagation
4. Restart app
```

**Purchase failing?**
```
1. Check Android logs: adb logcat | grep -i billing
2. Look for specific error codes
3. Verify test account in License Testing
4. Try: Clear Play Store cache/data
```

**Backend not receiving webhooks?**
```
1. Check Play Console → Monetization → Monetization setup
2. Configure Real-time developer notifications
3. Verify webhook URL is publicly accessible
4. Check backend server logs
```

---

## 🎓 Full Documentation

See `GOOGLE_PLAY_TESTING_GUIDE.md` for:
- Complete step-by-step instructions
- Detailed error troubleshooting
- Production deployment guide
- Backend webhook configuration
- Advanced testing scenarios

---

**🎯 Bottom Line:** You can start testing Google Pay payments **TODAY** with test cards. No app approval needed, no real money charged!
