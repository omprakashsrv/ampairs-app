# Ampairs Mobile Application

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Kotlin Multiplatform business management client that integrates with the Ampairs Spring Boot backend system. Built with Compose Multiplatform and offline-first architecture, targeting **Android, iOS, and Desktop (JVM)** platforms.

## Overview

Ampairs Mobile is part of a comprehensive three-tier business management ecosystem:

1. **Backend** - Spring Boot + Kotlin REST API
2. **Web Frontend** - Angular + Material Design 3
3. **Mobile App** - Kotlin Multiplatform ← **THIS PROJECT**

### Key Features

- **Offline-First Architecture** - Full functionality without internet connectivity
- **Multi-Tenant Support** - Workspace isolation with secure authentication
- **Comprehensive Business Management**:
  - Customer Relationship Management (CRM)
  - Product Catalog & Inventory Management
  - Order Processing & Fulfillment
  - Invoice Generation & GST Compliance
  - Tally ERP Integration
- **Real-time Synchronization** - Background sync with conflict resolution
- **Cross-Platform UI** - Native experience on Android, iOS, and Desktop

## Technology Stack

### Core Frameworks
- **Kotlin 2.2.0** - Modern multiplatform language
- **Compose Multiplatform 1.8.2** - Declarative UI framework
- **Material 3 Design System** - Consistent design language

### Architecture & Data
- **Store5 5.1.0** - Offline-first data management with caching
- **Room 2.7.0-alpha11** - Cross-platform local database
- **Ktor 3.2.1** - HTTP client with JWT authentication
- **Koin 4.1.0** - Dependency injection framework
- **kotlinx.serialization 1.7.3** - JSON parsing

### Platform Support
- **Android**: Min SDK 24, Target SDK 35
- **iOS**: iOS 14+ with Core Data integration
- **Desktop**: JVM with JDBC drivers

## Project Structure

```
ampairs-app/
├── composeApp/                      # Main application module
│   ├── src/
│   │   ├── commonMain/             # Shared code across platforms
│   │   │   └── kotlin/com/ampairs/
│   │   │       ├── auth/           # Authentication module
│   │   │       ├── workspace/      # Workspace management
│   │   │       ├── customer/       # Customer management
│   │   │       ├── product/        # Product catalog
│   │   │       ├── order/          # Order processing
│   │   │       ├── invoice/        # Invoice generation
│   │   │       ├── tally/          # Tally integration
│   │   │       ├── unit/           # Unit management
│   │   │       └── common/         # Shared utilities
│   │   ├── androidMain/            # Android-specific code
│   │   ├── iosMain/                # iOS-specific code
│   │   └── desktopMain/            # Desktop-specific code
│   └── build.gradle.kts
├── gradle/libs.versions.toml        # Version catalog
└── README.md
```

## Getting Started

### Prerequisites

- **macOS** (required for iOS development)
- **Java 21+**
- **Android Studio** with Kotlin Multiplatform Mobile plugin
- **Xcode 15+** (for iOS development)
- **CocoaPods** (for iOS dependencies)

### Environment Setup

1. **Verify your environment** using KDoctor:
   ```bash
   brew install kdoctor
   kdoctor
   ```

2. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd ampairs-app
   ```

3. **Open in Android Studio**:
   - Switch view from "Android" to "Project" to see all platform targets
   - Sync Gradle files

## Building and Running

### Android

**Run on emulator/device:**
```bash
./gradlew composeApp:assembleDebug
./gradlew composeApp:installDebug
```

**In Android Studio:**
- Select `composeApp` configuration
- Choose your device/emulator
- Click Run

### Desktop

**Run application:**
```bash
./gradlew composeApp:run
```

**Create native distribution:**
```bash
./gradlew composeApp:package
```

Output location: `build/compose/binaries`

### iOS

**Run on simulator:**
1. Select "Edit Configurations" in Android Studio
2. Navigate to iOS Application > iosApp
3. Select target device
4. Click Run

**Run on physical device:**
1. Find your Team ID: `kdoctor --team-ids`
2. Update `iosApp/Configuration/Config.xcconfig`:
   ```
   TEAM_ID=YOUR_TEAM_ID
   ```
3. Re-open project in Android Studio
4. Select your device and run

### Cleanup

```bash
./gradlew clean
./cleanup.sh  # Remove IDE files and build artifacts
```

## Architecture Overview

### Offline-First Pattern

The app uses **Store5** for robust offline-first data management:

```
UI Layer (Compose/MVI)
    ↓
Store5 Layer (Cache + Fetch)
    ↓
Repository Layer (Business Logic)
    ↓
Data Layer (Room + Ktor)
```

### Database Strategy

- **Platform-specific drivers**: Room (Android), Core Data bridge (iOS), JDBC (Desktop)
- **Multi-tenant isolation**: All entities include `tenant_id` for workspace segregation
- **Sync metadata**: Entities track `syncStatus` and timestamps for offline-first operations
- **Workspace-scoped**: Each workspace maintains isolated database instances

### Authentication & Security

- **JWT Authentication**: Phone + OTP flow with device ID support
- **Multi-device Support**: Secure token storage via encrypted Room database
- **Tenant Context**: HTTP headers include workspace/company context for API calls

### Dynamic Module Navigation

- **Backend-Driven**: Module availability determined by backend-installed modules
- **Type-Safe Routes**: Registry-based navigation with compile-time validation
- **Graceful Fallbacks**: "Update App" dialog for unimplemented modules

## Feature Modules

### Workspace Management
- Multi-tenant workspace selection
- Workspace context isolation
- Dynamic module discovery

### Customer Management (CRM)
- Customer CRUD with offline support
- Address management
- Customer groups and types
- GST compliance

### Product Catalog
- Product management with variants
- Categories and tax codes
- Image storage and caching
- Unit conversions

### Order Processing
- Order creation and management
- Status workflow tracking
- Pricing and discounts

### Invoice Generation
- Invoice creation with GST compliance
- PDF generation
- Email delivery
- Payment tracking

### Unit Management
- Custom unit definitions
- Unit conversion engine
- Multi-unit product support

## Theme System

The app supports dynamic theme switching:

- **Options**: Light, Dark, System (default)
- **Access**: Theme toggle in app header
- **Persistence**: Settings stored in DataStore Preferences
- **Platform**: Consistent across Android, iOS, Desktop

## Development Guidelines

### KMP Best Practices

Always use KMP-compatible APIs in `commonMain`:

✅ **Use**:
- `Clock.System.now()` for timestamps
- `kotlinx.datetime.*` for date/time operations
- `kotlinx.coroutines.*` for async operations
- Platform-specific expect/actual for native APIs

❌ **Avoid**:
- `System.currentTimeMillis()` (JVM-specific)
- `java.util.Date`, `Calendar` (Java-specific)
- `String.format()` (JVM-specific)
- Platform-specific imports in `commonMain`

### Koin Dependency Injection

- **Workspace-aware modules**: Use `factory` scope for workspace-scoped databases
- **Non-workspace modules**: Use `single` for auth and workspace databases
- **ViewModels**: Use `viewModel` or `viewModelOf` for automatic lifecycle management

### Database Conventions

- **Entity naming**: `{Feature}Entity` (e.g., `CustomerEntity`)
- **DAO naming**: `{Feature}Dao` (e.g., `CustomerDao`)
- **Timestamps**: Use ISO 8601 string format for sync tracking
- **UIDs**: Client-generated with `UidGenerator.generateUid(prefix)`

## Testing

Run tests across all platforms:

```bash
# Android
./gradlew compileDebugKotlinAndroid

# iOS
./gradlew compileKotlinIosSimulatorArm64

# Desktop
./gradlew compileKotlinDesktop
```

## Backend Integration

The mobile app integrates with the Ampairs Spring Boot backend:

- **API Version**: `/api/v1/{resource}`
- **Authentication**: JWT tokens with automatic refresh
- **Multi-tenancy**: `X-Workspace-ID` header for workspace context
- **Response Format**: `ApiResponse<T>` wrapper with error handling

Refer to backend `CLAUDE.md` for API specifications.

## Contributing

1. Follow KMP best practices (see CLAUDE.md)
2. Use `factory` scope for workspace-aware Koin modules
3. Write offline-first code with Store5 patterns
4. Test on all target platforms before committing
5. Maintain consistent Material 3 design patterns

## Version Catalog

Key dependencies are managed in `gradle/libs.versions.toml`:

- Kotlin 2.2.0
- Compose Multiplatform 1.8.2
- Room 2.7.0-alpha11
- Store5 5.1.0
- Ktor 3.2.1
- Koin 4.1.0

## Documentation

- **CLAUDE.md** - Comprehensive development guidelines and architecture documentation
- **BACKEND_UNIT_MODULE_SPEC.md** - Unit module backend integration specification

## License

Apache License 2.0 - See LICENSE file for details

## Support

For issues and feature requests, please contact the development team or refer to internal documentation.

---

**Built with ❤️ using Kotlin Multiplatform and Compose**
