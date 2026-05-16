# feature:unit

Unit of measurement management and conversion. Provides a catalogue of units (weight, volume, length, etc.) and a conversion engine used by products and inventory.

## Responsibilities

- Manage unit definitions (name, symbol, category)
- Perform unit-to-unit conversions via configurable conversion factors
- Provide a picker UI for selecting units in forms
- Offline-first sync via Room + Store5

## Key Classes

| Class | Purpose |
|---|---|
| `UnitApi` / `UnitApiImpl` | REST endpoints |
| `UnitRepository` | Offline-first data access |
| `UnitStore` | Store5 Fetcher + Room SourceOfTruth |
| `UnitConversionEngine` | Converts a value between two compatible units |
| `UnitListViewModel` | Browsable unit list |
| `UnitFormViewModel` | Create/edit unit |

## Koin Module

```kotlin
unitModule          // in com.ampairs.unit.di
unitPlatformModule  // platform-specific (DB factory)
```

## Database

`UnitDatabase` — workspace-scoped (`factory` scope).
