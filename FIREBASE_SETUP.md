# Firebase Authentication Setup Guide

This guide explains the Firebase Authentication integration for the Ampairs Mobile Application.

## ✅ Current Status: Production Ready

**Firebase Phone Authentication Status:**

- ✅ **Android**: Fully functional with native Firebase Android SDK
- ✅ **iOS**: Fully functional with native Firebase iOS SDK via CocoaPods
- ⏳ **Desktop**: Not supported by Firebase (uses Backend API)

## Overview

The app supports **two authentication methods**:

1. **Backend API Authentication** ✅ **ACTIVE** - Phone + OTP via Spring Boot backend server
2. **Firebase Authentication** ✅ **ACTIVE** - Phone + OTP via native Firebase SDKs (Android & iOS)

### Platform Support

| Platform | Backend API | Firebase Phone Auth | Status |
|----------|-------------|---------------------|--------|
| Android  | ✅ Active | ✅ **ACTIVE** (Native SDK) | Both Methods Available |
| iOS      | ✅ Active | ✅ **ACTIVE** (Native SDK via CocoaPods) | Both Methods Available |
| Desktop  | ✅ Active | ⏳ Not Supported | Backend API Only |

**Note:** Both Android and iOS now have full Firebase Phone Authentication support using native SDKs. iOS implementation uses CocoaPods for Firebase integration.

## Implementation Architecture

The Firebase authentication uses **native platform SDKs** directly:

- **Android**: Native Firebase Android SDK with Activity context
  - `implementation(libs.firebase.auth)`
  - reCAPTCHA verification via Activity provider
  - Returns Firebase ID token (JWT)

- **iOS**: Native Firebase iOS SDK via CocoaPods
  - CocoaPods integration: `pod("FirebaseAuth")`, `pod("FirebaseCore")`
  - APNs for silent push notifications
  - Returns Firebase ID token (JWT)

- **Common Layer:**
  - ✅ Expect/actual pattern for platform implementations
  - ✅ Domain models and result types (`FirebaseAuthResult<T>`)
  - ✅ Repository layer with state management
  - ✅ ViewModel integration for UI
  - ✅ UI components with auth method toggle

## Firebase Project Setup

To enable Firebase Phone Auth, configure Firebase projects for each platform:

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **Add Project** or select existing project
3. Enter project name (e.g., "Ampairs Mobile")
4. Disable Google Analytics (optional)
5. Click **Create Project**

### 2. Enable Phone Authentication

1. In Firebase Console, navigate to **Authentication**
2. Click **Get Started**
3. Go to **Sign-in method** tab
4. Find **Phone** provider and click to enable
5. Click **Save**

### 3. Configure Phone Number Test Accounts (Optional)

For development and testing without SMS costs:

1. In **Authentication > Sign-in method**
2. Scroll to **Phone numbers for testing**
3. Add test phone numbers with verification codes (e.g., `+919999999999` → `123456`)

## Android Configuration

### 1. Register Android App in Firebase

1. In Firebase Console, click **Add app** > **Android**
2. **Android package name**: `com.ampairs.app` (must match `applicationId` in `build.gradle.kts`)
3. **App nickname**: "Ampairs Android"
4. **Debug SHA-1**: Run this command to get your debug key:
   ```bash
   cd ~/.android
   keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Copy the SHA-1 fingerprint and paste it into Firebase Console
6. Click **Register app**

### 2. Download google-services.json

1. Download `google-services.json` from Firebase Console
2. Place it in: `/ampairs-mp-app/composeApp/google-services.json`

### 3. Add Google Services Plugin

**Already configured in the project**, but verify:

```kotlin
// composeApp/build.gradle.kts
plugins {
    // ... existing plugins
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

### 4. Verify Configuration

Ensure `google-services.json` is in the correct location:
```
ampairs-mp-app/
└── composeApp/
    ├── google-services.json  ← HERE
    ├── build.gradle.kts
    └── src/
```

### 5. Test Android App

```bash
./gradlew composeApp:assembleDebug
./gradlew composeApp:installDebug
```

## iOS Configuration

### ✅ Swift Package Manager (Recommended)

Firebase iOS dependencies are managed via **Swift Package Manager** directly in Xcode.

**See [IOS_FIREBASE_SPM_SETUP.md](./IOS_FIREBASE_SPM_SETUP.md) for complete step-by-step setup instructions.**

### Quick Setup Steps

### 1. Register iOS App in Firebase

1. In Firebase Console, click **Add app** > **iOS**
2. **iOS bundle ID**: `com.ampairs.app` (must match your Xcode project)
3. **App nickname**: "Ampairs iOS"
4. Click **Register app**

### 2. Download GoogleService-Info.plist

1. Download `GoogleService-Info.plist` from Firebase Console
2. Place it in: `/ampairs-mp-app/iosApp/GoogleService-Info.plist`

### 3. Add to Xcode Project

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Drag `GoogleService-Info.plist` into the project navigator
3. Ensure **Copy items if needed** is checked
4. Select target: **iosApp**
5. Click **Finish**

### 4. Configure Push Notifications (Required for Firebase Auth)

1. In Xcode, select your project in the navigator
2. Select **iosApp** target
3. Go to **Signing & Capabilities** tab
4. Click **+ Capability**
5. Add **Push Notifications**

### 5. Enable Background Modes (Optional)

For better auth experience:

1. Add **Background Modes** capability
2. Check **Remote notifications**

### 6. Verify Configuration

Ensure `GoogleService-Info.plist` is in the correct location:
```
ampairs-mp-app/
└── iosApp/
    ├── GoogleService-Info.plist  ← HERE
    ├── iosApp.xcodeproj
    └── iosApp/
```

### 7. Add Firebase via Swift Package Manager

1. In Xcode: **File → Add Package Dependencies...**
2. Enter: `https://github.com/firebase/firebase-ios-sdk`
3. Select version: `11.0.0` (or latest)
4. Choose products: **FirebaseAuth**, **FirebaseCore**
5. Click **Add Package**

### 8. Initialize Firebase in iOSApp.swift

```swift
import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### 9. Test iOS App

```bash
# Build the Kotlin framework
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode

# Then run in Xcode (Cmd+R)
```

## Desktop (Future Implementation)

Currently, Firebase Phone Auth is **not supported** on Desktop. The implementation returns an error message.

### Planned QR Code Authentication

Desktop will use QR code authentication similar to WhatsApp Web:

1. User opens desktop app
2. Desktop generates QR code with session token
3. User scans QR code with mobile app (Android/iOS)
4. Mobile app authenticates and confirms desktop session
5. Desktop receives auth tokens and completes login

**Status**: Stub implementation exists, full implementation planned for future release.

## Current Usage in App

### Authentication Method

Platforms support the following authentication methods:

- **Android**: Backend API ✅ + Firebase Auth ✅ (toggle available)
- **iOS**: Backend API ✅ + Firebase Auth ✅ (toggle available if Firebase configured)
- **Desktop**: Backend API ✅ only (Firebase not supported)

### Firebase Support Detection

The app dynamically detects Firebase availability:

```kotlin
// LoginViewModel.kt
val isFirebaseSupported: Boolean = firebaseAuthRepository.isSupported()

// PhoneScreen.kt - Firebase toggle shows if supported
if (viewModel.isFirebaseSupported) {
    // Show auth method selector (Backend API / Firebase)
}
```

**On Android**: `isSupported()` always returns `true` (Firebase SDK always available)

**On iOS**: `isSupported()` returns `true` if Firebase is configured via CocoaPods

**On Desktop**: `isSupported()` returns `false` (Firebase not supported)

### User Flow

**Login with Backend API:**
1. Select "Backend API" (or default if Firebase unavailable)
2. Enter phone number → OTP sent via backend
3. Enter OTP code → Backend verifies
4. Authenticated with backend JWT

**Login with Firebase:**
1. Select "Firebase Auth" from toggle
2. Enter phone number → Firebase sends SMS
3. Enter OTP code → Firebase verifies
4. Authenticated with Firebase ID token (JWT)

## Troubleshooting

### Android Issues

**Problem**: Firebase not initializing
- **Solution**: Verify `google-services.json` is in `composeApp/` directory
- **Check**: SHA-1 fingerprint is registered in Firebase Console

**Problem**: SMS not received
- **Solution**:
  - Check phone number format includes country code (+91...)
  - Verify Firebase quota limits not exceeded
  - Use test phone numbers for development

### iOS Issues

**Problem**: Firebase not initializing
- **Solution**:
  - Verify `GoogleService-Info.plist` is added to Xcode project
  - Check bundle ID matches Firebase Console

**Problem**: SMS not received
- **Solution**:
  - Ensure Push Notifications capability is enabled
  - Check Apple Developer account has valid certificates

### Desktop

**Expected**: Firebase auth not supported message
- This is normal behavior - Desktop uses Backend API only

## Security Considerations

### Production Deployment

1. **Enable App Check** (recommended):
   - Prevents abuse and unauthorized access
   - Follow [Firebase App Check documentation](https://firebase.google.com/docs/app-check)

2. **Configure reCAPTCHA** (web-based flows):
   - Add authorized domains in Firebase Console

3. **Rate Limiting**:
   - Firebase has built-in SMS quota limits
   - Monitor usage in Firebase Console > Usage tab

4. **Test Phone Numbers**:
   - Remove test numbers before production release
   - Use only for development/QA testing

### Backend Integration

After Firebase authenticates user, you may want to:

1. Send Firebase UID to backend
2. Link Firebase account with backend user account
3. Sync user data between Firebase and backend

**Note**: Current implementation completes Firebase auth but doesn't sync with backend yet.

## File Structure

```
ampairs-mp-app/
├── composeApp/
│   ├── google-services.json              # Android Firebase config
│   └── src/
│       ├── commonMain/kotlin/com/ampairs/auth/
│       │   ├── domain/
│       │   │   ├── AuthMethod.kt         # Auth method enum
│       │   │   └── FirebaseAuthResult.kt # Result types
│       │   ├── firebase/
│       │   │   ├── FirebaseAuthProvider.kt      # Expect interface
│       │   │   └── FirebaseAuthRepository.kt    # Repository
│       │   └── viewmodel/
│       │       └── LoginViewModel.kt     # Updated with Firebase support
│       ├── androidMain/kotlin/com/ampairs/auth/firebase/
│       │   └── FirebaseAuthProvider.android.kt  # Android implementation
│       ├── iosMain/kotlin/com/ampairs/auth/firebase/
│       │   └── FirebaseAuthProvider.ios.kt      # iOS implementation
│       └── desktopMain/kotlin/com/ampairs/auth/firebase/
│           └── FirebaseAuthProvider.desktop.kt  # Desktop stub
└── iosApp/
    └── GoogleService-Info.plist          # iOS Firebase config
```

## Additional Resources

- [Firebase Phone Auth Documentation](https://firebase.google.com/docs/auth/android/phone-auth)
- [GitLive Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [Firebase Console](https://console.firebase.google.com/)
- [Kotlin Multiplatform Firebase Best Practices](https://firebase.google.com/docs/android/kotlin-multiplatform)

## Support

For issues or questions:
- Check existing GitHub issues: [ampairs/ampairs-mp-app](https://github.com/ampairs/ampairs-mp-app/issues)
- Review Firebase Console logs for authentication errors
- Contact development team for Firebase project access

---

## Implementation Roadmap

### Phase 1: ✅ **COMPLETE** - Architecture & Foundation
- ✅ Firebase SDK dependencies configured
- ✅ Expect/actual pattern for platform implementations
- ✅ Domain models and result types
- ✅ Repository and ViewModel integration
- ✅ UI components with conditional Firebase toggle

### Phase 2: ✅ **COMPLETE** - Android Implementation
- ✅ Activity context injection for reCAPTCHA
- ✅ Native Firebase Android SDK integration
- ✅ Verification callback handling with PhoneAuthProvider
- ✅ Firebase ID token (JWT) retrieval
- ✅ Production-ready with Activity provider pattern

### Phase 3: ✅ **COMPLETE** - iOS Implementation
- ✅ CocoaPods configuration for Firebase iOS SDK
- ✅ Native Firebase iOS SDK integration
- ✅ iOS-specific verification flows with FIRPhoneAuthProvider
- ✅ Firebase ID token (JWT) retrieval
- ✅ Production-ready with native Objective-C interop

### Phase 4: ⏳ **FUTURE** - Desktop QR Code Auth
- ⏳ QR code generation with session tokens
- ⏳ Mobile app QR scanner
- ⏳ Desktop session confirmation flow
- ⏳ Backend session management

---

**Last Updated**: January 2025
**Status**: ✅ Production Ready (Android & iOS)
**Firebase SDK**: Native Android SDK + CocoaPods iOS SDK
**KMP Version**: Kotlin 2.2.20
