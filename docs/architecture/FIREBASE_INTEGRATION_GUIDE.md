# Firebase Analytics, Crashlytics, Performance, and Cloud Messaging Integration Guide

This document provides a complete overview of the Firebase integration added to the Ampairs Kotlin Multiplatform mobile application.

## 🎯 Overview

**Date**: January 2025
**Firebase Services Integrated**:
- ✅ Firebase Analytics
- ✅ Firebase Crashlytics
- ✅ Firebase Performance Monitoring
- ✅ Firebase Cloud Messaging (FCM)

**Platforms Supported**:
- ✅ Android (Native SDK)
- ✅ iOS (CocoaPods)
- ⚠️ Desktop (Not applicable - Firebase is mobile-only)

## 📦 Dependencies

### Android Dependencies

**File**: `composeApp/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.googleServices) // Google Services plugin
    alias(libs.plugins.firebaseCrashlytics) // Crashlytics plugin
    alias(libs.plugins.firebasePerf) // Performance Monitoring plugin
}

sourceSets {
    val androidMain by getting {
        dependencies {
            implementation(libs.firebase.auth)
            implementation(libs.google.firebase.analytics)
            implementation(libs.google.firebase.crashlytics)
            implementation(libs.google.firebase.perf)
            implementation(libs.google.firebase.messaging)
        }
    }
}
```

### iOS Dependencies (CocoaPods)

**File**: `composeApp/build.gradle.kts`

```kotlin
cocoapods {
    pod("FirebaseCore") { version = "~> 11.13" }
    pod("FirebaseAuth") { version = "~> 11.13" }
    pod("FirebaseMessaging") { version = "~> 11.13" }
    pod("FirebaseAnalytics") { version = "~> 11.13" }
    pod("FirebaseCrashlytics") { version = "~> 11.13" }
    pod("FirebasePerformance") { version = "~> 11.13" }
}
```

### Version Catalog

**File**: `gradle/libs.versions.toml`

```toml
[versions]
firebase-bom = "34.4.0"
firebase-crashlytics = "19.4.4"
firebase-analytics = "23.0.0"
firebase-perf = "22.0.2"
firebase-crashlytics-gradle = "3.1.0"
firebase-perf-gradle = "1.4.2"
google-services = "4.4.4"

[libraries]
firebase-auth = { module = "com.google.firebase:firebase-auth", version.ref = "firebase-auth" }
google-firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics", version.ref = "firebase-analytics" }
google-firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx", version.ref = "firebase-crashlytics" }
google-firebase-perf = { group = "com.google.firebase", name = "firebase-perf", version.ref = "firebase-perf" }

[plugins]
googleServices = { id = "com.google.gms.google-services", version.ref = "google-services" }
firebaseCrashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-gradle" }
firebasePerf = { id = "com.google.firebase.firebase-perf", version.ref = "firebase-perf-gradle" }
```

## 🏗️ Architecture

### Common Interfaces (Expect/Actual Pattern)

**Location**: `composeApp/src/commonMain/kotlin/com/ampairs/common/firebase/`

```
firebase/
├── analytics/
│   └── FirebaseAnalytics.kt (expect class)
├── crashlytics/
│   └── FirebaseCrashlytics.kt (expect class)
├── performance/
│   ├── FirebasePerformance.kt (expect class)
│   └── Trace.kt (expect class)
└── di/
    └── FirebaseModule.kt
```

### Platform Implementations

#### Android
- `composeApp/src/androidMain/kotlin/com/ampairs/common/firebase/analytics/FirebaseAnalytics.android.kt`
- `composeApp/src/androidMain/kotlin/com/ampairs/common/firebase/crashlytics/FirebaseCrashlytics.android.kt`
- `composeApp/src/androidMain/kotlin/com/ampairs/common/firebase/performance/FirebasePerformance.android.kt`

#### iOS
- `composeApp/src/iosMain/kotlin/com/ampairs/common/firebase/analytics/FirebaseAnalytics.ios.kt`
- `composeApp/src/iosMain/kotlin/com/ampairs/common/firebase/crashlytics/FirebaseCrashlytics.ios.kt`
- `composeApp/src/iosMain/kotlin/com/ampairs/common/firebase/performance/FirebasePerformance.ios.kt`

### Dependency Injection

**File**: `composeApp/src/commonMain/kotlin/com/ampairs/common/firebase/di/FirebaseModule.kt`

```kotlin
val firebaseModule = module {
    singleOf(::FirebaseAnalytics)
    singleOf(::FirebaseCrashlytics)
    singleOf(::FirebasePerformance)
}
```

**Koin Registration**: `composeApp/src/commonMain/kotlin/Koin.kt`

```kotlin
import com.ampairs.common.firebase.di.firebaseModule

fun initKoin(koinApplication: KoinApplication): KoinApplication {
    koinApplication.modules(
        listOf(
            firebaseModule, // ← Firebase module added here
            // ... other modules
        )
    )
    return koinApplication
}
```

### Navigation Integration (Automatic Screen Tracking)

**File**: `composeApp/src/commonMain/kotlin/AppNavigation.kt`

Firebase Analytics is integrated with Compose Navigation to automatically track screen views:

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val analytics: FirebaseAnalytics = koinInject()

    // Track screen views with Firebase Analytics
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { backStackEntry ->
            val route = backStackEntry.destination.route
            if (route != null) {
                // Extract screen name from route
                val screenName = extractScreenName(route)
                val screenClass = route.substringBefore("?").substringBefore("/")

                // Log screen view to Firebase Analytics
                analytics.setCurrentScreen(screenName, screenClass)
            }
        }
    }

    // ... NavHost setup
}
```

**Benefits:**
- ✅ **Zero boilerplate** - No manual tracking code in each screen
- ✅ **Consistent naming** - All screens follow same naming convention
- ✅ **Automatic updates** - New screens tracked automatically
- ✅ **Type-safe routes** - Uses Kotlin serialization routes
- ✅ **Parameter handling** - Automatically strips route parameters

## 🚀 Initialization

### Android

**Automatic**: Firebase is automatically initialized by the Google Services plugin when `google-services.json` is present in `composeApp/`.

### iOS

**Manual**: Firebase must be initialized in `MainViewController.kt`

```kotlin
@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    // Initialize Firebase for iOS
    if (FIRApp.defaultApp() == null) {
        FIRApp.configure()
    }
    // ... rest of initialization
}
```

## 💻 Usage Examples

### Analytics

#### Automatic Screen Tracking

**Screen views are automatically tracked** via Compose Navigation integration in `AppNavigation.kt`:

```kotlin
// Automatic tracking - no manual code required!
// When navigating: navController.navigate(Route.Customer)
// Analytics logs: Screen: "Customer", Class: "Route.Customer"
```

**Tracked automatically for all routes:**
- Main routes: `Route.Login` → "Login"
- Auth routes: `AuthRoute.Phone` → "Phone"
- Workspace routes: `WorkspaceRoute.Members` → "Workspace_Members"
- Customer routes: `CustomerRoute.List` → "Customer_List"
- Product routes: `ProductRoute.ProductForm` → "Product_ProductForm"

#### Manual Event Tracking

```kotlin
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.AnalyticsParams
import org.koin.compose.koinInject

@Composable
fun MyScreen() {
    val analytics: FirebaseAnalytics = koinInject()

    LaunchedEffect(Unit) {
        // Manual screen tracking (only if needed - automatic tracking covers most cases)
        analytics.setCurrentScreen("MyScreen", "MyScreenClass")

        // Log custom event
        analytics.logEvent(AnalyticsEvents.SELECT_CONTENT, mapOf(
            AnalyticsParams.CONTENT_TYPE to "product",
            AnalyticsParams.ITEM_ID to "12345"
        ))

        // Set user properties
        analytics.setUserProperty("user_type", "premium")
        analytics.setUserId("user123")
    }
}
```

#### Authentication Events & User ID Tracking

Login, logout, sign-up events and user IDs are automatically tracked throughout the app:

**LoginViewModel (After Authentication):**
```kotlin
// After successful login/signup:
analytics.setUserId(userData.id)  // Set user ID for session tracking

if (isNewUser) {
    analytics.logEvent(AnalyticsEvents.SIGN_UP, mapOf(
        AnalyticsParams.METHOD to "backend_api" or "firebase_phone"
    ))
} else {
    analytics.logEvent(AnalyticsEvents.LOGIN, mapOf(
        AnalyticsParams.METHOD to "backend_api" or "firebase_phone"
    ))
}
```

**UserSelectionViewModel (User Switching):**
```kotlin
// When user selects different account:
analytics.setUserId(selectedUserId)  // Update to new user

// When removing user:
analytics.setUserId(null)  // Clear if current user
```

**AppScreenWithHeader (Session Management):**
```kotlin
// On app start (if user already logged in):
analytics.setUserId(userEntity.id)

// On logout:
analytics.logEvent(AnalyticsEvents.LOGOUT)
analytics.setUserId(null)  // Clear user ID

// On user switch:
analytics.setUserId(null)  // Clear before switching
```

**User ID Lifecycle:**
1. **Login/Signup** → User ID set immediately after authentication
2. **App Start** → User ID restored from saved session
3. **User Switch** → Old ID cleared, new ID set on selection
4. **Logout** → User ID cleared completely

### Crashlytics

```kotlin
import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
import com.ampairs.common.firebase.crashlytics.CrashlyticsKeys
import org.koin.compose.koinInject

@Composable
fun MyViewModel() {
    val crashlytics: FirebaseCrashlytics = koinInject()

    fun handleError(error: Exception) {
        // Log breadcrumb
        crashlytics.log("User attempted action X")

        // Set custom keys
        crashlytics.setCustomKey(CrashlyticsKeys.SCREEN_NAME, "MyScreen")
        crashlytics.setCustomKey(CrashlyticsKeys.ACTION, "submit_form")
        crashlytics.setCustomKey(CrashlyticsKeys.USER_ID, userId)

        // Record non-fatal exception
        crashlytics.recordException(error)

        // Set user identifier
        crashlytics.setUserId(userId)
    }
}
```

### Performance Monitoring

#### Automatic Screen Load Tracking

**Screen load times are automatically tracked** via Compose Navigation integration in `AppNavigation.kt`:

```kotlin
// Automatic tracking - no manual code required!
// When navigating: navController.navigate(Route.Customer)
// Performance trace: "screen_load"
// Attributes: { screen_name: "Customer", screen_class: "Route.Customer" }
// Auto-stops after 3 seconds or on next navigation
```

**Console Output:**
```
Performance: Screen load trace started - Customer
Performance: Screen load trace auto-stopped - Customer
```

#### Manual Performance Tracking

Use helper functions for API and database operations:

```kotlin
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.firebase.performance.trackApiPerformance
import com.ampairs.common.firebase.performance.trackPerformance

class CustomerRepository(
    private val customerApi: CustomerApi,
    private val customerDao: CustomerDao,
    private val performance: FirebasePerformance,
) {
    // Track API request performance automatically
    suspend fun getCustomers(): Response<List<Customer>> {
        return trackApiPerformance(
            performance = performance,
            endpoint = "/api/v1/customers",
            method = "GET"
        ) {
            customerApi.getCustomers()
        }
    }

    // Track database operations
    suspend fun saveCustomers(customers: List<Customer>) {
        trackPerformance(
            performance = performance,
            traceName = PerformanceTraces.DATABASE_WRITE,
            attributes = mapOf(
                PerformanceAttributes.OPERATION_TYPE to "batch_insert",
                PerformanceAttributes.ENTITY_NAME to "Customer",
                "count" to customers.size.toString()
            )
        ) {
            customerDao.insertAll(customers.map { it.toEntity() })
        }
    }
}
```

**📖 For complete performance tracking guide, see [FIREBASE_PERFORMANCE_GUIDE.md](./FIREBASE_PERFORMANCE_GUIDE.md)**

### Firebase Cloud Messaging (FCM)

```kotlin
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.ampairs.common.firebase.messaging.FcmTopics
import com.ampairs.common.firebase.messaging.RemoteMessage
import org.koin.compose.koinInject

@Composable
fun MyApp() {
    val messaging: FirebaseMessaging = koinInject()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Get FCM token
        val token = messaging.getToken()
        println("FCM Token: $token")

        // Subscribe to topics
        messaging.subscribeToTopic(FcmTopics.ALL_USERS)
        messaging.subscribeToTopic(FcmTopics.workspace("workspace123"))

        // Set up message listener
        messaging.setOnMessageReceivedListener { message ->
            println("Received message: ${message.notification?.title}")
            // Handle the message
        }

        // Set up token refresh listener
        messaging.setOnTokenRefreshListener { newToken ->
            println("Token refreshed: $newToken")
            // Send token to your server
        }
    }
}

// Unsubscribe from topics
suspend fun unsubscribeFromWorkspace(workspaceId: String) {
    val messaging: FirebaseMessaging = koinInject()
    messaging.unsubscribeFromTopic(FcmTopics.workspace(workspaceId))
}

// Handle incoming messages
fun handleRemoteMessage(message: RemoteMessage) {
    when (message.data["type"]) {
        FcmMessageTypes.ORDER_UPDATE -> {
            val orderId = message.data[FcmDataKeys.ENTITY_ID]
            // Navigate to order details or show notification
        }
        FcmMessageTypes.WORKSPACE_INVITATION -> {
            val workspaceId = message.data[FcmDataKeys.WORKSPACE_ID]
            // Show invitation dialog
        }
    }
}
```

## 📱 Platform-Specific Setup

### Android Setup

1. ✅ **Dependencies added** via `build.gradle.kts`
2. ✅ **Plugins configured**: Google Services, Crashlytics, Performance
3. ⚠️ **Required**: Add `google-services.json` to `composeApp/` directory
   - Download from Firebase Console → Project Settings → Your Android App
4. ✅ **Auto-initialization**: Firebase initializes automatically on app start

### iOS Setup

1. ✅ **Dependencies added** via CocoaPods
2. ✅ **Firebase initialized** in `MainViewController.kt`
3. ⚠️ **Required**: Add `GoogleService-Info.plist` to Xcode project
   - Download from Firebase Console → Project Settings → Your iOS App
   - Add to `iosApp/` directory and include in Xcode target
4. ⚠️ **Required**: Add Crashlytics upload script to Xcode build phases
   - See detailed instructions in `IOS_CRASHLYTICS_SETUP.md`

## 🔧 Configuration Files Needed

### Android
- **File**: `composeApp/google-services.json`
- **Source**: Firebase Console → Android App → Download Config File
- **Location**: Place in `composeApp/` directory (root of app module)

### iOS
- **File**: `GoogleService-Info.plist`
- **Source**: Firebase Console → iOS App → Download Config File
- **Location**: Add to `iosApp/` directory and Xcode project

## 🧪 Testing

### Test Analytics

```kotlin
analytics.logEvent("test_event", mapOf("test_param" to "test_value"))
```
**Verify**: Firebase Console → Analytics → Events (may take 24 hours)

### Test Crashlytics

```kotlin
crashlytics.log("Test log message")
crashlytics.recordException(Exception("Test exception"))
```
**Verify**: Firebase Console → Crashlytics (appears in 5-10 minutes)

### Test Performance

```kotlin
val trace = performance.newTrace("test_trace")
trace.start()
delay(1000)
trace.stop()
```
**Verify**: Firebase Console → Performance (may take 12-24 hours)

## 🎨 Constants & Best Practices

### Pre-defined Event Names
- Use `AnalyticsEvents.*` constants for standard events
- Use `AnalyticsParams.*` for standard parameters
- Use `CrashlyticsKeys.*` for custom key names
- Use `PerformanceTraces.*` for trace names

### Best Practices

**Analytics**:
- ✅ Log significant user actions
- ✅ Track screen views automatically
- ✅ Set user properties for segmentation
- ❌ Don't log PII (personally identifiable information)

**Crashlytics**:
- ✅ Log breadcrumbs before potential errors
- ✅ Set custom keys for context
- ✅ Record non-fatal exceptions
- ❌ Don't log sensitive data in custom keys

**Performance**:
- ✅ Trace critical user flows
- ✅ Measure network requests
- ✅ Track screen load times
- ❌ Don't create too many traces (Firebase limits apply)

## 📊 Firebase Console

Access your Firebase data at:
- **Analytics**: https://console.firebase.google.com → Analytics
- **Crashlytics**: https://console.firebase.google.com → Crashlytics
- **Performance**: https://console.firebase.google.com → Performance

## 🔗 Related Documentation

- [IOS_CRASHLYTICS_SETUP.md](./IOS_CRASHLYTICS_SETUP.md) - iOS Xcode build phase setup
- [CLAUDE.md](./CLAUDE.md) - Project architecture and patterns
- [Firebase iOS Documentation](https://firebase.google.com/docs/ios/setup)
- [Firebase Android Documentation](https://firebase.google.com/docs/android/setup)

## ✅ Checklist for New Developers

- [ ] Add `google-services.json` for Android
- [ ] Add `GoogleService-Info.plist` for iOS
- [ ] Add Crashlytics upload script to Xcode (see `IOS_CRASHLYTICS_SETUP.md`)
- [ ] Run `./gradlew composeApp:assembleDebug` to verify Android build
- [ ] Run `pod install` in `iosApp/` directory for iOS
- [ ] Build iOS app in Xcode to verify configuration
- [ ] Test each Firebase service with provided examples
- [ ] Verify data appears in Firebase Console

---

**Last Updated**: January 2025
**Integration Version**: Firebase iOS SDK 11.13.0, Firebase Android SDK via BOM 34.4.0
**KMP Version**: Kotlin 2.2.21, Compose Multiplatform 1.9.1
