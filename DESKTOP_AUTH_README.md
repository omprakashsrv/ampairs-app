# Desktop Application Authentication

This document describes the browser-based authentication flow for the Ampairs desktop application, similar to Slack's authentication approach.

## Overview

The desktop application uses a secure browser-based authentication flow to avoid exposing sensitive phone authentication directly in the desktop app. After successful authentication, the web app passes authentication tokens to the desktop app via deep links.

## Architecture

```
Desktop App → Opens Browser → Web Authentication → Deep Link with Tokens → Desktop App
```

### Flow Diagram

```
┌─────────────────┐
│  Desktop App    │
│                 │
│  1. Click Login │
└────────┬────────┘
         │
         │ Opens browser to:
         │ https://app.ampairs.in/login
         │
         ▼
┌─────────────────────────┐
│  Web Browser            │
│  /login         │
│                         │
│  2. Enter Phone Number  │
│  3. Verify reCAPTCHA    │
│  4. Receive OTP (SMS)   │
│  5. Enter OTP           │
│                         │
│  Firebase Auth ✓        │
└────────┬────────────────┘
         │
         │ Exchange Firebase Token
         │ for Backend JWT Tokens
         │
         ▼
┌───────────────────────────────┐
│  Backend API                  │
│  POST /auth/v1/verify/firebase│
│                               │
│  Returns:                     │
│  - access_token               │
│  - refresh_token              │
└────────┬──────────────────────┘
         │
         │ Redirect with tokens
         │
         ▼
┌─────────────────────────────────────┐
│  Deep Link                          │
│  ampairs:/auth?                    │
│    access_token=xxx&                │
│    refresh_token=yyy                │
└────────┬────────────────────────────┘
         │
         │ Desktop app intercepts
         │ deep link
         │
         ▼
┌─────────────────┐
│  Desktop App    │
│                 │
│  6. Extract     │
│     tokens      │
│  7. Navigate to │
│     main screen │
└─────────────────┘
```

## Components

### 1. Web Application Components

#### `FirebaseAuthComponent`
- **Location**: `/src/app/auth/firebase-auth/firebase-auth.component.ts`
- **Route**: `/login`
- **Purpose**: Handles browser-based Firebase phone authentication for desktop and web apps
- **Features**:
  - Phone number input with country code selection
  - Firebase reCAPTCHA integration
  - OTP verification
  - Token exchange with backend
  - Deep link generation and redirect

#### `FirebaseAuthService`
- **Location**: `/src/app/core/services/firebase-auth.service.ts`
- **Purpose**: Manages Firebase Authentication
- **Methods**:
  - `initRecaptcha(containerId)`: Initialize reCAPTCHA verifier
  - `sendOTP(phoneNumber)`: Send OTP via Firebase
  - `verifyOTP(code)`: Verify OTP and get Firebase ID token
  - `resetRecaptcha()`: Reset for retry

#### `AuthService.authenticateWithFirebase()`
- **Location**: `/src/app/core/services/auth.service.ts`
- **Purpose**: Exchange Firebase token for backend JWT tokens
- **Endpoint**: `POST /auth/v1/verify/firebase`

### 2. Backend API Endpoint (To Be Implemented)

**Endpoint**: `POST /auth/v1/verify/firebase`

**Request Body**:
```json
{
  "firebase_id_token": "eyJhbGc...",
  "country_code": 91,
  "phone": "9876543210",
  "device_id": "DESKTOP_ABC123",
  "device_name": "MacBook Pro",
  "device_type": "DESKTOP",
  "platform": "macOS",
  "browser": "Electron",
  "os": "macOS 14.0",
  "user_agent": "Electron/28.0.0"
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

**Implementation Steps**:
1. Verify Firebase ID token with Firebase Admin SDK
2. Extract phone number from Firebase token
3. Match or create user in database
4. Generate JWT access and refresh tokens
5. Create device session
6. Return tokens

**Example Backend Implementation (Kotlin/Spring Boot)**:
```kotlin
@PostMapping("/verify/firebase")
fun authenticateWithFirebase(@RequestBody request: FirebaseAuthRequest): ApiResponse<AuthResponse> {
    // 1. Verify Firebase token
    val firebaseToken = FirebaseAuth.getInstance().verifyIdToken(request.firebase_id_token)
    val phoneNumber = firebaseToken.phoneNumber

    // 2. Find or create user
    val user = userService.findOrCreateByPhone(request.country_code, request.phone)

    // 3. Create device session
    val device = deviceService.createOrUpdateDevice(user, request)

    // 4. Generate JWT tokens
    val accessToken = jwtService.generateAccessToken(user, device)
    val refreshToken = jwtService.generateRefreshToken(user, device)

    // 5. Return tokens
    return ApiResponse.success(AuthResponse(
        access_token = accessToken,
        refresh_token = refreshToken,
        access_token_expires_at = Instant.now().plus(1, ChronoUnit.HOURS),
        refresh_token_expires_at = Instant.now().plus(7, ChronoUnit.DAYS)
    ))
}
```

### 3. Desktop Application (To Be Implemented)

#### Deep Link Handler
The desktop app needs to register a custom URL scheme handler for `ampairs://`.

**Kotlin Multiplatform (Desktop)**:
```kotlin
// Register deep link handler on app startup
fun setupDeepLinkHandler() {
    DeepLinkHandler.register("ampairs") { url ->
        when (url.host) {
            "auth" -> handleAuthCallback(url)
            else -> println("Unknown deep link: $url")
        }
    }
}

// Handle authentication callback
fun handleAuthCallback(url: URL) {
    val params = url.query.split("&").associate {
        val (key, value) = it.split("=")
        key to URLDecoder.decode(value, "UTF-8")
    }

    val accessToken = params["access_token"]
    val refreshToken = params["refresh_token"]

    if (accessToken != null && refreshToken != null) {
        // Store tokens securely
        TokenStorage.saveAccessToken(accessToken)
        TokenStorage.saveRefreshToken(refreshToken)

        // Navigate to main screen
        navigator.navigate(Screen.WorkspaceSelection)
    }
}

// Trigger browser authentication
fun login() {
    val authUrl = "https://app.ampairs.in/desktop-auth"
    Desktop.getDesktop().browse(URI(authUrl))
}
```

## Configuration

### 1. Firebase Setup

**Step 1**: Create a Firebase project at https://console.firebase.google.com

**Step 2**: Enable Phone Authentication:
- Go to Authentication → Sign-in method
- Enable "Phone" provider
- Add authorized domains (localhost, app.ampairs.in)

**Step 3**: Get Firebase configuration:
- Go to Project Settings → General
- Under "Your apps", add a web app
- Copy the Firebase config

**Step 4**: Update environment files:

`src/environments/environment.ts`:
```typescript
firebase: {
  apiKey: 'AIzaSyD...',
  authDomain: 'ampairs-dev.firebaseapp.com',
  projectId: 'ampairs-dev',
  storageBucket: 'ampairs-dev.appspot.com',
  messagingSenderId: '123456789',
  appId: '1:123456789:web:abcdef'
}
```

### 2. Deep Link Configuration

**Web App** (`environment.ts`):
```typescript
deepLink: {
  scheme: 'ampairs',
  host: 'auth'
}
```

**Desktop App** (register URL scheme):
- **macOS**: Update `Info.plist` with URL scheme
- **Windows**: Register protocol in installer
- **Linux**: Create `.desktop` file with URL handler

## Security Considerations

### 1. Token Security
- Tokens are passed via deep link (single use, immediate consumption)
- Desktop app must store tokens in secure storage (Keychain/Credential Manager)
- Never log tokens in console or analytics

### 2. Deep Link Validation
- Desktop app should validate token format before storing
- Implement token expiry checks
- Clear any pending deep link data after processing

### 3. reCAPTCHA
- Web app uses Firebase reCAPTCHA for bot protection
- Required for production deployments
- Test mode available for development

### 4. Backend Validation
- Backend MUST verify Firebase ID token authenticity
- Use Firebase Admin SDK for verification
- Validate phone number matches token claims

## Testing

### Local Development

1. **Start Web App**:
```bash
cd ampairs-web
npm start
```

2. **Navigate to Firebase Auth**:
```
http://localhost:4200/login
```

3. **Test Flow**:
   - Enter phone number
   - Verify reCAPTCHA
   - Enter OTP
   - Check deep link generation in browser console

### Production Testing

1. Deploy web app to production domain
2. Configure Firebase with production domain
3. Update deep link scheme in desktop app
4. Test end-to-end flow

## Usage

### From Desktop Application

```kotlin
// User clicks "Login" button
fun onLoginClick() {
    // Open browser to web authentication page
    val authUrl = "https://app.ampairs.in/firebase-auth"
    openBrowser(authUrl)

    // Desktop app waits for deep link callback
    // Deep link will be received via OS URL handler
}
```

### Deep Link URL Format

```
ampairs://auth?access_token=<JWT_ACCESS_TOKEN>&refresh_token=<JWT_REFRESH_TOKEN>
```

**Example**:
```
ampairs://auth?access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...&refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Troubleshooting

### Common Issues

#### 1. reCAPTCHA Not Loading
- Check Firebase configuration in environment files
- Verify domain is authorized in Firebase Console
- Check browser console for errors

#### 2. Deep Link Not Working
- Verify URL scheme is registered in desktop app
- Check OS permissions for URL handlers
- Test deep link manually in browser

#### 3. Token Exchange Fails
- Verify backend endpoint `/auth/v1/verify/firebase` is implemented
- Check Firebase token is valid
- Verify device information is being sent correctly

#### 4. OTP Not Received
- Check phone number format (include country code)
- Verify Firebase phone authentication is enabled
- Check Firebase quota limits

## Future Enhancements

1. **QR Code Authentication**: Generate QR code for quick mobile-to-desktop auth
2. **Biometric Support**: Add fingerprint/face recognition on desktop
3. **Remember Device**: Option to skip auth on trusted devices
4. **Multi-Account**: Support multiple account switching
5. **OAuth Providers**: Add Google/Apple sign-in options

## References

- [Firebase Phone Authentication](https://firebase.google.com/docs/auth/web/phone-auth)
- [Deep Linking Best Practices](https://developer.android.com/training/app-links)
- [JWT Token Security](https://jwt.io/introduction)
- [Electron Deep Links](https://www.electronjs.org/docs/latest/tutorial/launch-app-from-url-in-another-app)

## Support

For issues or questions:
- Open an issue on GitHub
- Contact: dev@ampairs.com
- Documentation: https://docs.ampairs.in
