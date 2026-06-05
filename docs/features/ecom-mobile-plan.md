# Ecom Storefront — Android & iOS Module Plan

**Target**: `feature/ecom` + `feature/ecom-api` in `ampairs-app` (Compose Multiplatform, Android + iOS first; Desktop compiles as a free side-effect of KMP).
**Source of truth**: backend `ecom` module + mobile API contract (`ecommobilecontract.md`), spec `008-ecommerce-order-platform`, and the Claude Design handoff (`Ecom Storefront Prototype.html`, Direction **A — app-native**).
**Status**: Plan for review. No code written yet.

---

## 1. Scope & key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Design direction | **A · app-native** (bottom tab bar, 2-col image grid, quick-add ⇄ stepper, full-screen cart, multi-step checkout) | This is a mobile module; Direction A is the design built for phones. Direction B (web header + dense list) is not implemented. |
| Auth model | **Guest-first by default, with a per-storefront `accessMode` toggle that can enforce login-first store-access gating** | Resolves the spec/contract (guest cart, login-at-checkout — FR-010/FR-012/FR-014) vs the design's final login-first gate. One nav graph, branched by config — mirrors the prototype's "Store access" Tweak. |
| Auth reuse | Reuse `feature/auth` + `feature/auth-api` **as-is** (phone + OTP, JWT, refresh, device session, `TokenRepository`, `X-Workspace-ID`) | FR-019/FR-020 require single platform-wide identity via the existing auth module. No new auth code. |
| Offline | Room-backed catalog (cursor incremental sync), online-only cart with optimistic local mirror, cached orders/addresses | Per contract §3, §8, §9. |
| DI | **Metro** (`@ContributesTo`, `@Inject`, `@ContributesIntoMap`+`@ViewModelKey`, `metroViewModel`) | The codebase uses Metro, not Koin (CLAUDE.md `.claude/rules.md` is authoritative over the older Koin docs). |
| Navigation | **Navigation3** `EcomRoute : NavKey` + `ecomEntryProvider` chained in `CombinedEntryProvider` | Matches every existing feature. |

> **Workspace note**: existing features are merchant-side and workspace-scoped (DB path `workspace_{slug}_{module}.db`). The ecom storefront is **customer-side** and scoped by **storefront slug**, not by the merchant workspace the logged-in user belongs to. The ecom Room DB is therefore scoped by `storefrontSlug` (see §7), and ecom is reachable as a **standalone entry surface**, not from inside the merchant workspace drawer.

---

## 2. Module layout

Two Gradle projects, mirroring the `product` / `product-api` split.

```
feature/ecom-api/                       # pure domain + public interfaces, serialization only
  src/commonMain/kotlin/com/ampairs/ecom/api/
    model/        Storefront, CatalogMeta, ListedProduct, ProductSyncItem,
                  Cart, CartItem, Address, EcomOrder, EcomOrderLineItem,
                  OrderStatus, LineItemStatus, StockStatus, StorefrontStatus
    EcomStorefrontService.kt            # interface other surfaces consume (e.g. "open store X")
    EcomSessionStore.kt                 # interface: current storefront slug + cart session token

feature/ecom/                           # impl: Room, Ktor, repos, sync, DI, Compose UI
  src/commonMain/kotlin/com/ampairs/ecom/
    data/api/      EcomApi (interface) + EcomApiImpl (Ktor)
    data/db/       EcomRoomDatabase, entities/, dao/, EcomRoomDatabaseConstructor (expect)
    data/repository/  StorefrontRepository, CatalogRepository, CartRepository,
                      AddressRepository, EcomOrderRepository
    domain/        StorefrontAccessManager, price/savings helpers, cursor logic
    sync/          EcomCatalogSyncDelegate, EcomAddressSyncDelegate, EcomOrderSyncDelegate
    di/EcomModule.kt                    # @ContributesTo: DAO + service @Provides
    ui/
      gate/        LoginGateScreen, RequestAccessScreen, PendingAccessScreen
      browse/      BrowseHomeScreen (category tiles + brand row + popular), DrillDownScreen
      catalog/     ProductCardComponents (quick-add), ProductDetailScreen, SearchScreen
      cart/        CartScreen (full-screen)
      checkout/    CheckoutScreen (Address → Review → Place), AddressFormSheet
      order/       OrderPlacedScreen, OrderTrackingScreen, OrdersListScreen
      account/     AccountScreen, AddressesScreen
      components/   StatusChip, Stepper, Bill, Timeline, SearchBox, BottomNav, theme tokens
  src/androidMain/kotlin/com/ampairs/ecom/EcomModule.android.kt   # DB factory (Android)
  src/iosMain/kotlin/com/ampairs/ecom/EcomModule.ios.kt          # DB factory (iOS)
  src/desktopMain/kotlin/com/ampairs/ecom/EcomModule.desktop.kt  # DB factory (Desktop stub)
  src/commonMain/composeResources/values/strings.xml             # all UI text
  schemas/                              # Room exported schemas
```

**`settings.gradle.kts`** — add `":feature:ecom-api", ":feature:ecom"` to the `include(...)` block.

**`feature/ecom-api/build.gradle.kts`** — mirror `product-api`: `kotlinMultiplatform`, `androidKmpLibrary`, `kotlinSerialization`; commonMain depends only on `libs.kotlinx.serialization.json`; namespace `com.ampairs.ecom.api`.

**`feature/ecom/build.gradle.kts`** — mirror `product`: plugins `kotlinMultiplatform, androidKmpLibrary, compose.compiler, jetbrainsCompose, kotlinSerialization, ksp, room, metro`. commonMain deps: `api(projects.feature.ecomApi)`, `api(projects.feature.authApi)` (for `TokenRepository`/`httpClient`), `implementation(projects.data.common)`, `implementation(projects.data.sync)`, `metro.runtime`, `metrox.viewmodel.compose`, `room.runtime`, compose, ktor, datetime, coil. KSP Room rows for all targets; `room { schemaDirectory("$projectDir/schemas") }`.

---

## 3. Reused infrastructure (no new copies)

| Need | Reuse | From |
|---|---|---|
| Phone + OTP login UI & flow | `LoginViewModel`, `PhoneScreen`, `OtpScreen`, `AuthRoute.Phone/Otp` | `feature/auth` |
| Token storage / access token / logged-in check | `TokenRepository` (`getAccessToken`, `getRefreshToken`, `clearTokens`, `getWorkspaceIdSync`) | `feature/auth-api` |
| Ktor client + bearer refresh + `X-Workspace-ID` | `httpClient(engine, tokenRepository)` + `get/post/put/delete` helpers | `feature/auth-api` (`KtorClient.kt`) |
| URL building | add `ApiUrlBuilder.ecomUrl("…")` and `ApiUrlBuilder.storeUrl(slug, "…")` | `data/common` |
| Workspace/scoped DB factory | `WorkspaceAwareDatabaseFactory`, `DatabaseScopeManager` (keyed here by storefront slug) | `data/common` |
| Background push/pull orchestration | `CentralSyncService`, `SyncDelegate`, `SyncEntity`, `@SyncEntityKey` | `data/sync` |
| KV prefs (cart session token, sync cursor, last storefront) | existing `DataStore<Preferences>` via `DataStoreManager` | `data/common` |
| Theme | `PlatformAmpairsTheme`, `MaterialTheme.colorScheme` | `shared` |

**Auth login is reused verbatim** — the ecom gate screen does not re-implement OTP; it routes into `AuthRoute.Phone`/`AuthRoute.Otp` and resumes the ecom flow on `onLoginSuccess`.

---

## 4. Navigation graph

Add to `shared/.../navigation/Routes.kt`:

```kotlin
@Serializable sealed interface EcomRoute : NavKey {
    @Serializable data class Storefront(val slug: String) : EcomRoute   // entry; resolves gate vs shop
    @Serializable data object Browse : EcomRoute                        // tab: home (categories/brands/popular)
    @Serializable data class DrillDown(                                 // category/brand/subcategory/search results
        val category: String? = null, val brand: String? = null,
        val subcategory: String? = null, val query: String? = null,
    ) : EcomRoute
    @Serializable data class ProductDetail(val productId: String) : EcomRoute
    @Serializable data object Cart : EcomRoute                          // full-screen
    @Serializable data object Checkout : EcomRoute
    @Serializable data class OrderPlaced(val orderRef: String) : EcomRoute
    @Serializable data object Orders : EcomRoute                        // tab
    @Serializable data class OrderTracking(val orderRef: String) : EcomRoute
    @Serializable data object Account : EcomRoute                       // tab
    @Serializable data object Addresses : EcomRoute
    // gate sub-states (used only when accessMode = LOGIN_FIRST or no access)
    @Serializable data class RequestAccess(val slug: String) : EcomRoute
    @Serializable data class PendingAccess(val slug: String) : EcomRoute
}
```

`ecomEntryProvider(key, backStack)` in `shared/.../navigation/providers/EcomEntryProvider.kt`, chained in `combinedEntryProvider(...)` with `?: ecomEntryProvider(key, backStack)`. The 3 tabs (Browse, Orders, Account — Cart is reached via the sticky bar / app-bar icon, full-screen) are switched by a `BottomNav` composable inside an `EcomShell` that hosts the inner back stack; the shell is itself a single NavEntry so tab state survives.

**Gate branch** — `EcomRoute.Storefront` resolves on entry:

```
Storefront(slug)
  → bootstrap GET /store/{slug}            (STOREFRONT_NOT_FOUND → "store not available")
  → accessMode = LOGIN_FIRST?
       yes → token present?
                no  → AuthRoute.Phone (reused) → on success, re-resolve
                yes → hasStoreAccess(slug)?
                          yes → Browse
                          no  → RequestAccess → PendingAccess → (granted) → Browse
       no (GUEST_FIRST) → Browse directly   (cart works as guest; login deferred to checkout)
```

`accessMode` comes from `StorefrontConfig` (default `GUEST_FIRST`), overridable per storefront (e.g. backend `storefront.access_mode`, falling back to a local DataStore flag for the demo toggle). This is the single switch that satisfies the "Both (gate as a toggle)" decision.

---

## 5. Design-system mapping (prototype → Compose)

The prototype's tokens are the Ampairs M3 tokens we already ship; **use `MaterialTheme.colorScheme`, do not hardcode hex.** Mapping of the prototype CSS variables to Compose:

| Prototype token | Compose | Notes |
|---|---|---|
| `--primary #7026B5` | `colorScheme.primary` | violet, brand |
| `--secondary-container #FFDDB4` / `--on-secondary-container` | `secondaryContainer` / `onSecondaryContainer` | active chips, delivery strip, status "processing" |
| `--tertiary-container #E0E0FF` | `tertiaryContainer` | "confirmed" status chip, account note, avatar |
| `--surface-container*` ramp | `surfaceContainer*` | cards, search box, tiles |
| `#C8EAD0 / #0d4f1c` (delivered/savings) | bespoke `SuccessGreen` token pair in `components/EcomColors.kt` | not in M3 scheme; define once |
| Roboto / Roboto Mono | bundled fonts (`Res.font`); prices use mono | already shipped per `colors_and_type.css` |
| `--corner-*`, `--space-*` | `RoundedCornerShape`, `Dp` constants in `EcomDimens.kt` | xs4/sm8/md12/lg16/xl28 |

**Component inventory (from `ecom.css`)** to build as reusable composables in `ui/components/`:

- `EcomSearchBox` (pill, surface-container-high) · `CategoryChip` (active = secondary-container) · `BottomNav` (3 tabs + cart badge, secondary-container pill on active).
- `ProductGridCard` — 1:1 thumb (Coil `AsyncImage`, icon placeholder fallback), brand caps / 2-line name / mono price + strikethrough MRP / unit, and the **quick-add control** `QuickAdd` that flips `ADD` (outlined primary) ⇄ `– qty +` stepper inline; disabled + "Out of stock" overlay when `stock_status == OUT_OF_STOCK`; "LIMITED" flag top-left.
- `StickyCartBar` — appears when cart non-empty: "N items · ₹total → View Cart" (primary fill, shadow-3).
- Cart screen pieces: `DeliveryStrip` (secondary-container), `SavingsBanner` (success green), `Bill` (item total MRP / product discount / FREE delivery / **To pay**), `CartLineStepper`.
- Checkout: `Steps` (Address→Review→Place dots), `AddressCard` (selectable radio), `OutlinedField`, `ReviewLine`.
- Order: `ConfirmCheck` (88dp primary-container circle), `RefPill` (mono), `StatusChip` (placed/confirmed/processing/dispatched/delivered/review/cancelled — exact backgrounds from CSS), `Timeline` (rail + nodes, done/current states), `OrderCard`.
- Account: `AcctHero` (avatar + name + phone/email), `AcctNote` (cross-store note FR-021), `AcctRow` list, `SignInCard` (signed-out).
- Gate: `AuthLogo` (mark + "Powered by Ampairs"), `StoreIdStrip`, `GateStatus` (lock/pending badge), `PhoneField` + `OtpBoxes` (reused from auth styling).

All user-visible text → `strings.xml` → `ampairsapp.feature.ecom.generated.resources.*`.

---

## 6. Screens (Direction A) ↔ contract endpoints

| # | Screen | Key API calls | Notes |
|---|---|---|---|
| 1 | **LoginGate** (login-first only) | reuse auth `POST /auth/v1/...` | phone+OTP via `feature/auth`; then access check |
| 2 | **RequestAccess / Pending** (login-first only) | `POST /ecom/account/store-access` + `GET …/store-access/{slug}` *(future API — see §11.1)* | store identity strip + your number; poll for grant |
| 3 | **BrowseHome** | `GET /store/{slug}` (bootstrap), `GET /store/{slug}/catalog-meta` | category tiles (3-col), brand row, popular grid, search box |
| 4 | **DrillDown / Search results** | `GET /store/{slug}/products?category=&brand=&subcategory=`, `…/products/search?q=` | subcategory refine rail, removable filter chips, paged grid (Paging3 wrapper) |
| 5 | **ProductDetail** | `GET /store/{slug}/products/{id}` | image, price/MRP/savings, stock, quick-add, sticky action bar |
| 6 | **Cart** (full-screen) | `GET/POST/DELETE /store/{slug}/cart/{token}[/items]` | delivery strip, steppers (cap at stock), savings, bill, checkout bar |
| 7 | **Checkout** | pre-validate via `GET …/products/{id}` per item, then `POST …/cart/{token}/checkout` | Address→Review→Place; guest-first triggers login here (FR-012) then `claim` |
| 8 | **OrderPlaced** | response of checkout | confirm check + order ref pill + Track button |
| 9 | **OrderTracking** | `GET /ecom/account/orders/{ref}?storefrontSlug=` | timeline incl. "Pending Merchant Review" (FR-017) |
| 10 | **OrdersList** | `GET /ecom/account/orders?storefront_slug=&page=` | status filter tabs, order cards |
| 11 | **Account** | from token/profile | hero, cross-store note, links, logout |
| 12 | **Addresses** | `GET/POST/PUT/DELETE /ecom/account/addresses` | list + add/edit sheet, default toggle |

**Cart session** — `POST /store/{slug}/cart` once per visit → store `session_token` in DataStore key `ecom_cart_session_{slug}`. On login at checkout → `POST …/cart/{token}/claim`, replace stored token with the merged one. Quantity add is capped client-side at `stock_quantity` with a toast; server is authoritative (handle `INSUFFICIENT_STOCK` 422, `CART_EXPIRED` 410 → recreate cart, `PRODUCT_UNAVAILABLE` 422 → remove + notify).

---

## 7. Room schema (from contract §8)

`EcomRoomDatabase` (version 1, `@ConstructedBy(EcomRoomDatabaseConstructor::class)`) with entities mirroring the contract DDL — Kotlin/Room types in parentheses:

- `StorefrontEntity` (uid PK, slug, name, logoUrl?, bannerUrl?, status, cachedAt:Long)
- `TaxonomyImageEntity` (uid PK, storefrontId, type CATEGORY|SUBCATEGORY|BRAND, name, imageUrl, sortOrder)
- `ListedProductEntity` (uid PK, storefrontId, managementProductId, name, brand?, category?, subcategory?, unit?, price:Double, mrp:Double?, stockStatus, stockQuantity:Int, imageUrls:String JSON, description?, isVisible:Int=1, updatedAt?) + indices on `(storefrontId,isVisible)`, `(storefrontId,category)`, `(storefrontId,brand)`
- `SyncCursorEntity` (storefrontId PK, nextSince:String, syncedAt:Long)
- `CartEntity` (uid PK, storefrontId, sessionToken UNIQUE, status, expiresAt)
- `CartItemEntity` (uid PK, cartId, listedProductId, managementProductId, productName, brand?, unit?, unitPrice:Double, mrpAtAdd:Double?, quantity:Int, primaryImageUrl?)
- `CustomerAddressEntity` (uid PK, label?, line1, line2?, city, state, pinCode, country="IN", phone?, isDefault:Int=0)
- `EcomOrderEntity` (uid PK, ecomOrderRef UNIQUE, storefrontId, status, subtotal:Double, totalAmount:Double, notes?, placedAt, confirmedAt?, deliveryAddress:String JSON)
- `EcomOrderLineItemEntity` (uid PK, orderUid, listedProductId, productName, unitPrice:Double, quantityOrdered:Int, quantityConfirmed:Int?, lineTotal:Double, status)

DAOs expose reactive `Flow` reads (`observeVisibleProducts(storefrontId)`, `observeCart`, `observeOrders`, `observeAddresses`) for UI and `suspend` upserts for sync. `image_urls` stored as JSON string via a Room `TypeConverter` (kotlinx.serialization).

**Scoping**: the DB is created per **storefront slug** through `WorkspaceAwareDatabaseFactory` with `moduleName = "ecom_{slug}"` (Android flat file `workspace_{slug}_ecom.db`-style; iOS/Desktop directory form), registered in `DatabaseScopeManager` keyed `{slug}:ecom`. Switching storefronts swaps the DB exactly like switching workspaces.

---

## 8. Offline sync (data/sync integration)

Add to `SyncEntity`: `ECOM_PRODUCT("ecom_product")`, `ECOM_ADDRESS("ecom_address")`, `ECOM_ORDER("ecom_order")`. (Cart is **not** a sync entity — online-only with optimistic local mirror per contract §9.)

| Delegate | pull | push |
|---|---|---|
| `EcomCatalogSyncDelegate` (`@SyncEntityKey(ECOM_PRODUCT)`) | **cursor-based incremental**: read `SyncCursorEntity.nextSince`, loop `GET …/products/sync?since=&page=` while `has_more`; upsert by uid; `is_visible=false` → set `isVisible=0` (don't delete); store `next_since`. First run seeds via paged `GET …/products`. | no-op (catalog is read-only on client) |
| `EcomAddressSyncDelegate` (`@SyncEntityKey(ECOM_ADDRESS)`) | `GET /ecom/account/addresses` → upsert | push rows `synced=0`: POST/PUT/DELETE, then mark synced (standard offline-first CRUD) |
| `EcomOrderSyncDelegate` (`@SyncEntityKey(ECOM_ORDER)`) | `GET /ecom/account/orders?storefront_slug=` → upsert order + line items | no-op (orders are placed via the live checkout call, not push-sync) |

Triggers per contract §9: app foreground → background incremental catalog sync; pull-to-refresh → foreground; **before checkout** → live `GET …/products/{id}` per cart item (never trust cached stock); after order placed → refresh orders. ViewModels call `syncService.markPendingPush(ECOM_ADDRESS)` after address writes; catalog/order refresh via `syncService.emit(TriggerPull(...))`. Repos follow the offline-first rules in `.claude/skills/offline-sync` (fail-fast local writes, `Result.failure` when all pushes fail).

---

## 9. DI (Metro)

- **commonMain** `di/EcomModule.kt` — `@ContributesTo(AppScope::class) interface EcomDaoModule { companion object { @Provides fun … dao(db: EcomRoomDatabase) = db.xDao(); @Provides fun storefrontService(impl: StorefrontRepository): EcomStorefrontService = impl } }`.
- **platform** `EcomModule.android/ios/desktop.kt` — `@Provides @SingleIn(AppScope::class) fun provideEcomDatabase(factory: WorkspaceAwareDatabaseFactory, …): EcomRoomDatabase` (Android takes `Context`). Note the DB is slug-scoped, so the provider resolves the active slug from `EcomSessionStore` before calling the factory.
- Repositories & sync delegates: `@Inject` classes (unscoped, safe across slug switches). Delegates also `@ContributesIntoMap(AppScope::class) @SyncEntityKey(...)`.
- `EcomApiImpl`: `@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)` over `EcomApi`, building `httpClient(engine, tokenRepository)`.
- ViewModels: `@Inject @ContributesIntoMap(AppScope::class) @ViewModelKey` (plain) or `@AssistedInject` + inner `Factory` for id-carrying screens (ProductDetail, OrderTracking, DrillDown). Screens use `metroViewModel()` / `assistedMetroViewModel<VM, VM.Factory>(key=id){ create(id) }`.

ViewModels (MVI: `StateFlow<UiState>` + `SharedFlow<Event>`): `StorefrontGateViewModel`, `BrowseViewModel`, `DrillDownViewModel` (Paging), `ProductDetailViewModel`, `CartViewModel`, `CheckoutViewModel`, `OrderPlacedViewModel`, `OrdersViewModel`, `OrderTrackingViewModel`, `AccountViewModel`, `AddressesViewModel`.

---

## 10. Phased roadmap (after this plan is approved)

1. **Scaffold & wire** — create both modules, build files, `settings.gradle`, `ApiUrlBuilder.storeUrl/ecomUrl`, `EcomRoute`, `ecomEntryProvider` (placeholder screens), `SyncEntity` additions. Gate: all 3 targets compile.
2. **API + domain (ecom-api)** — DTOs with `@SerialName(snake_case)`, `EcomApi`/`EcomApiImpl`, `Response<T>` null-checks, error-code mapping.
3. **Room + repositories** — entities/DAOs/DB, slug-scoped factory, `DatabaseScopeManager`, repositories (offline-first writes).
4. **Sync** — three delegates + cursor logic; verify foreground/pull-to-refresh/pre-checkout.
5. **Theme + components** — `EcomColors/Dimens`, all reusable composables from §5.
6. **Screens vertical slice** — bootstrap → BrowseHome → DrillDown → ProductDetail → quick-add.
7. **Cart + checkout** — full-screen cart, bill, checkout steps, guest→login claim, stock pre-validation.
8. **Orders + account + addresses** — tracking timeline, order list, account, address CRUD.
9. **Gate** — login-first branch + RequestAccess/Pending, `accessMode` config.
10. **Polish + validate** — strings, a11y (`contentDescription`), `collectAsStateWithLifecycle`, run the 3 compile gates after each commonMain change.

**Compile gates** (run throughout):
```bash
./gradlew shared:compileKotlinIosSimulatorArm64
./gradlew androidApp:compileDebugKotlinAndroid
./gradlew desktopApp:compileKotlin
```

---

## 11. Resolved decisions & remaining risks

**Resolved (confirmed by product owner):**

1. **Store-access flow → build against a defined future API.** The `LOGIN_FIRST` gate targets a contract the backend `ecom` module will add. Assumed shape (mobile drives the UI to this; backend implements to match):
   - `POST /ecom/account/store-access` — body `{ "storefront_slug": "green-mart" }`, auth required. Returns `{ "status": "PENDING" | "GRANTED" | "REJECTED", "requested_at": "…" }`.
   - `GET /ecom/account/store-access/{slug}` — auth required. Returns the same access-status object; `404 ACCESS_NOT_REQUESTED` if none. Drives the Pending screen's poll.
   - `GET /store/{slug}` (bootstrap) gains `"access_mode": "GUEST_FIRST" | "LOGIN_FIRST"` and, for authed callers, `"viewer_access": "GRANTED" | "PENDING" | "NONE"` so the gate resolves in one round-trip.
   - Until the backend ships these, the repository's `StoreAccessApi` is feature-flagged to a stub that auto-grants (demo), behind the same interface — no UI rework when the real endpoint lands.
2. **Surface → standalone customer storefront.** Ecom is opened by slug/deep-link with its own `EcomShell` (Browse/Orders/Account tabs), independent of the merchant workspace drawer. Not surfaced inside the merchant app navigation.
3. **Slug source (default, extensible)** — support an Android App Link / iOS Universal Link for `store.ampairs.com/{slug}` **and** a "last store" persisted in DataStore (`ecom_last_storefront_slug`). An in-app store picker can be layered on later without changing `EcomRoute.Storefront`.
4. **`accessMode` source (default, both)** — read `access_mode` from the `GET /store/{slug}` payload when present; otherwise fall back to a local DataStore flag (`ecom_access_mode_{slug}`) so the demo toggle works before the backend field exists.

**Remaining risks / minor confirms (non-blocking — sensible defaults assumed):**

5. **Profile data** — contract lacks `GET /ecom/account/profile`; Account hero (name/phone/email) is sourced from the auth user profile (`UserDataService` in `feature/auth-api`). Flag if a separate ecom profile is intended.
6. **Order status realtime** — tracking advances via polling (`GET …/orders/{ref}`) in v1; STOMP/Krossbow push is available in the stack if low-latency updates are wanted later.

---

## 11a. Phase 1 status (scaffold landed)

Phase 1 (scaffold & wire) is implemented:

- `feature/ecom-api` — all contract DTOs (`Storefront`, `CatalogMeta`, `ListedProduct`, `ProductSync*`, `Cart*`, `Address*`, `EcomOrder*`, `StoreAccess*`) + enums.
- `feature/ecom` — `EcomApi` + `EcomApiImpl` (Ktor, slug/account URLs), full Room layer (9 entities, 7 DAOs, `EcomRoomDatabase` v1), repositories (storefront, catalog w/ cursor sync, cart, address push/pull, orders, store-access stub), 3 sync delegates, Metro DI (common DAO module + android/ios/desktop DB providers), the `StorefrontGateViewModel` (assisted) + `EcomStorefrontScreen`, and `strings.xml`.
- Shared wiring — `EcomRoute`, `ecomEntryProvider` chained in `CombinedEntryProvider`, `api(projects.feature.ecom)` in `shared`.
- Infra — `ApiUrlBuilder.ecomUrl`/`storeUrl`, `SyncEntity.ECOM_PRODUCT/ECOM_ADDRESS/ECOM_ORDER`, both modules in `settings.gradle.kts`.

**Compile gate not run in CI sandbox.** The KMP plugin (2.3.21) requires a JetBrains-vendor JDK 21 toolchain; this container has only OpenJDK and the network policy blocks `api.foojay.io` toolchain provisioning (403), and the repo's `org.gradle.java.home` points at a macOS path. Code was written by mirroring `feature/product`/`product-api` exactly. **Run locally before merging:**
```bash
./gradlew :feature:ecom:compileKotlinDesktop
./gradlew shared:compileKotlinIosSimulatorArm64
./gradlew androidApp:compileDebugKotlinAndroid
```

## 12. What is explicitly out of scope (v1)

Merchant-management screens (create/publish storefront, list products, Pending-Merchant-Review edit/confirm), Direction B, payments, delivery fee/ETA, split-shipment UI (data model supports it per FR-029), Desktop-specific UX (compiles only).
