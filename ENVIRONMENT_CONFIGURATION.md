# Environment Configuration Guide

This document describes how to configure the Ampairs desktop application for different environments (development, staging, production).

## Overview

The desktop application uses environment-based configuration to determine:
- **API Base URL**: Backend API endpoint
- **Web Auth URL**: Web application URL for browser-based authentication
- **WebSocket URL**: Real-time communication endpoint

## Configuration Methods

Configuration values are resolved in the following priority order (highest to lowest):

1. **Direct parameter** (programmatic override)
2. **System properties** (JVM arguments with `-D` flag)
3. **Environment variables** (OS-level environment variables)
4. **Default values** (based on environment)

## Environment Types

### Development Environment (DEV)
- **API Base URL**: `http://localhost:8080`
- **Web Auth URL**: `http://localhost:4200/login`
- **WebSocket URL**: `ws://localhost:8080`
- **Debug Mode**: Enabled

### Production Environment (PRODUCTION)
- **API Base URL**: `https://api.ampairs.com`
- **Web Auth URL**: `https://app.ampairs.com/login`
- **WebSocket URL**: `wss://api.ampairs.com`
- **Debug Mode**: Disabled

## Configuration Options

### 1. Environment Selection

**Set the environment using any of these methods:**

#### System Property (Recommended for development)
```bash
./gradlew composeApp:run -Dampairs.environment=dev
./gradlew composeApp:run -Dampairs.environment=production
```

#### Environment Variable
```bash
export AMPAIRS_ENVIRONMENT=dev
./gradlew composeApp:run

# Or inline
AMPAIRS_ENVIRONMENT=production ./gradlew composeApp:run
```

#### Default
If not specified, defaults to `dev` environment.

### 2. API Base URL Override

**Override the default API URL:**

#### System Property
```bash
./gradlew composeApp:run -Dampairs.api.baseUrl=https://staging-api.ampairs.com
```

#### Environment Variable
```bash
export AMPAIRS_API_BASE_URL=https://staging-api.ampairs.com
./gradlew composeApp:run
```

### 3. Web Authentication URL Override

**Override the web authentication URL for browser-based login:**

#### System Property
```bash
./gradlew composeApp:run -Dampairs.web.authUrl=https://staging.ampairs.com/login
```

#### Environment Variable
```bash
export AMPAIRS_WEB_AUTH_URL=https://staging.ampairs.com/login
./gradlew composeApp:run
```

## Common Use Cases

### Local Development (Default)

```bash
# Backend running on localhost:8080
# Web app running on localhost:4200
./gradlew composeApp:run
```

Default configuration:
- API: `http://localhost:8080`
- Web Auth: `http://localhost:4200/login`

### Development with Remote Backend

```bash
./gradlew composeApp:run \
  -Dampairs.environment=dev \
  -Dampairs.api.baseUrl=https://dev-api.ampairs.com
```

Configuration:
- API: `https://dev-api.ampairs.com`
- Web Auth: `http://localhost:4200/login` (still local)

### Staging Environment

```bash
./gradlew composeApp:run \
  -Dampairs.environment=production \
  -Dampairs.api.baseUrl=https://staging-api.ampairs.com \
  -Dampairs.web.authUrl=https://staging.ampairs.com/login
```

Configuration:
- API: `https://staging-api.ampairs.com`
- Web Auth: `https://staging.ampairs.com/login`

### Production Environment

```bash
./gradlew composeApp:run -Dampairs.environment=production
```

Configuration:
- API: `https://api.ampairs.com`
- Web Auth: `https://app.ampairs.com/login`

### Custom Configuration (e.g., Local Network Testing)

```bash
./gradlew composeApp:run \
  -Dampairs.environment=dev \
  -Dampairs.api.baseUrl=http://192.168.1.100:8080 \
  -Dampairs.web.authUrl=http://192.168.1.100:4200/login
```

Configuration:
- API: `http://192.168.1.100:8080`
- Web Auth: `http://192.168.1.100:4200/login`

## Environment Variables Reference

### Complete List

| Environment Variable | System Property | Default (DEV) | Default (PROD) | Description |
|---------------------|----------------|---------------|----------------|-------------|
| `AMPAIRS_ENVIRONMENT` | `ampairs.environment` | `dev` | - | Environment type: `dev` or `production` |
| `AMPAIRS_API_BASE_URL` | `ampairs.api.baseUrl` | `http://localhost:8080` | `https://api.ampairs.com` | Backend API base URL |
| `AMPAIRS_WEB_AUTH_URL` | `ampairs.web.authUrl` | `http://localhost:4200/login` | `https://app.ampairs.com/login` | Web authentication URL |

### Setting Environment Variables

#### macOS/Linux (Temporary)
```bash
export AMPAIRS_ENVIRONMENT=production
export AMPAIRS_API_BASE_URL=https://api.ampairs.com
export AMPAIRS_WEB_AUTH_URL=https://app.ampairs.com/login
```

#### macOS/Linux (Persistent - ~/.bashrc or ~/.zshrc)
```bash
echo 'export AMPAIRS_ENVIRONMENT=dev' >> ~/.zshrc
echo 'export AMPAIRS_API_BASE_URL=http://localhost:8080' >> ~/.zshrc
source ~/.zshrc
```

#### Windows (Command Prompt)
```cmd
set AMPAIRS_ENVIRONMENT=production
set AMPAIRS_API_BASE_URL=https://api.ampairs.com
```

#### Windows (PowerShell)
```powershell
$env:AMPAIRS_ENVIRONMENT="production"
$env:AMPAIRS_API_BASE_URL="https://api.ampairs.com"
```

## Packaged Application Configuration

When running the packaged desktop application (outside Gradle), use environment variables or configuration files.

### Using Environment Variables

#### macOS
```bash
# Set for current session
export AMPAIRS_ENVIRONMENT=production
open Ampairs.app

# Or inline
AMPAIRS_ENVIRONMENT=production open Ampairs.app
```

#### Linux
```bash
export AMPAIRS_ENVIRONMENT=production
./ampairs
```

#### Windows
```cmd
set AMPAIRS_ENVIRONMENT=production
ampairs.exe
```

### Using JVM Arguments (If creating launcher script)

#### launcher.sh (macOS/Linux)
```bash
#!/bin/bash
java -Dampairs.environment=production \
     -Dampairs.api.baseUrl=https://api.ampairs.com \
     -Dampairs.web.authUrl=https://app.ampairs.com/login \
     -jar ampairs-desktop.jar
```

#### launcher.bat (Windows)
```bat
@echo off
java -Dampairs.environment=production ^
     -Dampairs.api.baseUrl=https://api.ampairs.com ^
     -Dampairs.web.authUrl=https://app.ampairs.com/login ^
     -jar ampairs-desktop.jar
```

## Configuration Verification

### Check Current Configuration

The application logs the current configuration on startup:

```
=== Ampairs Configuration ===
Environment: PRODUCTION
API Base URL: https://api.ampairs.com
WebSocket URL: wss://api.ampairs.com
Web Auth URL: https://app.ampairs.com/login
Debug Mode: false
============================
```

### Programmatic Access

In code, access configuration via `ConfigurationManager`:

```kotlin
import com.ampairs.common.config.ConfigurationManager

// Current environment
val env = ConfigurationManager.environment  // DEV or PRODUCTION

// API Base URL
val apiUrl = ConfigurationManager.apiBaseUrl

// Web Authentication URL
val webAuthUrl = ConfigurationManager.webAuthUrl

// Debug mode
val isDebug = ConfigurationManager.isDebug

// Build API endpoint
val endpoint = ConfigurationManager.getApiUrl("customers")
// Result: https://api.ampairs.com/api/v1/customers

// Log full configuration
println(ConfigurationManager.logCurrentConfig())
```

## Troubleshooting

### Issue: Wrong URL Being Used

**Check configuration priority:**
1. Verify no system properties are set unintentionally
2. Check environment variables: `printenv | grep AMPAIRS`
3. Confirm environment setting: Check startup logs

**Debug command:**
```bash
# Print all AMPAIRS environment variables
printenv | grep AMPAIRS

# Check system properties (add to main.kt temporarily)
println(System.getProperties())
```

### Issue: Browser Opens to Wrong URL

**The web auth URL is resolved in this order:**
1. Parameter passed to `openAuthenticationBrowser(url)`
2. System property: `-Dampairs.web.authUrl`
3. Environment variable: `AMPAIRS_WEB_AUTH_URL`
4. Default based on `AMPAIRS_ENVIRONMENT`

**Check the logs for the actual URL being opened:**
```
DeepLinkHandler: Opened browser to: https://app.ampairs.com/login
DeepLinkHandler: Environment: production
```

### Issue: Cannot Connect to Backend

**Verify backend is accessible:**
```bash
# Test API endpoint
curl https://api.ampairs.com/api/v1/health

# Or for local development
curl http://localhost:8080/api/v1/health
```

**Check if using correct environment:**
- DEV should use `http://localhost:8080`
- PRODUCTION should use `https://api.ampairs.com`

## Best Practices

### Development

1. **Use defaults for local development** - No configuration needed
2. **Use system properties for temporary overrides** - Easy to change per run
3. **Use environment variables for persistent settings** - Set once in shell config

### Staging/Testing

1. **Create environment-specific launcher scripts**
2. **Document staging URLs in team wiki**
3. **Use separate staging backend and web app**

### Production

1. **Use production environment setting**
2. **Verify URLs before building release**
3. **Test authentication flow end-to-end**
4. **Monitor logs for configuration issues**

## Security Considerations

### Sensitive Information

- Never commit credentials or API keys to configuration
- Use environment variables for sensitive values
- Consider using a secrets management system for production

### URL Validation

- Ensure HTTPS is used in production
- Validate certificate authenticity
- Implement certificate pinning if needed

### Deep Link Security

- Deep links contain JWT tokens temporarily
- Tokens are consumed immediately after receipt
- Ensure deep link handler validates URL scheme (`ampairs://`)

## Examples

### Multi-Environment Development Team

```bash
# Developer 1: Local everything
./gradlew composeApp:run

# Developer 2: Local web, remote dev backend
./gradlew composeApp:run -Dampairs.api.baseUrl=https://dev-api.ampairs.com

# QA Tester: Staging environment
./gradlew composeApp:run \
  -Dampairs.environment=production \
  -Dampairs.api.baseUrl=https://staging-api.ampairs.com \
  -Dampairs.web.authUrl=https://staging.ampairs.com/login
```

### CI/CD Pipeline

```yaml
# .github/workflows/desktop-app.yml
env:
  AMPAIRS_ENVIRONMENT: production
  AMPAIRS_API_BASE_URL: https://api.ampairs.com
  AMPAIRS_WEB_AUTH_URL: https://app.ampairs.com/login

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Build Desktop App
        run: ./gradlew composeApp:packageDistributionForCurrentOS
```

## Related Documentation

- [Desktop Authentication Implementation](DESKTOP_AUTH_IMPLEMENTATION.md)
- [Web Application README](../ampairs-web/DESKTOP_AUTH_README.md)
- [Backend API Documentation](../ampairs-backend/API.md)

## Support

For configuration issues or questions:
- Check console logs for configuration details
- Verify environment variables are set correctly
- Review this guide's troubleshooting section
- Contact: dev@ampairs.com
