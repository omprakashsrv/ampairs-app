//
// Storefront ecom app (multi-store marketplace) — iOS AppDelegate.
//
// Configures Firebase before the Compose UI starts and forwards the silent-push notifications /
// reCAPTCHA callback URLs that Firebase Phone Auth needs (the shared framework disables method
// swizzling). Trimmed vs the main app's AppDelegate — the storefront app does not wire FCM push.
//

import Foundation
import UIKit
import FirebaseCore
import FirebaseAuth

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        // Initialize Firebase FIRST (before the Compose graph is created).
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }

        // Register for remote notifications so Firebase Phone Auth can use silent-push verification
        // (falls back to reCAPTCHA on the Simulator / when APNs is unavailable).
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }

        return true
    }

    // MARK: - APNs Token (silent-push verification for Phone Auth)

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // Expected on the Simulator — Phone Auth falls back to reCAPTCHA.
    }

    // MARK: - Remote Notification Forwarding (required for Phone Auth silent push)

    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable : Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.noData)
    }

    // MARK: - URL Forwarding (reCAPTCHA OAuth callback for Phone Auth)

    func application(_ app: UIApplication,
                     open url: URL,
                     options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        if Auth.auth().canHandle(url) {
            return true
        }
        return false
    }
}
