//
// Created by Kevin Block on 5/31/25.
//
// Firebase Phone Auth Manual Integration
// Swizzling disabled - notifications and URLs forwarded manually to Firebase
//
// Phase 4 (FCM push): also registers for FCM, forwards the registration token and incoming
// notifications into the shared Kotlin FirebaseMessaging singleton via FcmBridge.

import Foundation
import UIKit
import ComposeApp
import FirebaseCore
import FirebaseAuth
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        print("AppDelegate: Starting initialization")

        // Initialize Firebase FIRST (before KMP init)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
            print("AppDelegate: ✅ Firebase configured")
        }

        // Set up FCM + user-notification delegates (Phase 4 push notifications)
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        // Request notification permission, then register for remote notifications (APNs)
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            print("AppDelegate: Notification permission granted: \(granted)")
            if let error = error {
                print("AppDelegate: ⚠️ Notification authorization error: \(error)")
            }
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }

        // Call KMP Initializer AFTER Firebase setup
        KMPInitializerKt.onDidFinishLaunchingWithOptions()
        print("AppDelegate: ✅ KMP initialized")

        return true
    }

    // MARK: - APNs Token Handling (For Silent Push Verification + FCM)

    // This is called when APNs registration succeeds (devices only, not simulator)
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("AppDelegate: ✅ APNs token registered")

        // Forward token to Firebase Auth for silent push verification
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)

        // Hand the APNs token to FCM so it can mint an FCM registration token
        Messaging.messaging().apnsToken = deviceToken
    }

    // This is called when APNs registration fails (expected on simulator)
    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("AppDelegate: ⚠️ APNs registration failed - Firebase will use reCAPTCHA fallback")
        #if targetEnvironment(simulator)
        print("AppDelegate: Running on simulator - this is expected")
        #endif
    }

    // MARK: - FCM Token (Phase 4)

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        print("AppDelegate: ✅ FCM registration token received")
        // Forward into the shared Kotlin FirebaseMessaging singleton; PushTokenRegistrar's refresh
        // listener then registers it with the backend.
        FcmBridge.shared.onFcmToken(token: token)
    }

    // MARK: - Remote Notification Forwarding (CRITICAL for Phone Auth + FCM data messages)

    // Firebase Phone Auth sends silent push notifications for verification
    // We must forward them to Firebase Auth when swizzling is disabled
    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable : Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {

        print("AppDelegate: Received remote notification")

        // Check if Firebase Auth can handle this notification
        if Auth.auth().canHandleNotification(userInfo) {
            print("AppDelegate: ✅ Notification forwarded to Firebase Auth")
            completionHandler(.noData)
            return
        }

        // Otherwise it's an app/FCM notification — forward to the Kotlin messaging singleton
        FcmBridge.shared.onFcmMessage(userInfo: userInfo)
        completionHandler(.noData)
    }

    // MARK: - Foreground / Tapped Notification Handling (Phase 4)

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        FcmBridge.shared.onFcmMessage(userInfo: userInfo)
        // Show the notification even when the app is in the foreground
        completionHandler([[.banner, .badge, .sound]])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        FcmBridge.shared.onFcmMessage(userInfo: userInfo)
        completionHandler()
    }

    // MARK: - URL Forwarding (For Phone Auth OAuth Callback)

    // Firebase Phone Auth uses URLs for reCAPTCHA callback
    // We must forward them to Firebase Auth when swizzling is disabled
    func application(_ app: UIApplication,
                     open url: URL,
                     options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {

        print("AppDelegate: Received URL: \(url.absoluteString)")

        // Check if Firebase Auth can handle this URL
        if Auth.auth().canHandle(url) {
            print("AppDelegate: ✅ URL forwarded to Firebase Auth")
            return true
        }

        // Handle other deep links here
        print("AppDelegate: URL not related to Firebase Auth")
        return false
    }
}
