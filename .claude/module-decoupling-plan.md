# Module Decoupling Plan

**Branch**: `refactor/module-decoupling`  
**Goal**: Eliminate all inter-feature-impl dependencies so every module depends only downward through clearly defined contracts.

---

## Current Coupling Map

| Category | Description | Modules Affected |
|---|---|---|
| **A** | Agent plugin contracts (`ActionDescriptor`, `ActionHandler`, etc.) live in `feature:agent` but are used as a plugin framework by 5 other modules | customer, product, inventory, invoice, order |
| **B** | Cross-cutting services (`EventManager`, `WorkspaceContextManager`) live inside feature modules | customer, product, workspace, business |
| **C** | Shared domain models (`Customer`, `Product`, `Unit`) imported directly from feature impls | inventory, invoice, order |
| **D** | Direct DAO/Repository cross-access (`CustomerDao`, `CustomerRepository`, `ProductDao`) | invoice, order, workspace |
| **E** | Embedded UI components (`ProductsListScreen`, `CartItem`, `SubscriptionOnboardingScreen`) | invoice, workspace |

---

## Target Architecture (post all phases)

```
Layer 0  data:common
           └─ agent contracts (ActionDescriptor …)
           └─ EventManager interface + EventType
           └─ WorkspaceContextManager
           └─ CacheCleanable interface

Layer 1  auth:api   agent
Layer 2  form:api   tax:api   unit:api   subscription:api   event (impl)
Layer 3  customer:api   product:api
Layer 4  customer:impl   product:impl   auth:impl   form:impl
         tax:impl   unit:impl   subscription:impl
         business   inventory   invoice   order   workspace
Layer 5  shared  (wires everything, provides UI slots)
Layer 6  androidApp   desktopApp
```

Zero inter-impl cross-feature dependencies.

---

## Phase 1 — Move Cross-Cutting Contracts to `data:common`

**Fixes**: Category A (agent contracts) + Category B (EventManager, WorkspaceContextManager)  
**Effort**: Low — file moves + package renames, no new modules  
**Risk**: Low — purely additive  

### Tasks

- [ ] **1.1** Move agent plugin framework to `data:common`
  - Move `agent/core/` files: `ActionDescriptor`, `ActionHandler`, `ActionParameter`, `ActionResult`, `ActionType`, `AgentAction`, `NavigationTarget`, `ParameterType`
  - New package: `com.ampairs.common.agent`
  - Update `feature:agent` internal imports to new package
  - Update all consumer imports: customer, product, inventory, invoice, order
  - Remove `feature:agent` from build deps of customer, product, inventory, invoice, order
  - Compile-validate all 3 targets

- [ ] **1.2** Move `EventManager` interface + `EventType` to `data:common`
  - New package: `com.ampairs.common.event`
  - Keep `EventManagerFactory`, `EventLogger`, concrete impl in `feature:event`
  - Update consumer imports in customer, product, workspace
  - Remove `feature:event` from build deps of customer and product
  - (`workspace` still needs `feature:event` for `EventManagerFactory` — resolved in Phase 3)
  - Compile-validate all 3 targets

- [ ] **1.3** Move `WorkspaceContextManager` to `data:common`
  - New package: `com.ampairs.common.workspace` (alongside existing `WorkspaceContext`)
  - Update consumer imports in product, business
  - Remove `feature:workspace` from build deps of product and business
  - Compile-validate all 3 targets

**Phase 1 Exit Criteria**  
- `customer`, `product` have no `feature:agent` or `feature:event` dependency  
- `product`, `business` have no `feature:workspace` dependency  
- All 3 compilation targets pass

---

## Phase 2 — Split Imported Features into `:api` / `:impl`

**Fixes**: Category C (shared domain models)  
**Effort**: Medium — new sub-modules, `settings.gradle.kts` changes, build file wiring  
**Risk**: Medium — many build file changes, must keep existing module aliases working  

### Tasks

- [ ] **2.1** Split `feature:auth` → `auth:api` + `auth:impl`
  - `auth:api` contains: `TokenRepository` interface, `UserSession` model, `UserWorkspaceRepository` interface
  - `auth:impl` contains: everything else (Room DB, Ktor, ViewModels, UI)
  - All other features that import `auth` swap to `auth:api` only
  - Add both to `settings.gradle.kts`
  - Compile-validate all 3 targets

- [ ] **2.2** Split `feature:customer` → `customer:api` + `customer:impl`
  - `customer:api` contains: `CustomerSummary` model, `CustomerDataService` interface
  - `customer:impl` contains: `Customer` (full domain), Room, Ktor, ViewModel, UI
  - inventory, invoice, order, workspace swap to `customer:api`
  - Add both to `settings.gradle.kts`
  - Compile-validate all 3 targets

- [ ] **2.3** Split `feature:product` → `product:api` + `product:impl`
  - `product:api` contains: `ProductSummary` model, `ProductDataService` interface, `PAGE_SIZE` constant
  - `product:impl` contains: `Product` (full domain), `InventoryApiModel`, Room, Ktor, ViewModel, UI
  - inventory, invoice, order, workspace swap to `product:api`
  - Add both to `settings.gradle.kts`
  - Compile-validate all 3 targets

- [ ] **2.4** Split `feature:tax` → `tax:api` + `tax:impl`
  - `tax:api` contains: `TaxCode` model, `TaxCodeRepository` interface, `formatDecimal` util
  - `tax:impl` contains: Room, Ktor, ViewModel, UI
  - product swaps to `tax:api`
  - Add both to `settings.gradle.kts`
  - Compile-validate all 3 targets

- [ ] **2.5** Split `feature:form` → `form:api` + `form:impl`
  - `form:api` contains: `EntityType`, `EntityConfigSchema`, `EntityAttributeDefinition`, `AttributeDataType`, `ConfigRepository` interface
  - `form:impl` contains: Room, Ktor, ViewModel, UI
  - customer, business swap to `form:api`
  - Add both to `settings.gradle.kts`
  - Compile-validate all 3 targets

- [ ] **2.6** Split `feature:unit` → `unit:api` + `unit:impl`
  - `unit:api` contains: `Unit` model
  - `unit:impl` contains: Room, Ktor, ViewModel, UI
  - inventory swaps to `unit:api`
  - Add both to `settings.gradle.kts`
  - Compile-validate all 3 targets

- [ ] **2.7** Split `feature:subscription` → `subscription:api` + `subscription:impl`
  - `subscription:api` contains: `SubscriptionOnboardingManager` interface
  - `subscription:impl` contains: `SubscriptionOnboardingScreen`, `SubscriptionViewModel`, billing logic
  - Update `settings.gradle.kts`
  - (workspace UI coupling resolved in Phase 4)
  - Compile-validate all 3 targets

**Phase 2 Exit Criteria**  
- `inventory`, `invoice`, `order` depend on `customer:api` and `product:api` only — not on `:impl`  
- `product` depends on `tax:api` only  
- `customer`, `business` depend on `form:api` only  
- `inventory` depends on `unit:api` only  
- All 3 compilation targets pass

---

## Phase 3 — Service Interfaces for Cross-Module Data Access

**Fixes**: Category D (direct DAO/Repo cross-access)  
**Effort**: Medium — new interfaces + DI bindings  
**Risk**: Low — additive; existing code continues to work during transition  

### Tasks

- [ ] **3.1** Define `CustomerDataService` interface in `customer:api`
  ```kotlin
  interface CustomerDataService {
      fun searchCustomers(query: String): Flow<PagingData<CustomerSummary>>
      suspend fun getById(uid: String): CustomerSummary?
  }
  ```
  - `CustomerRepository` in `customer:impl` implements `CustomerDataService`
  - Bind via Metro `@Provides` in `shared`

- [ ] **3.2** Define `ProductDataService` interface in `product:api`
  ```kotlin
  interface ProductDataService {
      fun searchProducts(query: String): Flow<PagingData<ProductSummary>>
      suspend fun getById(uid: String): ProductSummary?
  }
  ```
  - `ProductRepository` in `product:impl` implements `ProductDataService`
  - Bind via Metro `@Provides` in `shared`

- [ ] **3.3** Migrate `invoice` to use service interfaces
  - Replace `CustomerRepository` + `CustomerDao` with `CustomerDataService`
  - Replace `ProductRepository` + `ProductDao` with `ProductDataService`
  - Remove all direct DAO imports from invoice
  - Compile-validate all 3 targets

- [ ] **3.4** Migrate `order` to use service interfaces
  - Replace `CustomerRepository` + `CustomerDao` with `CustomerDataService`
  - Replace `ProductRepository` + `ProductDao` with `ProductDataService`
  - Remove all direct DAO imports from order
  - Compile-validate all 3 targets

- [ ] **3.5** Define `CacheCleanable` interface in `data:common`
  ```kotlin
  interface CacheCleanable { suspend fun clearCache() }
  ```
  - `CustomerRepository`, `ProductRepository` implement `CacheCleanable`
  - Register both into a `Set<CacheCleanable>` via Metro multi-binding in `shared`
  - `workspace` injects `Set<CacheCleanable>` and calls `clearCache()` on workspace switch
  - Remove `CustomerRepository`, `ProductRepository` imports from `workspace`

- [ ] **3.6** Migrate `workspace` EventManager dependency
  - `workspace` currently needs `EventManagerFactory` from `feature:event`
  - Move `EventManagerFactory` into `data:common` alongside `EventManager` interface
  - Or: provide `EventManager` instance via DI from `shared` so `workspace` never imports `feature:event`
  - Remove `feature:event` from `workspace` build deps
  - Compile-validate all 3 targets

**Phase 3 Exit Criteria**  
- No `*Dao` or `*Repository` imports exist in `invoice`, `order`, or `workspace` that cross feature boundaries  
- `workspace` has zero `feature:event` dependency  
- All 3 compilation targets pass

---

## Phase 4 — Eliminate Embedded UI Components

**Fixes**: Category E (embedded composables across features)  
**Effort**: High — composable API redesign + slot pattern  
**Risk**: Medium — UI changes, need golden-path testing on all affected screens  

### Tasks

- [x] **4.1** Extract `CartItem` to shared UI
  - Move `CartItem` composable to `data:common` or a new `core:ui` module
  - Both `invoice` and `product` reference it from the shared location
  - Compile-validate all 3 targets

- [x] **4.2** Replace `ProductsListScreen` embedding in invoice with navigation slot
  - Add `productPickerSlot: @Composable (onSelected: (ProductSummary) -> Unit, onDismiss: () -> Unit) -> Unit` param to `InvoiceScreen`
  - `InvoiceEntryProvider` in `shared` supplies the real `ProductsListScreen` as the slot
  - Remove `feature:product` UI imports from `feature:invoice`
  - Test: invoice → product picker → select → item appears in invoice
  - Compile-validate all 3 targets

- [x] **4.3** Replace `SubscriptionOnboardingScreen` embedding in workspace with navigation slot
  - Add `subscriptionSlot: @Composable () -> Unit` param to `WorkspaceModulesScreen`
  - `WorkspaceEntryProvider` in `shared` supplies `SubscriptionOnboardingScreen`
  - Remove `feature:subscription` UI imports from `feature:workspace`
  - Test: workspace modules → subscription upsell flow still works
  - Compile-validate all 3 targets

- [x] **4.4** Replace `Phone` UI component usage in customer
  - `customer` imports `Phone` from `feature:auth` (a Composable)
  - Move `Phone` to `auth:api` or `data:common` core UI
  - Remove UI dependency on `auth:impl` from `customer` for this component
  - Compile-validate all 3 targets

**Phase 4 Exit Criteria**  
- No cross-feature Composable imports anywhere in feature modules  
- `invoice`, `workspace`, `customer` have zero UI-layer imports from other features  
- All 3 compilation targets pass  
- Manual smoke test of invoice creation, workspace module selection, customer phone field

---

## Final Validation Checklist

After all 4 phases:

- [ ] Run `./gradlew androidApp:compileDebugKotlinAndroid`
- [ ] Run `./gradlew shared:compileKotlinIosSimulatorArm64`
- [ ] Run `./gradlew desktopApp:compileKotlin`
- [ ] Grep confirms zero cross-feature `:impl` imports:
  ```bash
  grep -r "import com.ampairs.customer\." feature/invoice/ feature/order/ feature/inventory/ feature/workspace/
  grep -r "import com.ampairs.product\." feature/invoice/ feature/order/ feature/inventory/ feature/workspace/
  ```
- [ ] Dependency graph re-scanned to confirm no regressions

---

## Progress Tracker

| Task | Status | Notes |
|---|---|---|
| 1.1 Move agent plugin contracts | ✅ Done | 6 files → data:common/agent; 5 consumer modules updated; 2 pre-existing iOS/Desktop compile bugs fixed |
| 1.2 Move EventManager to data:common | ✅ Done | IEventManager + EventType/WorkspaceEvent/ConnectionState/EventLogger → data:common/event; EventManager implements IEventManager |
| 1.3 Move WorkspaceContextManager | ✅ Done | Already in data:common — no action needed |
| 2.1 Split auth:api / auth:impl | ✅ Done | Created feature/auth-api with TokenRepository, UserWorkspaceRepository, Token, RefreshToken, DeviceInfo, DeviceSession, DeviceService, KtorClient, KtorApiClientRequest; 8 consumer modules switched to authApi; event stale dep removed |
| 2.2 Split customer:api / customer:impl | ✅ Done | Customer, CustomerAddress, CustomerListItem → customer-api; inventory switched to customerApi |
| 2.3 Split product:api / product:impl | ✅ Done | ProductSummary, InventoryApiModel, Constants, ProductType/ServiceType → product-api; inventory switched to productApi |
| 2.4 Split tax:api / tax:impl | ✅ Done | TaxCode, TaxCodeType, formatDecimal → tax-api; TaxCodeLookup interface + binding; product switched to taxApi |
| 2.5 Split form:api / form:impl | ✅ Done | EntityType, EntityConfigSchema, EntityFieldConfig, EntityAttributeDefinition, AttributeDataType, ConfigLookup interface → form-api; customer + business switched to formApi |
| 2.6 Split unit:api / unit:impl | ✅ Done | Unit + UnitListItem → unit-api; inventory switched to unitApi |
| 2.7 Split subscription:api / subscription:impl | ✅ Done | SubscriptionOnboardingLookup interface → subscription-api; SubscriptionOnboardingManager implements it; DI binding added |
| 3.1 CustomerDataService interface | ✅ Done | CustomerDataService in customer-api; CustomerRepository implements it; @Provides binding in CustomerModule |
| 3.2 ProductDataService interface | ✅ Done | ProductDataService in product-api; ProductSummary.quantity added; ProductRepository implements it; @Provides binding in ProductModule |
| 3.3 Migrate invoice to service interfaces | ✅ Done | InvoiceItem uses ProductSummary; InvoiceViewModel/InvoiceRepository use CustomerDataService+ProductDataService |
| 3.4 Migrate order to service interfaces | ✅ Done | OrderItem uses ProductSummary; OrderViewModel/OrderRepository use CustomerDataService+ProductDataService |
| 3.5 CacheCleanable multi-binding | ✅ Done | CacheCleanable in data:common; CustomerRepository+ProductRepository implement it; Set<CacheCleanable> @Provides in shared |
| 3.6 Migrate workspace EventManager | ✅ Done | EventManagerProvider returns IEventManager; EventManagerProvider binding moved from workspace to shared/EventManagerModule; feature:event+customer+product removed from workspace deps |
| 4.1 Extract CartItem to shared UI | ✅ Done | |
| 4.2 ProductsListScreen navigation slot | ✅ Done | |
| 4.3 SubscriptionOnboardingScreen slot | ✅ Done | |
| 4.4 Phone component extraction | ✅ Done | |
| Final validation | ⬜ Pending | Needs all above |