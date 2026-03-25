# feature:aws

AWS cloud storage integration. Provides pre-signed S3 URL generation for image uploads and downloads.

## Responsibilities

- Generate pre-signed S3 URLs for secure, time-limited access to objects
- Abstract the S3 client behind a common interface for all platforms

## Key Classes

| Class | Purpose |
|---|---|
| `S3Client` | Common interface — `getPreSignedUrl(bucket, key): String` |
| `AndroidS3Client` | Android implementation using `aws.sdk.kotlin:s3` |
| `DesktopS3Client` | Desktop implementation using `aws.sdk.kotlin:s3` |
| `IosS3Client` | iOS stub (returns direct URL; AWS iOS SDK not yet integrated) |
| `S3CredentialProvider` | Provides AWS credentials (Android/Desktop only) |

## Koin Module

Clients are registered by the platform-specific Koin setup. Consumers inject `S3Client`.

## Platform-Specific

| Platform | SDK | Notes |
|---|---|---|
| Android | `aws.sdk.kotlin:s3` | Full pre-signed URL support |
| Desktop | `aws.sdk.kotlin:s3` | Full pre-signed URL support |
| iOS | None | Stub implementation; direct URL returned |

> **Note:** `aws.sdk.kotlin:s3` has no iOS artifact. It must only be declared in `androidMain` and `desktopMain` source sets, not in `commonMain` or `iosMain`.
