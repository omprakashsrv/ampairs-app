# feature:auth

Authentication and user session management. Handles phone + OTP login, multi-device support, JWT token storage/refresh, Firebase Auth integration, and account lifecycle (deletion, restoration, device management).

## Responsibilities

- Phone number entry and OTP verification flow
- Firebase Auth bridge for Android (reCAPTCHA, SMS verification)
- JWT access/refresh token storage in Room database
- Multi-device login with device management UI
- User profile creation and update
- Account deletion and restoration workflows

## Architecture

```
UI (LoginScreen / OtpScreen / PhoneScreen)
    ↓
ViewModel (LoginViewModel)
    ↓
FirebaseAuthRepository → FirebaseAuthProvider (platform)
AuthApi → Backend OTP verification
    ↓
TokenRepository (Room) + UserWorkspaceRepository (Room)
```

## Key Classes

| Class | Purpose |
|---|---|
| `AuthApi` / `AuthApiImpl` | Backend auth REST endpoints |
| `FirebaseAuthRepository` | Bridges Firebase phone auth to backend |
| `FirebaseAuthProvider` | Platform expect/actual for Firebase SDK |
| `TokenRepository` / `TokenRepositoryImpl` | JWT token CRUD in Room |
| `UserWorkspaceRepository` | Persists logged-in user and workspace mapping |
| `LoginViewModel` | Orchestrates phone → OTP → token flow |
| `DeviceManagementViewModel` | Lists and revokes linked devices |
| `AccountDeletionViewModel` | Soft-delete account with confirmation |
| `RecaptchaService` | Android reCAPTCHA integration |

## Koin Module

```kotlin
authModule  // in com.ampairs.auth
```

## Platform-Specific

| Platform | Notes |
|---|---|
| Android | Firebase Auth SDK, reCAPTCHA, Google Play Services |
| iOS | Firebase Auth via CocoaPods expect/actual |
| Desktop | Browser-based auth flow (`DesktopBrowserAuthScreen`) |

## Database

`AuthRoomDatabase` — uses `single` scope (not workspace-scoped, exists before workspace selection).

Contains: `UserEntity`, `UserSessionEntity`, `UserTokenEntity`
