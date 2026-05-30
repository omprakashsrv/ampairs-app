# feature:business

Business profile management. Covers the company's identity, operational settings, tax configuration, images, and custom attributes within a workspace.

## Responsibilities

- View and edit the business profile (name, address, GSTIN, logo)
- Configure business operations (opening hours, delivery radius, etc.)
- Set up tax configuration (GST rates, tax components)
- Upload and manage business images
- Define custom entity attributes for forms

## Key Classes

| Class | Purpose |
|---|---|
| `BusinessApi` / `BusinessApiImpl` | REST endpoints for business data |
| `BusinessRepository` | Offline-first data access (reactive DAO Flow + API sync) |
| `BusinessOverviewViewModel` | Dashboard summary |
| `BusinessProfileViewModel` | Profile form state |
| `BusinessOperationsViewModel` | Operations settings |
| `BusinessTaxConfigViewModel` | Tax configuration |
| `BusinessImagesViewModel` | Image upload/manage |
| `BusinessCustomAttributesViewModel` | Custom form fields |

## Domain Models

`Business`, `BusinessProfile`, `BusinessOverview`, `BusinessOperations`, `BusinessStore`, `BusinessImage`, `TaxConfiguration`, `BusinessType`

## Koin Module

```kotlin
businessModule  // in com.ampairs.business
```

## Database

`BusinessRoomDatabase` — workspace-scoped (`factory` scope in Koin).
