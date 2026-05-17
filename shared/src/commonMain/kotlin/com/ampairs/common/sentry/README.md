# Sentry Error Tracking Integration

This directory contains the Sentry error tracking integration for the Ampairs Kotlin Multiplatform app.

## Overview

Sentry is integrated across all platforms (Android, iOS, Desktop) using the official Sentry Kotlin Multiplatform SDK. This implementation follows the [official Sentry Compose Multiplatform documentation](https://docs.sentry.io/platforms/kotlin/guides/compose-multiplatform/).

## Setup

### 1. Environment Variable

Set the `SENTRY_AUTH_TOKEN` environment variable for source code upload:

```bash
export SENTRY_AUTH_TOKEN="your_token_here"
```

Generate a token at: https://sentry.io/settings/ampairs/auth-tokens/

### 2. DSN Configuration

The DSN is configured in the app initialization:
- **DSN**: `https://e11636e1c2d82a2c62621f392ddfc784@o4510332999106560.ingest.de.sentry.io/4510333000810576`
- **Organization**: `ampairs`
- **Project**: `kotlin`

## Platform Support

All platforms use the same shared Sentry KMP SDK with automatic platform-specific implementations:

### Android
- Sentry KMP with Android-specific features
- Automatic session tracking
- Activity lifecycle tracking
- User interaction tracking
- Initialized in `MainApp.onCreate()` before Koin

### Desktop (JVM)
- Sentry KMP with JVM-specific features
- Uncaught exception handling
- Initialized in `main()` before Koin

### iOS
- Sentry KMP with Cocoa SDK integration
- Cocoa SDK version installed via Cocoapods (`pod "Sentry"`)
- Automatic iOS-specific features
- Initialized from shared `SentryManager.initialize()` call

## Usage

### Basic Error Tracking

```kotlin
import com.ampairs.common.sentry.ErrorTracking

// Capture an exception
try {
    // Your code
} catch (e: Exception) {
    ErrorTracking.captureException(e, "feature_name")
}

// Capture a message
ErrorTracking.captureMessage("Something went wrong", SentryLevel.ERROR)
```

### Extension Functions

```kotlin
import com.ampairs.common.sentry.reportToSentry
import com.ampairs.common.sentry.reportFailureToSentry

// Report exception
try {
    // Your code
} catch (e: Exception) {
    e.reportToSentry("feature_name")
}

// Report Result failures
suspend fun fetchData(): Result<Data> {
    return repository.getData()
        .reportFailureToSentry("data_fetch")
}
```

### Safe Execution Wrapper

```kotlin
import com.ampairs.common.sentry.trySentry

// Automatically catch and report exceptions
val result = trySentry(tag = "feature_name") {
    // Your code that might throw
    performRiskyOperation()
}
```

### User Context

```kotlin
// Set user information
ErrorTracking.setUser(
    userId = "123",
    username = "john.doe",
    email = "john@example.com"
)

// Clear user information (e.g., on logout)
ErrorTracking.clearUser()
```

### Breadcrumbs

```kotlin
// Add breadcrumbs for debugging context
ErrorTracking.addBreadcrumb("User clicked submit button", "ui")
ErrorTracking.logInfo("API request started", "network")
ErrorTracking.logDebug("Processing data", "data")
```

## Architecture

### Files

- `SentryManager.kt` - Shared singleton object using Sentry KMP SDK in `commonMain`
- `SentryModule.kt` - Koin module (placeholder for future Sentry-related dependencies)
- `ErrorTracking.kt` - Convenient utility wrapper with safe error handling
- `README.md` - This documentation

### Initialization Flow

1. App entry point (Android: `MainApp.onCreate()`, Desktop: `main()`) calls `SentryManager.initialize()`
2. Sentry KMP SDK is configured with DSN, environment, and debug settings
3. Gradle plugin auto-installs platform-specific dependencies (Android, iOS Cocoa, Desktop JVM)
4. `SentryManager` and `ErrorTracking` objects are available globally across all platforms
5. Koin is initialized after Sentry to ensure errors during Koin setup are captured

## Best Practices

1. **Tag Errors**: Always provide a descriptive tag for categorization
2. **User Context**: Set user information after authentication
3. **Breadcrumbs**: Add breadcrumbs for important user actions
4. **Silent Failures**: Error tracking should never crash the app
5. **Privacy**: Don't log sensitive user data (passwords, tokens, etc.)

## Environment Configuration

- **Development**: `enableDebug = true`, environment = "dev"
- **Production**: `enableDebug = false`, environment = "production"

## Testing

To test Sentry integration:

```kotlin
// Trigger a test exception
ErrorTracking.captureException(
    Exception("This is a test."),
    "test"
)

// Verify in Sentry dashboard:
// https://sentry.io/organizations/ampairs/issues/
```

## iOS Implementation Notes

iOS support is fully enabled through the Sentry KMP SDK:

1. **Cocoa SDK**: Automatically installed via Cocoapods (`pod "Sentry"` in `build.gradle.kts`)
2. **Version Compatibility**: Verify Cocoa SDK version compatibility using the [official table](https://github.com/getsentry/sentry-kotlin-multiplatform?tab=readme-ov-file#cocoa-sdk-version-compatibility-table)
3. **Shared Code**: Same `SentryManager.initialize()` call works on iOS
4. **Platform Features**: iOS-specific features are automatically enabled by the Cocoa SDK

## Troubleshooting

### Source Context Not Appearing

- Verify `SENTRY_AUTH_TOKEN` is set
- Check `includeSourceContext = true` in `build.gradle.kts`
- Ensure gradle plugin version is up to date

### Errors Not Appearing in Sentry

- Check DSN is correct
- Verify network connectivity
- Check debug logs for initialization errors
- Ensure Sentry is initialized before error occurs

### Build Failures

- Verify all platform dependencies are correctly configured
- Check that Cocoapods is set up correctly for iOS
- Ensure Gradle sync completed successfully

## References

- [Sentry Kotlin Documentation](https://docs.sentry.io/platforms/kotlin-multiplatform/)
- [Sentry Android Documentation](https://docs.sentry.io/platforms/android/)
- [Sentry Dashboard](https://sentry.io/organizations/ampairs/projects/kotlin/)
