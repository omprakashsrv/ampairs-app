# Feature Module `:api` / `:impl` Split Pattern

## Problem

In a multi-module KMP project, features naturally need data from each other.  
The naive solution — importing directly from another feature's `impl` module — creates tight coupling:

```
feature:invoice  →  feature:customer  (imports CustomerRepository, CustomerDao)
feature:invoice  →  feature:product   (imports ProductRepository, ProductDao)
feature:order    →  feature:customer
feature:order    →  feature:product
feature:workspace → feature:subscription (imports SubscriptionOnboardingScreen)
```

This causes **circular dependency risk**, **slow incremental builds** (changing customer rebuilds invoice), and **inability to test modules in isolation**.

---

## Solution: `:api` / `:impl` Split

Each feature is split into two Gradle modules:

```
feature/customer-api/   ← thin contracts only (interfaces, domain models)
feature/customer/       ← full implementation (Room DB, Ktor, ViewModels, UI)
```

Consumer modules depend on `:api` only. The `:impl` module never appears in another feature's `build.gradle.kts`.

---

## What goes in `:api`

| What | Rule |
|---|---|
| **Domain models** shared across features | `Customer`, `ProductSummary`, `TaxCode`, `Unit` |
| **Service interfaces** for cross-feature data access | `CustomerDataService`, `ProductDataService`, `UserDataService` |
| **Repository interfaces** needed by other modules | `TokenRepository`, `UserWorkspaceRepository` |
| **Lookup interfaces** for settings/config | `SubscriptionOnboardingLookup`, `TaxCodeLookup`, `ConfigLookup` |
| **Utility types** used externally | `NumberFormatUtils`, `SubscriptionOnboardingLookup` |

**What does NOT go in `:api`:**
- Room entities, DAOs, database classes
- Ktor API implementations
- ViewModels and Compose screens
- Any `@Inject` implementation class

---

## Module Layout

```
feature/customer-api/
└── src/commonMain/kotlin/com/ampairs/customer/
    ├── domain/
    │   └── Customer.kt              ← shared domain model
    └── data/
        └── CustomerDataService.kt   ← interface only

feature/customer/
└── src/commonMain/kotlin/com/ampairs/customer/
    ├── data/
    │   ├── api/CustomerApiImpl.kt
    │   ├── db/CustomerDao.kt
    │   └── repository/CustomerRepository.kt   ← implements CustomerDataService
    ├── di/CustomerModule.kt                   ← binds CustomerDataService → CustomerRepository
    └── ui/...
```

---

## Service Interface Pattern

### 1. Define the interface in `:api`

```kotlin
// feature/customer-api/.../customer/data/CustomerDataService.kt
package com.ampairs.customer.data

import com.ampairs.customer.domain.Customer

interface CustomerDataService {
    suspend fun getById(uid: String): Customer?
}
```

### 2. Implement on the repository in `:impl`

```kotlin
// feature/customer/.../repository/CustomerRepository.kt
@Inject @SingleIn(AppScope::class)
class CustomerRepository(
    private val customerDao: CustomerDao,
    private val customerApi: CustomerApi,
) : CustomerDataService, CacheCleanable {

    override suspend fun getById(uid: String): Customer? = getCustomer(uid)

    override suspend fun clearCache() { customerDao.clearWorkspaceCustomers() }
}
```

### 3. Bind the interface in the DI module

```kotlin
// feature/customer/.../di/CustomerModule.kt
@ContributesTo(AppScope::class)
interface CustomerDaoModule {
    companion object {
        @Provides
        fun provideCustomerDataService(repo: CustomerRepository): CustomerDataService = repo
    }
}
```

### 4. Consume from `:api` in other features

```kotlin
// feature/invoice/.../viewmodel/InvoiceViewModel.kt
@AssistedInject
class InvoiceViewModel(
    private val customerDataService: CustomerDataService,   // ← api interface
    private val productDataService: ProductDataService,     // ← api interface
    private val tokenRepository: TokenRepository,           // ← api interface
    ...
)
```

```kotlin
// feature/invoice/build.gradle.kts
dependencies {
    implementation(projects.feature.authApi)        // ✅ api only
    implementation(projects.feature.customerApi)    // ✅ api only
    implementation(projects.feature.productApi)     // ✅ api only
}
```

---

## UI Slot Pattern (for Composable cross-feature embedding)

When a screen needs to display a composable from another feature, use a **slot parameter** instead of a direct import. The slot is wired by the `shared` navigation layer.

### Problem

```kotlin
// feature/invoice — directly imports from feature/product UI layer
import com.ampairs.product.ui.list.ProductsListScreen

fun InvoiceScreen(...) {
    ProductsListScreen(onProductClick = { ... })  // hard dependency
}
```

### Solution

```kotlin
// feature/invoice — declares a slot, knows nothing about product UI
fun InvoiceScreen(
    ...
    productPickerSlot: @Composable (onProductClick: (String) -> Unit) -> Unit = {},
) {
    productPickerSlot { productId -> /* handle selection */ }
}
```

```kotlin
// shared/.../navigation/providers/InvoiceEntryProvider.kt
// The shared layer wires the real screen into the slot
is InvoiceRoute.Root -> NavEntry(key) {
    InvoiceScreen(
        ...
        productPickerSlot = { onProductClick ->
            ProductsListScreen(
                onProductClick = onProductClick,
                onCreateProduct = {},
                onFormConfig = {}
            )
        }
    )
}
```

The slot default is a no-op lambda `{}`, so the screen compiles and previews independently.

---

## Resulting Dependency Layers

```
Layer 0   data:common
             CacheCleanable, CartItem, Phone, IEventManager, agent contracts

Layer 1   :api modules  (no inter-feature deps)
             auth-api, customer-api, product-api, form-api,
             tax-api, unit-api, subscription-api

Layer 2   agent, event

Layer 3   auth, tax, unit, update, subscription, product, form
             (each depends on its own :api + data:common)

Layer 4   business, customer, inventory, invoice, order, workspace
             (depend on :api modules only — never on another feature's :impl)

Layer 5   shared
             (wires navigation slots, owns entry providers, aggregates DI)

Layer 6   androidApp, desktopApp
```

**Invariant**: A module in Layer N may only import from layers 0 through N-1.  
No module in Layer 4 imports from another Layer 4 module's source.

---

## Checklist When Adding a New Cross-Feature Dependency

- [ ] Does the consumer only need an interface, not the full impl? → put the interface in `:api`
- [ ] Does the consumer need a domain model? → put the model in `:api`
- [ ] Does the consumer need a Composable from another feature? → use a slot parameter
- [ ] Add `@Provides fun provideXxxService(impl: XxxRepository): XxxService = impl` in the impl's DI module
- [ ] Build file: `implementation(projects.feature.xyzApi)` — never `implementation(projects.feature.xyz)` across feature boundaries
- [ ] Compile all 3 targets: Android, iOS simulator, Desktop

---

## :api Module `build.gradle.kts` Template

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)
    android { namespace = "com.ampairs.xyz.api"; compileSdk = ...; minSdk = ... }
    jvm("desktop")
    iosArm64(); iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.data.common)
                implementation(libs.kotlinx.serialization.json)
                // No Compose, no Room, no Ktor client
            }
        }
        // platform source sets with ktor client if api models need it
    }
}
```

No Compose, no Room, no Ktor client engine in an `:api` module — keep it pure data contracts.
