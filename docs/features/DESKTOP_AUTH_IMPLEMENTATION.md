# Desktop Browser-Based Authentication Implementation

This document describes the implementation of secure browser-based authentication for the Ampairs desktop application.

## Overview

The desktop application uses a browser-based OAuth-like flow to authenticate users, similar to Slack, WhatsApp Desktop, and other desktop applications. This approach provides better security by avoiding direct phone number and OTP handling in the desktop app.

## Architecture

```
┌─────────────────┐
│  Desktop App    │
│  (Click Login)  │
└────────┬────────┘
         │
         │ Opens Browser
         ▼
┌─────────────────────────┐
│  Web Browser            │
│  app.ampairs.com/login  │
│  - Phone + OTP Auth     │
│  - Firebase Auth        │
└────────┬────────────────┘
         │
         │ Backend JWT Exchange
         ▼
┌─────────────────────────────────┐
│  Deep Link Redirect             │
│  ampairs://auth?access_token=xxx│
│         &refresh_token=yyy      │
└────────┬────────────────────────┘
         │
         │ OS Intercepts Deep Link
         ▼
┌─────────────────┐
│  Desktop App    │
│  - Store tokens │
│  - Navigate home│
└─────────────────┘
```

## Implementation Components

### 1. Deep Link Handler (Desktop-specific)

**File**: `composeApp/src/desktopMain/kotlin/com/ampairs/auth/deeplink/DeepLinkHandler.desktop.kt`

**Features**:
- Registers `ampairs://` URL scheme handler using Java Desktop API
- Parses deep link URLs and extracts query parameters
- Emits events via SharedFlow for reactive handling
- Provides utility to open browser to authentication URL

**Key Methods**:
- `setupDeepLinkHandler()` - Initialize deep link listener (called in main.kt)
- `deepLinkEvents: SharedFlow<DeepLinkEvent>` - Observe authentication callbacks
- `openAuthenticationBrowser(url)` - Open system browser to auth page
- `processDeepLink(url)` - Manual deep link processing (fallback)

### 2. Desktop Browser Auth Screen

**File**: `composeApp/src/desktopMain/kotlin/com/ampairs/auth/ui/DesktopBrowserAuthScreen.kt`

**Features**:
- Clean Material 3 UI with app branding
- "Sign in with Browser" button that opens web authentication
- Waiting state with progress indicator
- Error handling with user-friendly messages
- Educational information about the authentication flow

**Flow**:
1. User clicks "Sign in with Browser"
2. Screen shows "Waiting for authentication..." with spinner
3. Browser opens to web auth page
4. User completes phone authentication in browser
5. Desktop app receives deep link and automatically continues
6. Tokens stored and user navigated to workspace selection

### 3. LoginViewModel Extension

**File**: `composeApp/src/commonMain/kotlin/com/ampairs/auth/viewmodel/LoginViewModel.kt`

**New Method**: `handleBrowserAuthTokens()`

**Features**:
- Accepts access and refresh tokens from deep link
- Creates dummy user session for token operations
- Fetches user information from backend API
- Saves user to local database
- Updates token storage with real user session
- Logs analytics events (sign_up or login with method: "desktop_browser")
- Handles errors gracefully with user-friendly messages

### 4. Navigation Updates

**Files**:
- `composeApp/src/commonMain/kotlin/Routes.kt` - Added `AuthRoute.DesktopBrowserAuth`
- `composeApp/src/commonMain/kotlin/com/ampairs/auth/Navigation.kt` - Added desktop auth route handling

**Features**:
- New `AuthRoute.DesktopBrowserAuth` route for desktop authentication screen
- Platform-specific route selection (`getAuthRouteForPlatform()`)
- Desktop uses `DesktopBrowserAuth`, mobile uses `Phone`
- Expect/actual pattern for `DesktopBrowserAuthScreen` composable
- Workspace status check after successful authentication

### 5. Platform Implementations

**Desktop** (`DesktopBrowserAuthScreen.desktop.kt`):
- Full implementation with deep link handling

**Android** (`DesktopBrowserAuthScreen.android.kt`):
- Stub implementation (not available on mobile)

**iOS** (`DesktopBrowserAuthScreen.ios.kt`):
- Stub implementation (not available on mobile)

## Deep Link URL Format

```
ampairs://auth?access_token=<JWT_ACCESS_TOKEN>&refresh_token=<JWT_REFRESH_TOKEN>
```

**Example**:
```
ampairs://auth?access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...&refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Configuration Requirements

### 1. Operating System Deep Link Registration

For the desktop app to receive deep links, the `ampairs://` URL scheme must be registered with the operating system:

#### macOS

Create/update `Info.plist` in the application bundle:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string>com.ampairs.app</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>ampairs</string>
        </array>
    </dict>
</array>
```

#### Windows

Register protocol in installer script or Windows Registry:

```
HKEY_CURRENT_USER\Software\Classes\ampairs
    (Default) = "URL:Ampairs Protocol"
    URL Protocol = ""
    \shell\open\command
        (Default) = "C:\Path\To\Ampairs.exe" "%1"
```

#### Linux

Create `.desktop` file with URL handler:

```desktop
[Desktop Entry]
Name=Ampairs
Exec=/usr/bin/ampairs %u
Type=Application
MimeType=x-scheme-handler/ampairs;
```

Register the handler:
```bash
xdg-mime default ampairs.desktop x-scheme-handler/ampairs
```

### 2. Web Application Configuration

The web application must redirect to the deep link after successful authentication:

**Angular Example** (already implemented per web README):
```typescript
// After Firebase auth and backend JWT exchange
const deepLink = `ampairs://auth?access_token=${accessToken}&refresh_token=${refreshToken}`;
window.location.href = deepLink;
```

### 3. Backend API Endpoint

The backend needs to support the Firebase token exchange endpoint:

**Endpoint**: `POST /auth/v1/firebase`

**Request**:
```json
{
  "firebase_token": "eyJhbGc...",
  "country_code": 91,
  "phone": "9876543210",
  "device_id": "DESKTOP_ABC123",
  "device_name": "MacBook Pro",
  "device_type": "DESKTOP",
  "platform": "macOS"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGc...",
    "refresh_token": "eyJhbGc...",
    "access_token_expires_at": "2025-10-27T05:00:00Z",
    "refresh_token_expires_at": "2025-11-03T04:00:00Z"
  }
}
```

## Security Considerations

### 1. Token Security
- Tokens are passed via deep link (single use, immediate consumption)
- Desktop app stores tokens in Room database with proper security
- Tokens are never logged to console or analytics
- Deep link events are cleared after processing

### 2. Deep Link Validation
- URL scheme validation (`ampairs://` only)
- Host validation (`auth` only)
- Required parameters validation (access_token, refresh_token)
- URL decoding to handle special characters
- Error handling for malformed URLs

### 3. Browser Authentication
- Uses Firebase Phone Authentication with reCAPTCHA protection
- Backend validates Firebase ID token authenticity
- JWT tokens generated only after successful Firebase verification
- Device information tracked for multi-device support

### 4. User Experience
- Clear visual feedback during authentication process
- User-friendly error messages
- Educational information about the flow
- Cancel option while waiting for authentication
- Automatic navigation on success

## Testing

### Local Development

1. **Start the desktop application**:
```bash
cd ampairs-mp-app
./gradlew composeApp:run
```

2. **Test deep link handler manually**:
   - Open browser
   - Navigate to: `ampairs://auth?access_token=test_token&refresh_token=test_refresh`
   - Desktop app should receive the deep link (check console logs)

3. **Test with web authentication** (requires web app and backend):
   - Click "Sign in with Browser" in desktop app
   - Complete authentication in web browser
   - Desktop app should automatically complete login

### Production Testing

1. Build desktop application with URL scheme registration
2. Install on test machine
3. Complete end-to-end authentication flow
4. Verify tokens are stored correctly
5. Verify user can access workspace selection and modules

## Troubleshooting

### Issue: Deep link not received by desktop app

**Possible Causes**:
- URL scheme not registered with OS
- Another application handling the scheme
- Desktop API not supported on platform

**Solution**:
- Check URL scheme registration in OS
- Use `processDeepLink()` method as fallback
- Check console logs for initialization errors

### Issue: Browser doesn't open

**Possible Causes**:
- Desktop.browse() not supported
- Default browser not configured

**Solution**:
- Check if Desktop API is supported
- Manually copy URL and open in browser
- Configure default browser in OS settings

### Issue: Authentication completes but tokens not stored

**Possible Causes**:
- Network error fetching user info
- Database write failure
- Token repository error

**Solution**:
- Check console logs for errors
- Verify backend API is accessible
- Check database permissions

### Issue: "Waiting for authentication" doesn't update

**Possible Causes**:
- Web page didn't redirect to deep link
- Deep link handler not initialized
- Flow not collecting deep link events

**Solution**:
- Check web app redirects to `ampairs://auth` URL
- Verify `setupDeepLinkHandler()` called in main.kt
- Check `LaunchedEffect` is collecting `deepLinkEvents` flow

## Implementation Checklist

- [x] Create DeepLinkHandler with SharedFlow events
- [x] Implement DesktopBrowserAuthScreen with Material 3 UI
- [x] Add handleBrowserAuthTokens() to LoginViewModel
- [x] Add AuthRoute.DesktopBrowserAuth to routes
- [x] Update auth navigation with desktop route
- [x] Create expect/actual implementations for all platforms
- [x] Initialize deep link handler in main.kt
- [x] Add proper error handling and user feedback
- [ ] Register URL scheme in desktop package (platform-specific)
- [ ] Update web app to redirect to deep link (see web README)
- [ ] Implement backend `/auth/v1/firebase` endpoint (see web README)
- [ ] Test end-to-end flow with actual tokens
- [ ] Add integration tests for deep link handling
- [ ] Document URL scheme registration in build scripts

## Future Enhancements

1. **QR Code Authentication**: Generate QR code for quick mobile-to-desktop auth
2. **Biometric Support**: Add fingerprint/face recognition on desktop
3. **Remember Device**: Option to skip auth on trusted devices
4. **Token Refresh**: Automatic background token refresh
5. **Multi-Account**: Support quick switching between multiple accounts
6. **Session Management**: View and revoke active sessions
7. **OAuth Providers**: Add Google/Apple/Microsoft sign-in options

## References

- [Firebase Phone Authentication](https://firebase.google.com/docs/auth/web/phone-auth)
- [Java Desktop API](https://docs.oracle.com/javase/8/docs/api/java/awt/Desktop.html)
- [Deep Linking Best Practices](https://developer.android.com/training/app-links)
- [JWT Token Security](https://jwt.io/introduction)
- [Kotlin Multiplatform Expect/Actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html)

## Support

For issues or questions:
- Check console logs for detailed error messages
- Review this document's troubleshooting section
- Open an issue on GitHub with reproduction steps
- Contact: dev@ampairs.com
