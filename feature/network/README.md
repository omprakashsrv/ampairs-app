# feature:network

Network security infrastructure. Provides certificate pinning, MITM protection, app-update enforcement, and a factory for creating secure Ktor HTTP clients.

## Responsibilities

- Pin TLS certificates to prevent MITM attacks
- Enforce minimum app version requirements (show blocking update dialog)
- Create `HttpClient` instances with pinning and custom trust managers
- Manage certificate expiry and pin rotation

## Key Classes

| Class | Purpose |
|---|---|
| `CertificatePinningService` | Interface: `validateCertificate()`, `isPinValid()` |
| `CertificateManager` | Stores pins, checks expiry, supports rotation |
| `AppUpdateEnforcer` | Checks version against backend minimum, shows blocking dialog |
| `SecureKtorClientFactory` | Builds a Ktor `HttpClient` with certificate pinning configured |

## Platform-Specific

| Platform | Implementation |
|---|---|
| Android | `AndroidCertificatePinningService` via OkHttp `CertificatePinner` |
| Android | `AndroidAppUpdateEnforcer` with Play Store integration |
| Android | `AndroidSecureEngine` (custom `SSLSocketFactory`) |
| Desktop | OkHttp-based pinning with file-backed pin storage |

## Koin Module

Platform-specific — registered in `androidMain` / `desktopMain` Koin setups. No `commonMain` module (interface only in common).

## Configuration

Certificate pins are configured at app startup. See the source-level README for pin format and rotation instructions.

> For detailed setup and rotation procedures, see `network/src/commonMain/.../README.md`.
