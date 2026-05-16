# feature:customer

Full-featured CRM module. Manages customers, customer groups, customer types, state/region master data, location picking, and customer image galleries.

## Responsibilities

- CRUD for customers with address, GST, and contact info
- Customer type and group classification
- State/region master data (pre-loaded)
- Location picking via map (Google Maps on Android, JMapViewer on Desktop)
- Contact picker from device address book (Android)
- Customer image upload and gallery management

## Architecture

```
UI Screens
    ↓
ViewModels (Koin factory injection)
    ↓
CustomerStore / CustomerTypeStore / CustomerGroupStore / StateStore  (Store5)
    ↓
Repository (offline-first: DB first, async server sync)
    ↓
CustomerApi / CustomerTypeApi / CustomerGroupApi / CustomerImageApi + CustomerDatabase (Room)
```

## Key Classes

| Class | Purpose |
|---|---|
| `CustomerApi` / `CustomerApiImpl` | Customer CRUD endpoints |
| `CustomerGroupApi`, `CustomerTypeApi` | Classification endpoints |
| `CustomerImageApi` | Image upload/list/delete |
| `CustomerRepository` | Offline-first data access |
| `CustomersListViewModel` | Paginated customer list |
| `CustomerFormViewModel` | Create/edit form with UID generation |
| `CustomerDetailsViewModel` | Customer detail view |
| `CustomerImageViewModel` | Image gallery management |
| `CustomerActionHandler` | Agent integration (search, read, create, update) |

## Domain Models

`Customer`, `CustomerGroup`, `CustomerType`, `State`, `CustomerImage`, `MasterState`

## Koin Module

```kotlin
customerModule     // in com.ampairs.customer.di
customerPlatformModule  // platform-specific (DB factory)
```

## Navigation Routes

```kotlin
CustomerListRoute
CustomerDetailsRoute(customerId)
CustomerCreateRoute(customerId?)
CustomerTypeListRoute / CustomerTypeCreateRoute(id?)
CustomerGroupListRoute / CustomerGroupCreateRoute(id?)
StateListRoute
```

## Platform-Specific

| Platform | Feature |
|---|---|
| Android | Google Maps location picker, device contact picker |
| Desktop | JMapViewer OpenStreetMap dialog |
| iOS | Native Maps integration |

## Database

`CustomerDatabase` — workspace-scoped (`factory` scope).

Tables: customers, customer_groups, customer_types, states, customer_images
