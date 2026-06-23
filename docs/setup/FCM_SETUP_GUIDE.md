# FCM Push Notifications — Setup & Operations Guide

This guide reflects the **actual** implementation (Metro DI, multi-module KMP). It replaces an earlier
draft that referenced Koin / `composeApp` (both removed long ago).

> **Architecture rule:** the **backend is the only trigger**; the app is a **pure consumer** — it
> registers its FCM token, pulls the notification feed, and renders. The app never originates a push.

---

## 1. What is implemented

### Backend (`ampairs`, `notification` module)
- **`FcmPushProvider`** — sends to a device token (`sendPush`) or an FCM topic (`sendToTopic`) via the
  Firebase Admin SDK. Active only when `notification.push.enabled=true` and credentials are supplied
  (`FirebaseConfig`, `@ConditionalOnProperty`). When disabled, push rows fail gracefully and retry.
- **`PushDispatchService.dispatch(...)`** — the single trigger entry point: persists a
  `NotificationLog` (in-app center) and fans the push out to resolved device tokens (workspace-wide
  or a single user). Token resolution/pruning via `DevicePushTokenAdapter` (subscription module).
- **In-app feed** — canonical offline-sync resource `GET/POST /notification/v1/notifications/feed/sync`
  (+ `/{uid}/read`, `/read-all`).
- **Trigger endpoints:** workspace member-joined → push; `POST .../announce` (per-workspace, push +
  in-app entry); `POST .../announce/global` (SUPER_ADMIN, FCM `announcements` topic, push-only).
- **Extension point for order/invoice (deliberate follow-up):** add a server-side listener that calls
  `PushDispatchService.dispatch(..., type = "order_update" | "invoice_update", ...)`. Aggregate per
  sync batch — a device's first sync can bulk-upsert hundreds of rows, so one-push-per-row would storm.

### App (`ampairs-app`)
- **`FirebaseMessaging`** expect/actual (`shared/.../firebase/messaging/`) — Android & iOS real,
  Desktop stub.
- **Android:** `androidApp/.../AmpairsFirebaseMessagingService` forwards `onNewToken` /
  `onMessageReceived` to the shared singleton and shows a foreground notification. The manifest
  declares `POST_NOTIFICATIONS`, the service with the `com.google.firebase.MESSAGING_EVENT` filter,
  and the default notification-channel meta-data; `MainApp` creates the channel.
- **iOS:** `iosApp/iosApp/AppDelegate.swift` registers `UNUserNotificationCenter` + Messaging
  delegates, requests authorization, and forwards token/notifications into Kotlin via
  `shared/.../FcmBridge.kt` (populated by `MainViewController`). Existing Firebase Auth APNs handling
  is preserved.
- **`shared/.../push/PushTokenRegistrar`** (WorkspaceScope) — registers the FCM token with the backend
  on workspace activation + on refresh; subscribes/unsubscribes the `announcements` topic per
  preference; gates foreground display on the master + per-type toggles.
- **`feature/notification`** — Room store + `NotificationSyncDelegate` (`/feed/sync`), list screen,
  unread bell badge, and settings (permission request + device-local preferences).

---

## 2. Remaining deployment configuration (not code)

### Backend
Set in the deploy environment (never commit the JSON — see `10-security`):
```
notification.push.enabled=true
# provide ONE of:
notification.push.credentialsJson=<service-account JSON>     # e.g. from a FCM_SERVICE_ACCOUNT_JSON secret
notification.push.credentialsPath=/run/secrets/fcm-sa.json
```
Service account: Firebase console → Project settings → Service accounts → Generate new private key.

### Android
- Ensure `androidApp/google-services.json` is present (already required by the existing Firebase
  Auth / Crashlytics setup) and matches the FCM project.
- `POST_NOTIFICATIONS` is requested at runtime from the notification settings screen (API 33+).

### iOS
- `GoogleService-Info.plist` present in the Xcode project.
- Upload the **APNs authentication key** (.p8) to Firebase console → Cloud Messaging.
- In Xcode (`iosApp` target) enable capabilities: **Push Notifications** and **Background Modes →
  Remote notifications**.

---

## 3. Smoke test (after config)

```bash
# 1. App logs in → PushTokenRegistrar registers the token (verify DeviceRegistration.pushToken is set).

# 2. Direct test push to a known token:
curl -X POST "$BASE/notification/v1/notifications/send/immediate?recipient=$FCM_TOKEN&message=Hello&channel=PUSH_NOTIFICATION" \
  -H "Authorization: Bearer $JWT" -H "X-Workspace-ID: $WS"

# 3. Per-workspace announcement (push + in-app center entry):
curl -X POST "$BASE/notification/v1/notifications/announce?title=Heads%20up&message=Maintenance%20tonight" \
  -H "Authorization: Bearer $JWT" -H "X-Workspace-ID: $WS"

# 4. Global topic announcement (SUPER_ADMIN; all installs subscribed to "announcements"):
curl -X POST "$BASE/notification/v1/notifications/announce/global?title=Release&message=v1.1%20is%20live" \
  -H "Authorization: Bearer $SUPER_ADMIN_JWT"
```
Expected: notification appears on device; the in-app center (bell) shows the entry for workspace
sends; marking read round-trips via `/feed/sync`.

---

## 4. Key files

| Concern | Backend (`ampairs`) | App (`ampairs-app`) |
|---|---|---|
| Send | `notification/.../provider/push/FcmPushProvider.kt` | `shared/.../firebase/messaging/FirebaseMessaging.*.kt` |
| Platform receive / config | `notification/.../config/FirebaseConfig.kt` | `androidApp/.../AmpairsFirebaseMessagingService.kt`, `iosApp/iosApp/AppDelegate.swift`, `shared/.../FcmBridge.kt` |
| Dispatch / triggers | `notification/.../service/PushDispatchService.kt`, `controller/NotificationController.kt` | `shared/.../push/PushTokenRegistrar.kt` |
| Token | `subscription/.../controller/DeviceController.kt`, `domain/service/DevicePushTokenAdapter.kt` | `feature/subscription/.../api/SubscriptionApiImpl.kt` (`updatePushToken`) |
| In-app center | `notification/.../service/NotificationLogService.kt` (`/feed/sync`) | `feature/notification/` (Room + `NotificationSyncDelegate` + UI) |
