---
name: cmp-practices
description: "Compose Multiplatform best practices and anti-patterns guide. Use when writing, reviewing, or debugging Compose Multiplatform / KMP UI code — covers state, recomposition, side effects, MVI, platform parity, and performance."
trigger: /cmp-practices
---

# /cmp-practices — Compose Multiplatform Best Practices

> **Version baseline (as of May 2025):** CMP 1.11.0 (stable) / 1.12.0-alpha01 (cutting edge) · Ktor 3.5.0 latest (project on 3.3.2) · coroutines 1.10.2 · Kotlin 2.3.21 · Navigation3 1.0.0-alpha06

When invoked, apply this knowledge actively while writing or reviewing code. Flag every anti-pattern encountered. Do not skip sections that are relevant to the current file.

---

## Reference Index

Consult these files for deeper guidance. They are not auto-loaded — read them when the topic is directly relevant.

| Topic | File | When to use |
|---|---|---|
| MVI state modeling, domain layer, where logic belongs | `references/architecture.md` | Architecture questions, new screen design |
| Cross-cutting anti-patterns with BAD/GOOD examples | `references/anti-patterns.md` | Code review, spotting violations |
| Navigation3 — scenes, adaptive layouts, tabs, deep links | `references/navigation-3.md` | Adding routes, dialogs, adaptive layouts |
| Coroutines & Flow — operators, dispatchers, stateIn | `references/coroutines-flow.md` | Flow chains, scope, StateFlow setup |
| Recomposition deep dive — three phases, API decision table | `references/performance.md` | Performance issues, stability problems |
| Ktor HttpClient setup, DTOs, repository pattern | `references/networking-ktor.md` | New API endpoints, networking setup |
| JWT Bearer auth, WebSocket, SSE | `references/networking-ktor-auth.md` | Auth setup, real-time features |
| KMP placement guide, interface vs expect/actual | `references/cross-platform.md` | Any commonMain API decision |
| Compose resources — Res vs R, fonts, MVI integration | `references/resources.md` | Strings, drawables, plurals, fonts |
| Three phases, modifier ordering, slot pattern, CompositionLocal | `references/compose-essentials.md` | Core Compose fundamentals |
| SKIE, Flow→Swift, UIKitView, Swift interop | `references/ios-swift-interop.md` | iOS interop, SKIE setup |
| MVI discipline, naming, anti-overengineering | `references/clean-code.md` | Code review, ViewModel design |
| RemoteMediator, offline-first paging | `references/paging-offline.md` | Paged lists with Room + Ktor |
| Metro DI — ViewModel, Repository, platform modules | `references/metro-di.md` | Any new screen, ViewModel, or feature module |

---

## 1. State and Recomposition

### DO

```kotlin
// Hoist state — composable is stateless, caller owns the value
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) { ... }

// remember for expensive objects that must survive recomposition
val painter = remember { Painter(...) }

// rememberSaveable for state that must survive process death / config change
var expanded by rememberSaveable { mutableStateOf(false) }

// derivedStateOf — compute only when upstream state changes
val isButtonEnabled by remember { derivedStateOf { email.isNotEmpty() && password.length >= 8 } }

// key() inside LazyColumn to preserve identity across reorders
LazyColumn {
    items(list, key = { it.id }) { item -> ItemRow(item) }
}
```

### AVOID

```kotlin
// ❌ State defined inside a composable that should be hoisted
@Composable fun SearchBar() {
    var query by remember { mutableStateOf("") }  // caller can't control this
}

// ❌ Reading state inside lambda breaks smart recomposition
Text(text = viewModel.uiState.value.name)   // reads .value inside composition

// ❌ Recomputing on every recomposition
val filtered = list.filter { it.active }   // move inside remember { derivedStateOf { } }

// ❌ Using List instead of SnapshotStateList for observed collections
var items = mutableListOf<Item>()   // Compose won't observe changes
```

---

## 2. Stability and Skippability

Compose skips recomposing a composable only when all parameters are **stable**. Instability causes unnecessary recompositions.

### Stability rules

| Type | Stable? | Fix |
|---|---|---|
| `Int`, `String`, `Boolean`, primitives | ✅ | — |
| `data class` with only stable fields | ✅ (Kotlin 2+) | — |
| `List<T>`, `Map<K,V>` | ❌ (mutable contract) | Use `ImmutableList` from `kotlinx.collections.immutable` or `@Immutable` |
| Lambdas referencing unstable captures | ❌ | Wrap in `remember { }` or use stable ViewModel reference |
| Classes with `var` fields | ❌ | Use `@Stable` annotation only when you guarantee observable mutations |

```kotlin
// ✅ Stable data class
@Immutable
data class CustomerUiState(val name: String, val email: String)

// ✅ Stable lambda via remember
val onClick = remember(id) { { onItemClick(id) } }

// ❌ Unstable lambda captures mutable var
val onClick = { onItemClick(counter) }   // counter changes → new lambda → recomposition
```

### Verify with compiler reports

```bash
# In build.gradle.kts
kotlinOptions { freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=build/compose_reports") }
./gradlew assembleDebug
```

---

## 3. Side Effects

Use the correct effect API — wrong choice causes leaks, double-execution, or missed cancellation.

| Need | API |
|---|---|
| Launch coroutine tied to composable lifecycle | `LaunchedEffect(key)` |
| Subscribe/unsubscribe to external source | `DisposableEffect(key)` |
| Push value to non-Compose code after recomposition | `SideEffect { }` |
| Produce state from suspend fun | `produceState` |
| Share reusable coroutine scope across composables | `rememberCoroutineScope()` |

```kotlin
// ✅ LaunchedEffect restarts only when key changes
LaunchedEffect(userId) {
    viewModel.loadUser(userId)
}

// ✅ DisposableEffect cleans up properly
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> ... }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}

// ❌ Coroutine launched directly in composition (no lifecycle tie)
viewModel.load()    // called every recomposition

// ❌ LaunchedEffect(Unit) for something that should re-run on key change
LaunchedEffect(Unit) { viewModel.loadUser(userId) }   // won't re-run if userId changes
```

---

## 4. MVI Pattern (this project's pattern)

The project uses **MVI** with `StateFlow<UiState>` + `Channel<UiEffect>` (one-off effects). See `references/architecture.md` and `references/coroutines-flow.md` for the full pattern.

```kotlin
// ViewModel
class CustomerListViewModel(private val repo: CustomerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CustomerListEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CustomerListEvent> = _events.asSharedFlow()

    fun onIntent(intent: CustomerListIntent) { ... }
}

// Screen — collectAsStateWithLifecycle (not collectAsState)
@Composable
fun CustomerListScreen(viewModel: CustomerListViewModel = metroViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CustomerListEvent.NavigateTo -> { ... }
                is CustomerListEvent.ShowError -> { ... }
            }
        }
    }
}
```

### Anti-patterns in MVI

```kotlin
// ❌ UI calling repository directly — breaks the MVI boundary
val customers = repo.getCustomers()   // inside composable

// ❌ Business logic in composable
if (user.role == "admin") showAdminPanel()   // logic belongs in ViewModel/state

// ❌ collectAsState() without lifecycle awareness — causes emissions while backgrounded
val state by viewModel.uiState.collectAsState()   // use collectAsStateWithLifecycle()

// ❌ Emitting to SharedFlow with no buffer — drops events if collector is slow
val _events = MutableSharedFlow<Event>()   // add extraBufferCapacity = 1
```

---

## 5. KMP Platform Compatibility in Compose

### What breaks on non-JVM targets (iOS / Wasm)

```kotlin
// ❌ Context in commonMain — Android-only
LocalContext.current   // not available on iOS/Desktop
context.getSystemService(...)

// ❌ Android-specific Compose APIs in commonMain
rememberLauncherForActivityResult(...)
BackHandler { }   // Android only — use expect/actual

// ❌ Java APIs in commonMain
val fmt = SimpleDateFormat("yyyy-MM-dd")   // JVM only
System.currentTimeMillis()                 // use Clock.System.now()

// ✅ Platform-specific UI via expect/actual
// commonMain
expect @Composable fun BackNavigationHandler(onBack: () -> Unit)
// androidMain
actual @Composable fun BackNavigationHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
// iosMain — no-op or side drawer dismiss
actual @Composable fun BackNavigationHandler(onBack: () -> Unit) { }
```

### Dispatchers on iOS

`Dispatchers.IO` is available on all KMP targets since `kotlinx.coroutines 1.7+`. This project uses 1.10.2, so it's safe everywhere. The old `Dispatchers.Default` workaround is no longer needed.

```kotlin
// ✅ Safe in commonMain — coroutines 1.10.2
withContext(Dispatchers.IO) { ... }
```

---

## 6. Performance

### Lazy lists

```kotlin
// ✅ Always provide stable keys
LazyColumn {
    items(customers, key = { it.uid }) { customer ->
        CustomerRow(customer = customer)
    }
}

// ✅ contentType for heterogeneous lists — skips rebinding wrong view types
LazyColumn {
    items(feed, key = { it.id }, contentType = { it::class }) { ... }
}

// ❌ index as key — breaks animations and state preservation on reorder
items(list, key = { index, _ -> index }) { ... }
```

### Image loading (Coil 3)

```kotlin
// ✅ AsyncImage with size constraint to avoid loading full-res into thumbnails
AsyncImage(
    model = ImageRequest.Builder(LocalPlatformContext.current)
        .data(url)
        .size(64.dp.toPx().toInt())   // avoids decoding a 4K image for a 64dp slot
        .build(),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(64.dp)
)

// ❌ No size hint — decodes full resolution unnecessarily
AsyncImage(model = url, contentDescription = null)
```

### Modifier order matters

```kotlin
// ✅ clickable before padding — touch target is larger
Modifier.clickable { }.padding(8.dp)

// ❌ padding before clickable — only the inner padded area responds to taps
Modifier.padding(8.dp).clickable { }
```

### Avoid allocation in composition

```kotlin
// ❌ New object created every recomposition
Text(style = TextStyle(color = Color.Red))   // allocates TextStyle each time

// ✅
val errorStyle = remember { TextStyle(color = Color.Red) }
Text(style = errorStyle)

// ❌ Lambda captures non-stable ref
items.forEach { item ->
    Button(onClick = { onItemClick(item) }) { ... }   // new lambda per item per recomposition
}
// ✅ key-scoped or stable lambda
```

---

## 7. Navigation3 (this project)

This project uses **Navigation3** (androidx.navigation3, alpha). Routes implement `NavKey` via `@Serializable sealed interface`.

```kotlin
// ✅ Correct route definition
@Serializable data object CustomerList : CustomerRoute
@Serializable data class CustomerDetail(val uid: String) : CustomerRoute

// ✅ Entry provider pattern
class CustomerEntryProvider : NavEntryProvider {
    override fun NavEntry.Companion.provideEntry(key: NavKey): NavEntry? = when (key) {
        is CustomerRoute.CustomerList -> NavEntry(key) { CustomerListScreen() }
        is CustomerRoute.CustomerDetail -> NavEntry(key) { CustomerDetailScreen(uid = key.uid) }
        else -> null
    }
}

// ❌ Old NavController.navigate() — does not exist in Navigation3
navController.navigate(CustomerRoute.CustomerList)

// ❌ Passing ViewModel from entry provider to screen
NavEntry(key) { vm ->   // wrong — Metro creates ViewModels, not the entry provider
    CustomerListScreen(viewModel = vm)
}
```

---

## 8. Accessibility

```kotlin
// ✅ Always provide contentDescription for icon buttons
IconButton(onClick = { ... }) {
    Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
}

// ✅ Use semantics for custom components
Box(
    modifier = Modifier.semantics {
        contentDescription = "Customer card: ${customer.name}"
        role = Role.Button
    }
)

// ❌ No description on interactive elements
Icon(Icons.Default.Delete, contentDescription = null)   // screen readers silent
```

---

## 9. String Resources (KMP rule)

All user-visible strings must come from Compose resources — never hardcoded.

```kotlin
// ✅ Composable context
Text(text = stringResource(Res.string.customer_list_title))

// ✅ Non-composable suspend context (e.g. androidMain service)
val msg = getString(Res.string.error_network)   // suspend, call before suspendCancellableCoroutine

// ❌ Hardcoded string
Text(text = "Customer List")

// ❌ Android R.string — KMP android library plugin doesn't generate R class
context.getString(R.string.customer_list_title)
```

Strings go in: `feature/{name}/src/commonMain/composeResources/values/strings.xml`
Import: `import ampairsapp.{module.path}.generated.resources.*`

### Publishing + Compose Resources — pin `packageOfResClass`

When a module that has a `composeResources/` directory (i.e. source files import `ampairsapp.*`) is given `group = "..."` for Maven publishing, CMP shifts the auto-derived accessor package away from the project-path-based name. This breaks all existing resource imports at compile time.

**Fix: always add `compose.resources { packageOfResClass }` whenever adding `maven-publish` to a module that uses Compose resources.**

```kotlin
// build.gradle.kts — required whenever group = "..." is set on a module with composeResources
group = "com.ampairs"
version = "1.0.0"

compose.resources {
    // Pins the accessor package so the publishing group can't shift it.
    // Package pattern: ampairsapp.{module:path:with:dots}.generated.resources
    // Examples:
    //   :data:common       → "ampairsapp.data.common.generated.resources"
    //   :feature:auth      → "ampairsapp.feature.auth.generated.resources"
    //   :feature:customer  → "ampairsapp.feature.customer.generated.resources"
    packageOfResClass = "ampairsapp.{module.path}.generated.resources"
}
```

How to derive the package for any module: take the Gradle project path (e.g. `:feature:customer`), drop the leading colon, replace remaining colons with dots, append `.generated.resources` → `ampairsapp.feature.customer.generated.resources`.

**Check before adding publishing to any module:**
1. Does `src/commonMain/composeResources/` exist? → pin required
2. Do any source files import `ampairsapp.*`? → pin required
3. Neither → no pin needed

---

## 10. Theming

```kotlin
// ✅ Use MaterialTheme tokens — never hardcode colors
Text(color = MaterialTheme.colorScheme.onSurface)
Surface(color = MaterialTheme.colorScheme.surfaceVariant)

// ✅ Project theme access — via CompositionLocal in App.kt (Metro-provided)
PlatformAmpairsTheme(darkTheme = themeManager.isDarkTheme())

// ❌ Hardcoded color values
Text(color = Color(0xFF333333))

// ❌ Accessing AppGraph inside composable
LocalAppGraph.current.themeManager   // NEVER — use CompositionLocal or inject via ViewModel
```

---

## 11. Checklist Before Committing Compose Code

- [ ] No `LocalContext.current` in commonMain
- [ ] No `BackHandler` / Android-only APIs in commonMain (use expect/actual)
- [ ] All user-visible strings use `stringResource(Res.string.xxx)`
- [ ] State hoisted to appropriate level; composables are stateless where possible
- [ ] LazyList items have stable `key =` parameter
- [ ] `collectAsStateWithLifecycle()` — not `collectAsState()` — for Flow collection
- [ ] Side effects use correct API (LaunchedEffect / DisposableEffect / SideEffect)
- [ ] No business logic inside composables — pure UI rendering only
- [ ] All `IconButton` / interactive elements have `contentDescription`
- [ ] No hardcoded color values — use `MaterialTheme.colorScheme.*`
- [ ] No `LocalAppGraph.current` inside any `@Composable`
- [ ] If adding `maven-publish` to a module: check for `composeResources/` or `ampairsapp.*` imports — if present, add `compose.resources { packageOfResClass = "ampairsapp.{module.path}.generated.resources" }`
- [ ] Compile all 3 targets after any commonMain change:
  ```bash
  ./gradlew androidApp:compileDebugKotlinAndroid
  ./gradlew shared:compileKotlinIosSimulatorArm64
  ./gradlew desktopApp:compileKotlin
  ```

---

## 12. Currency & Locale Formatting (workspace business locale)

Money and (soon) dates render using the **active workspace's business locale** — currency, timezone,
date/time format from the business profile — never hardcoded. Storage/sync stay UTC; this is a
*display-only* concern.

### How it flows

```
business profile (timezone/date_format/time_format/currency)
  → BusinessLocaleProvider (@SingleIn(WorkspaceScope::class), feature/business) : Flow<AppLocale>
  → exposed on WorkspaceGraph.businessLocaleProvider
  → AppNavigationNav3 collects it from the ACTIVE workspace graph and provides LocalAppLocale
  → any @Composable reads LocalAppLocale.current  (defaults to AppLocale.Default = INR/UTC)
```

Sourcing from `workspaceSession.graph` (recreated per workspace) means the locale is never stale
after a workspace switch — do **not** resolve it via a root-level ViewModel.

### Money — `com.ampairs.common.locale`

```kotlin
// ✅ business-currency aware (INR keeps Indian grouping identical to the old toInr())
val locale = LocalAppLocale.current
Text(formatMoney(amount, locale))            // "₹9,20,710.50" / "$1,234.00" / ...
Text(currencySymbol(locale.currencyCode))    // just the symbol, for toggle labels etc.

// ❌ hardcoded currency — banned in UI
Text("₹$amount")
Text(amount.toInr())     // legacy; data/common — do not call in UI
Text(amount.asRupee())   // legacy; ecom — do not call in UI
```

- `formatMoney(amount: Double?, locale)` / `formatMoney(amount, currencyCode)` — symbol + grouping, 2 dp, null → "".
- Read `LocalAppLocale.current` only in `@Composable` scope (it's a CompositionLocal). A non-composable
  string builder (e.g. print HTML) must take a `currencySymbol: String` parameter passed from the
  calling composable — see `buildInvoiceHtml(...)`.

### Dates (forthcoming slice)

`AppLocale.timeZoneId` / `dateFormat` / `timeFormat` exist but date formatters are still being
migrated. Until then, prefer `DateTimeFormatter`; the timezone-aware variants will also read
`LocalAppLocale`. Note the **computation** trap: bucketing an `Instant` to a calendar day/month with
`TimeZone.currentSystemDefault()` is wrong when the business timezone differs — use the business
`timeZoneId`.

---

## Quick Anti-Pattern Reference

| Anti-pattern | Fix |
|---|---|
| `collectAsState()` | `collectAsStateWithLifecycle()` |
| `LocalContext.current` in commonMain | expect/actual or pass as param |
| `BackHandler` in commonMain | expect/actual `BackNavigationHandler` |
| `Dispatchers.IO` before coroutines 1.7 | Safe since 1.7+ — project uses 1.10.2, no issue |
| Hardcoded string in `Text()` | `stringResource(Res.string.xxx)` |
| `LocalAppGraph.current` in composable | Metro-injected ViewModel |
| Business logic in composable | Move to ViewModel intent handler |
| LazyList without `key` | `items(list, key = { it.id })` |
| Unstable lambda in recomposition | `remember(key) { { ... } }` |
| `List<T>` param causing instability | `@Immutable` wrapper or `ImmutableList` |
| State in composable that caller needs | Hoist state up |
| `derivedStateOf` outside `remember` | `remember { derivedStateOf { } }` |
| `SideEffect` for async work | `LaunchedEffect` |
| `LaunchedEffect(Unit)` for key-dependent work | `LaunchedEffect(theKey)` |
| Padding before clickable | `clickable { }.padding(...)` |
| New object allocation in composition | `remember { }` |
| Hardcoded `₹` / `toInr()` / `asRupee()` in UI | `formatMoney(amount, LocalAppLocale.current)` (see §12) |
| `maven-publish` added, resource imports broken | Add `compose.resources { packageOfResClass = "ampairsapp.{module.path}.generated.resources" }` |
