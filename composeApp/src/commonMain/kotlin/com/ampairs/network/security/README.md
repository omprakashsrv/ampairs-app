# Certificate Pinning Implementation

This document explains the certificate pinning implementation for both Android and Desktop applications.

## Overview

The certificate pinning implementation protects against Man-in-the-Middle (MITM) attacks by validating SSL certificates against known public key hashes. The system also enforces app updates when certificates expire (1-month validity period as specified).

## Architecture

### Core Components

1. **CertificatePinningService**: Interface for certificate validation and management
2. **CertificateManager**: Handles certificate storage and expiration logic
3. **AppUpdateEnforcer**: Manages app update dialogs and network request blocking
4. **SecureKtorClientFactory**: Creates HTTP clients with certificate pinning enabled

### Platform-Specific Implementations

#### Android
- `AndroidCertificatePinningService`: Uses OkHttp CertificatePinner
- `AndroidCertificateStorage`: SharedPreferences-based storage
- `AndroidAppUpdateEnforcer`: Native Android dialogs with Play Store integration
- `AndroidSecureEngineFactory`: OkHttp engine with certificate pinning

#### Desktop
- `DesktopCertificatePinningService`: Uses OkHttp CertificatePinner
- `DesktopCertificateStorage`: File-based storage using Properties
- `DesktopAppUpdateEnforcer`: Swing-based dialogs with browser integration
- `DesktopSecureEngineFactory`: OkHttp engine with certificate pinning

## Usage

### 1. Initialize Certificate Pinning

Certificate pinning is automatically initialized through Koin dependency injection when the app starts.

```kotlin
// Certificate pinning is injected automatically
val certificatePinningService: CertificatePinningService = koinInject()
val appUpdateEnforcer: AppUpdateEnforcer = koinInject()
val secureClientFactory: SecureKtorClientFactory = koinInject()
```

### 2. Create Secure HTTP Client

```kotlin
// Android
val androidEngineFactory: AndroidSecureEngineFactory = koinInject()
val engine = androidEngineFactory.createEngine()
val httpClient = secureClientFactory.createSecureHttpClient(engine, tokenRepository)

// Desktop
val desktopEngineFactory: DesktopSecureEngineFactory = koinInject()
val engine = desktopEngineFactory.createEngine()
val httpClient = secureClientFactory.createSecureHttpClient(engine, tokenRepository)
```

### 3. Using the Secure Client

```kotlin
// The secure client automatically:
// - Validates certificates against pinned hashes
// - Checks for certificate expiration
// - Shows update dialogs when needed
// - Blocks network requests if updates are required

httpClient.get("https://your-backend-url/api/endpoint")
```

## Certificate Management

### Default Configuration

- **Backend Hostname**: `localhost:8080` (configurable in CertificateManager)
- **Certificate Validity**: 1 month (as specified in requirements)
- **Default Pins**: Placeholder hashes (must be replaced with actual certificate pins)

### Updating Certificate Pins

1. **For Development**: Update `DEFAULT_CERTIFICATE_PINS` in `CertificateManager.kt`
2. **For Production**: Implement secure certificate update mechanism (fetch from secure endpoint)

```kotlin
// Example: Update certificates programmatically
val certificateManager: CertificateManager = koinInject()
val newPins = listOf("NEW_SHA256_HASH_1", "NEW_SHA256_HASH_2")
val newExpirationDate = LocalDateTime.now().plusMonths(1)
certificateManager.updateCertificates(newPins, newExpirationDate)
```

### Extracting Certificate Pins

For development/testing, you can extract certificate pins from certificate files:

```kotlin
// Desktop only
val desktopService = certificatePinningService as DesktopCertificatePinningService
val pin = desktopService.extractCertificatePin(File("path/to/certificate.crt"))
```

## App Update Enforcement

### Update Scenarios

1. **Required Update**: Certificates are expired
   - Shows blocking dialog
   - Prevents app usage until updated
   - Redirects to app store/download page

2. **Recommended Update**: Certificates expire in ≤3 days
   - Shows optional dialog
   - Allows continued usage
   - Suggests updating for security

3. **No Update Required**: Certificates are valid for >3 days
   - Normal operation continues

### Platform-Specific Update Flows

#### Android
- Opens Google Play Store app
- Falls back to Play Store website if app unavailable
- Force closes app if update is required and user declines

#### Desktop
- Opens default browser to GitHub releases page (configurable)
- Exits application if update is required and user declines

## Security Features

### MITM Attack Protection
- Certificate public key pinning using SHA-256 hashes
- Backup certificate pin support for certificate rotation
- Network request blocking when certificates are invalid

### Automatic Certificate Rotation
- Checks certificate expiration on app startup
- Monitors certificate status during app usage
- Automatic update prompts based on expiration timeline

### Network Request Security
- Validates certificate status before making requests
- Blocks network requests when updates are required
- Secure token refresh with certificate validation

## Configuration

### Environment Variables

Update these values in `CertificateManager.kt` for your environment:

```kotlin
companion object {
    const val BACKEND_HOSTNAME = "your-backend-hostname"
    
    val DEFAULT_CERTIFICATE_PINS = listOf(
        "YOUR_PRIMARY_CERTIFICATE_PIN",   // Replace with actual SHA-256 hash
        "YOUR_BACKUP_CERTIFICATE_PIN"     // Replace with backup certificate hash
    )
}
```

### Update URLs

Update the download URLs for your application:

#### Android (`AndroidAppUpdateEnforcer.kt`)
```kotlin
// Update package name
data = Uri.parse("market://details?id=com.yourcompany.yourapp")
```

#### Desktop (`DesktopAppUpdateEnforcer.kt`)
```kotlin
// Update download URL
Desktop.getDesktop().browse(URI("https://your-website.com/download"))
```

## Testing

### Development Testing
1. Use test certificates with short validity periods
2. Test certificate expiration scenarios
3. Verify update dialogs appear correctly

### Certificate Pin Generation
```bash
# Extract certificate from server
openssl s_client -connect hostname:443 -servername hostname < /dev/null | openssl x509 -outform PEM > cert.pem

# Generate SHA-256 hash of public key
openssl x509 -in cert.pem -pubkey -noout | openssl pkey -pubin -outform DER | openssl dgst -sha256 -binary | openssl enc -base64
```

## Troubleshooting

### Common Issues

1. **Certificate Pin Mismatch**: Update pins with current certificate hashes
2. **Update Dialog Not Showing**: Check certificate expiration dates
3. **Network Requests Blocked**: Verify certificate status and update enforcement logic

### Debug Logging

Certificate pinning includes debug logging:
- Certificate validation results
- Update status checks
- Network request blocking events

Monitor logs with tag: `SecureHttpClient`

## Production Deployment

### Pre-Deployment Checklist

1. ✅ Replace default certificate pins with production pins
2. ✅ Update backend hostname configuration
3. ✅ Configure update URLs for production
4. ✅ Test certificate validation with production certificates
5. ✅ Verify update enforcement works correctly
6. ✅ Test both Android and Desktop platforms

### Certificate Rotation Process

1. Generate new certificates with 1-month validity
2. Extract SHA-256 pins from new certificates
3. Update application with new pins
4. Deploy updated application before old certificates expire
5. Monitor certificate expiration and plan next rotation