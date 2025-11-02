# Firebase Cloud Messaging (FCM) Setup Guide

This document explains how to configure Firebase Cloud Messaging for push notifications in the Ampairs KMP mobile app.

## Prerequisites

✅ **Already Completed:**
- Firebase CocoaPods/Android dependencies added
- FCM implementation created for Android and iOS
- Koin modules configured for dependency injection

## Platform-Specific Setup

### Android Setup

#### 1. Create FirebaseMessagingService

Create a custom service to handle FCM messages and token refresh:

**File**: `composeApp/src/androidMain/kotlin/com/ampairs/app/AmpairsFirebaseMessagingService.kt`

```kotlin
package com.ampairs.app

import android.util.Log
import com.ampairs.common.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class AmpairsFirebaseMessagingService : FirebaseMessagingService() {
    private val messaging: FirebaseMessaging by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        // Notify the common messaging class
        messaging.onTokenRefreshed(token)

        // Send token to your server
        // sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received: ${message.notification?.title}")

        // Notify the common messaging class
        messaging.onMessageReceived(message)

        // Show notification if app is in background
        if (message.notification != null) {
            showNotification(message)
        }
    }

    private fun showNotification(message: RemoteMessage) {
        // Implement your notification display logic
        // Use NotificationManager to show notification
    }
}
```

#### 2. Register Service in AndroidManifest.xml

Add the service declaration in `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<application>
    <!-- ... other declarations ... -->

    <service
        android:name=".AmpairsFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>

    <!-- Optional: Default notification icon and color -->
    <meta-data
        android:name="com.google.firebase.messaging.default_notification_icon"
        android:resource="@drawable/ic_notification" />
    <meta-data
        android:name="com.google.firebase.messaging.default_notification_color"
        android:resource="@color/notification_color" />
    <meta-data
        android:name="com.google.firebase.messaging.default_notification_channel_id"
        android:value="@string/default_notification_channel_id" />
</application>
```

#### 3. Create Notification Channel (Android 8.0+)

In your MainActivity or Application class:

```kotlin
private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "default_channel",
            "Default Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Default notification channel for Ampairs"
            enableLights(true)
            enableVibration(true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
```

#### 4. Request Notification Permission (Android 13+)

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
```

### iOS Setup

#### 1. Enable Push Notifications in Xcode

1. Open `iosApp.xcworkspace` in Xcode
2. Select the `iosApp` target
3. Go to "Signing & Capabilities" tab
4. Click "+ Capability"
5. Add "Push Notifications"
6. Add "Background Modes" and enable "Remote notifications"

#### 2. Configure AppDelegate

Update `iosApp/iosApp/AppDelegate.swift`:

```swift
import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Configure Firebase
        FirebaseApp.configure()

        // Set up FCM
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        // Request notification permissions
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            print("Notification permission granted: \(granted)")
        }

        application.registerForRemoteNotifications()

        return true
    }

    // MARK: - FCM Token
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("FCM token: \(fcmToken ?? "")")

        // Notify Kotlin code
        if let token = fcmToken {
            // Call your Kotlin FirebaseMessaging instance
            // messaging.onTokenRefreshed(token)
        }
    }

    // MARK: - Remote Notifications
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register for remote notifications: \(error)")
    }

    // MARK: - Notification Handling
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo

        // Notify Kotlin code
        // messaging.onMessageReceived(userInfo)

        // Show notification even when app is in foreground
        completionHandler([[.banner, .badge, .sound]])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo

        // Handle notification tap
        // Navigate to specific screen based on notification data

        completionHandler()
    }
}
```

#### 3. Upload APNs Certificate to Firebase

1. Go to Firebase Console → Project Settings → Cloud Messaging
2. Under "Apple app configuration", upload your APNs authentication key or certificate
3. For development, use the APNs Sandbox certificate
4. For production, use the APNs Production certificate

## Testing FCM

### Test Token Retrieval

```kotlin
@Composable
fun TestFcmScreen() {
    val messaging: FirebaseMessaging = koinInject()
    var token by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        token = messaging.getToken()
    }

    Text("FCM Token: $token")
}
```

### Send Test Message from Firebase Console

1. Go to Firebase Console → Cloud Messaging
2. Click "Send your first message"
3. Enter notification title and text
4. Click "Send test message"
5. Enter your FCM token
6. Click "Test"

### Test Topic Subscription

```kotlin
suspend fun testTopicSubscription() {
    val messaging: FirebaseMessaging = koinInject()

    // Subscribe to test topic
    messaging.subscribeToTopic("test_topic")

    // Send message to topic from Firebase Console
    // Topic name: test_topic
}
```

## Common Issues & Solutions

### Android

**Issue**: Token is null
- **Solution**: Make sure `google-services.json` is in `composeApp/` directory
- **Solution**: Ensure Google Services plugin is applied in `build.gradle.kts`

**Issue**: Messages not received in background
- **Solution**: Implement `FirebaseMessagingService` properly
- **Solution**: Add service to `AndroidManifest.xml`

**Issue**: Notification doesn't show
- **Solution**: Create notification channel for Android 8.0+
- **Solution**: Request POST_NOTIFICATIONS permission for Android 13+

### iOS

**Issue**: Token is nil
- **Solution**: Enable Push Notifications capability in Xcode
- **Solution**: Upload APNs certificate to Firebase Console
- **Solution**: Call `registerForRemoteNotifications()` in AppDelegate

**Issue**: Messages not received
- **Solution**: Set `Messaging.messaging().delegate = self` in AppDelegate
- **Solution**: Implement `didReceiveRemoteNotification` in AppDelegate
- **Solution**: Enable Background Modes → Remote notifications

**Issue**: APNs certificate errors
- **Solution**: Regenerate APNs certificate/key from Apple Developer Portal
- **Solution**: Upload correct certificate (Sandbox for dev, Production for release)

## Message Payload Structure

### Notification Message (Shows as notification)

```json
{
  "notification": {
    "title": "New Order",
    "body": "You have a new order from Customer XYZ",
    "image": "https://example.com/image.png"
  },
  "data": {
    "type": "order_update",
    "entity_id": "order_12345",
    "workspace_id": "workspace_abc"
  }
}
```

### Data Message (Silent, app handles display)

```json
{
  "data": {
    "type": "chat_message",
    "title": "New Message",
    "body": "You have a new message",
    "entity_id": "message_67890",
    "workspace_id": "workspace_abc",
    "deep_link": "ampairs://chat/67890"
  }
}
```

## Security Best Practices

1. **Never log FCM tokens in production**
2. **Validate message sender** on your backend
3. **Use topic permissions** to control who can send to topics
4. **Encrypt sensitive data** in message payloads
5. **Implement rate limiting** on your backend for sending messages
6. **Store tokens securely** (use encrypted storage)

## Additional Resources

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [FCM Android Setup](https://firebase.google.com/docs/cloud-messaging/android/client)
- [FCM iOS Setup](https://firebase.google.com/docs/cloud-messaging/ios/client)
- [FCM Best Practices](https://firebase.google.com/docs/cloud-messaging/concept-options)

---

**Last Updated**: January 2025
**KMP Version**: Kotlin 2.2.21, Compose Multiplatform 1.9.1
**Firebase iOS SDK**: 11.13.0
**Firebase Android SDK**: 24.1.0
