//
// Created by Kevin Block on 5/31/25.
//
// Firebase Phone Auth Manual Integration
// Swizzling disabled - notifications and URLs forwarded manually to Firebase

import Foundation
import UIKit
import ComposeApp
import FirebaseCore
import FirebaseAuth

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        print("AppDelegate: Starting initialization")

        // Initialize Firebase FIRST (before KMP init)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
            print("AppDelegate: ✅ Firebase configured")
        }

        // Call KMP Initializer AFTER Firebase setup
        KMPInitializerKt.onDidFinishLaunchingWithOptions()
        print("AppDelegate: ✅ KMP initialized")

        return true
    }

    // MARK: - APNs Token Handling (For Silent Push Verification)

    // This is called when APNs registration succeeds (devices only, not simulator)
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("AppDelegate: ✅ APNs token registered")

        // Forward token to Firebase Auth for silent push verification
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
    }

    // This is called when APNs registration fails (expected on simulator)
    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("AppDelegate: ⚠️ APNs registration failed - Firebase will use reCAPTCHA fallback")
        #if targetEnvironment(simulator)
        print("AppDelegate: Running on simulator - this is expected")
        #endif
    }

    // MARK: - Remote Notification Forwarding (CRITICAL for Phone Auth)

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

        // Handle other app notifications here
        print("AppDelegate: Notification not related to Firebase Auth")
        completionHandler(.noData)
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
