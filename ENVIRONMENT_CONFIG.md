# Environment Configuration Guide

This guide explains how to configure API base URLs for different environments and platforms in the Ampairs KMP app.

## Quick Configuration

### 1. Update Your IP Address

Edit `/composeApp/src/commonMain/kotlin/com/ampairs/common/config/AppConfig.kt`:

```kotlin
companion object {
    /**
     * Get IP address for mobile platforms (Android/iOS)
     * This should be your development machine's IP address
     */
    const val DEV_MOBILE_IP = "192.168.1.100" // ← Change this to your actual IP
}
```

### 2. Android Build Configuration

Edit `/composeApp/build.gradle.kts` build types:

```kotlin
buildTypes {
    val debug by getting {
        buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8080\"") // ← Your IP here
        buildConfigField("String", "ENVIRONMENT", "\"dev\"")
        signingConfig = signingConfigs["release"]
    }
    val release by getting {
        buildConfigField("String", "API_BASE_URL", "\"https://api.ampairs.com\"") // ← Production URL
        buildConfigField("String", "ENVIRONMENT", "\"production\"")
        // ... rest of config
    }
}
```

## Platform-Specific Configuration

### Android
- **Dev Environment**: Uses `BuildConfig.API_BASE_URL` from gradle build configuration
- **Configuration**: Set in `build.gradle.kts` build types
- **Default Dev URL**: `http://192.168.1.100:8080`
- **Default Prod URL**: `https://api.ampairs.com`

### Desktop (JVM)
- **Dev Environment**: Uses `http://localhost:8080` by default
- **Configuration**: System properties or environment variables
- **System Property**: `-Dampairs.api.baseUrl=http://localhost:8080`
- **Environment Variable**: `AMPAIRS_API_BASE_URL=http://localhost:8080`

### iOS
- **Dev Environment**: Uses mobile IP address like Android
- **Configuration**: Info.plist or runtime defaults
- **Default Dev URL**: `http://192.168.1.100:8080`
- **Default Prod URL**: `https://api.ampairs.com`

## Runtime Configuration

### Desktop Command Line Examples

```bash
# Dev with custom URL
./gradlew composeApp:run -Dampairs.api.baseUrl=http://192.168.1.50:8080

# Dev with environment variable
AMPAIRS_API_BASE_URL=http://192.168.1.50:8080 ./gradlew composeApp:run

# Production mode
./gradlew composeApp:run -Dampairs.environment=production
```

### Android Build Variants

```bash
# Debug build (uses dev configuration)
./gradlew composeApp:assembleDebug

# Release build (uses production configuration)
./gradlew composeApp:assembleRelease
```

## Configuration Flow

1. **Platform Detection**: App detects current platform (Android/iOS/Desktop)
2. **Config Loading**: Each platform loads configuration differently:
   - Android: From BuildConfig (generated from gradle)
   - Desktop: From system properties/environment variables
   - iOS: From Info.plist/defaults
3. **URL Building**: All API calls use `ApiUrlBuilder` which references `ConfigurationManager`
4. **Runtime Access**: Configuration available through `ConfigurationManager.current`

## Advanced Configuration

### Custom Configuration at Runtime

```kotlin
// Update configuration programmatically
ConfigurationManager.updateConfig(
    AppConfig(
        apiBaseUrl = "http://192.168.1.50:8080",
        environment = Environment.DEV
    )
)

// Reset to platform defaults
ConfigurationManager.resetToDefault()

// Access current config
val config = ConfigurationManager.current
println("Current API URL: ${config.apiBaseUrl}")
```

### Environment Variables (Desktop)

```bash
# Set environment variables
export AMPAIRS_ENVIRONMENT=dev
export AMPAIRS_API_BASE_URL=http://192.168.1.100:8080

# Run application
./gradlew composeApp:run
```

### System Properties (Desktop)

```bash
# Using system properties
./gradlew composeApp:run \
  -Dampairs.environment=dev \
  -Dampairs.api.baseUrl=http://192.168.1.100:8080
```

## URL Structure

### API Endpoints
- **Auth**: `{baseUrl}/auth/v1/*`
- **User**: `{baseUrl}/user/v1/*`
- **Workspace**: `{baseUrl}/workspace/v1/*`
- **Customer**: `{baseUrl}/customer/v1/*`
- **Product**: `{baseUrl}/product/v1/*`
- **Order**: `{baseUrl}/order/v1/*`
- **Invoice**: `{baseUrl}/invoice/v1/*`
- **Inventory**: `{baseUrl}/inventory/v1/*`

### WebSocket URLs
- **Dev**: `ws://192.168.1.100:8080/ws/*`
- **Prod**: `wss://api.ampairs.com/ws/*`

## Debugging

### View Current Configuration
The app logs current configuration when HTTP client is initialized:

```
=== Ampairs Configuration ===
Environment: DEV
API Base URL: http://192.168.1.100:8080
WebSocket URL: ws://192.168.1.100:8080
Debug Mode: true
============================
```

### Common Issues

1. **"Connection refused"**: Update IP address in configuration
2. **"Network unreachable"**: Check if development server is running
3. **CORS errors**: Ensure backend CORS configuration allows your IP
4. **SSL errors in production**: Check HTTPS certificate configuration

## IP Address Discovery

### Find Your Development Machine IP

**Windows:**
```cmd
ipconfig | findstr "IPv4"
```

**macOS/Linux:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
# or
ip addr show | grep "inet " | grep -v 127.0.0.1
```

**Quick Test:**
```bash
# Test if your IP is accessible
curl http://YOUR_IP:8080/actuator/health
```

Update the `DEV_MOBILE_IP` constant in `AppConfig.kt` with your discovered IP address.