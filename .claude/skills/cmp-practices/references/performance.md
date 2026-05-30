# Performance & Recomposition

## Three Phases

Every frame: **Composition → Layout → Drawing**. State reads in later phases skip earlier phases.

```kotlin
// ❌ Reads in Composition phase — triggers full recomposition on every scroll offset change
Box(modifier = Modifier.offset(scrollOffset.dp, 0.dp))

// ✅ Reads in Layout phase — skips Composition entirely
Box(modifier = Modifier.offset { IntOffset(scrollOffset.value.toInt(), 0) })

// ✅ Reads in Drawing phase — skips both Composition and Layout
Box(modifier = Modifier.graphicsLayer { alpha = animatedAlpha.value })
```

Use `mutableIntStateOf()` / `mutableFloatStateOf()` instead of `mutableStateOf<Int>()` to avoid boxing.

## Performance Mistakes and Fixes

| Issue | Fix |
|---|---|
| Unstable parameters (`MutableList`, lambdas in state, anonymous objects) | `@Immutable` data classes + immutable collections |
| Parent reads whole state, ripples through tree | Slice aggressively — pass only what each child renders |
| Expensive calculations in composition (parse, sort, filter, format) | Move upstream to ViewModel/domain |
| Callback recreation in hot paths (large lazy lists) | `remember(key) { { callback(key) } }` |
| `remember` misuse — caching business state | Only for local UI state, expensive local objects |
| `derivedStateOf` around cheap expressions | Use only when derived from rapidly changing Compose state |
| `rememberSaveable` for entire screen state | Only for tiny UI-local values surviving recreation |
| State reads too high in tree (scroll state, animation state) | Read close to use |
| Lazy list: missing keys, unstable items, inline filters/sorts | Stable keys, immutable models, pre-computed data |
| Reducer emits unchanged state | Guard identical transitions before `_state.update {}` |
| Ephemeral visual state (shimmer alpha, pulse) in screen state | Keep visual-only state local |
| Lambdas in data classes | Move callbacks out, use `remember(key)` wrappers |
| Raw text input causing stutter (25+ fields) | Group fields into nested data classes, isolate read scopes |

## API Decision Table

| API | Use it for | Do NOT use for |
|---|---|---|
| `remember` | local objects/state across recompositions | business state, repo results |
| `rememberSaveable` | small UI-local state needing restoration | whole screen state, domain objects |
| `derivedStateOf` | reducing updates from fast-changing Compose state | cheap string concat, ViewModel-owned derivations |
| `key` | preserving identity in dynamic children/lists | hiding bad state models |
| `LaunchedEffect` | collecting effects, startup, one-shot route work | business logic in leaf composables |
| `DisposableEffect` | register/unregister listeners with cleanup | long-running business jobs |
| `snapshotFlow` | turning Compose state reads into `Flow` operators | normal state rendering |
| `collectAsStateWithLifecycle` | top-level screen state collection | collecting everywhere in the tree |
| Stable callbacks | hot repeated UI paths (lazy lists) | every single callback everywhere |

## Code Examples

### BAD: calculating in composable

```kotlin
@Composable
fun OrderSummary(state: OrderState) {
    val subtotal = state.items.sumOf { it.price * it.qty }   // runs every recomposition
    val tax = subtotal * 0.18
    Text("Subtotal: $subtotal  GST: $tax")
}
```

### GOOD: derive upstream, narrow reads

```kotlin
data class OrderDerived(val subtotal: Double, val gst: Double, val total: Double)
data class OrderState(val items: List<LineItem>, val derived: OrderDerived)

@Composable
fun OrderSummary(derived: OrderDerived) {   // only recomposes when derived changes
    Text("Subtotal: ${derived.subtotal}  GST: ${derived.gst}")
}
```

### GOOD: immutable models + stable key + stable callback

```kotlin
@Immutable
data class CustomerRowUi(val uid: String, val name: String, val email: String)

@Composable
fun CustomerList(items: List<CustomerRowUi>, onOpen: (String) -> Unit) {
    LazyColumn {
        items(items, key = { it.uid }) { item ->
            val onClick = remember(item.uid, onOpen) { { onOpen(item.uid) } }
            CustomerRow(item = item, onClick = onClick)
        }
    }
}
```

### GOOD: guard identical transitions

```kotlin
private fun onSearchChanged(query: String) {
    if (_state.value.searchQuery == query) return   // no-op if unchanged
    _state.update { it.copy(searchQuery = query) }
}
```

### GOOD: correct `derivedStateOf`

```kotlin
val listState = rememberLazyListState()
val showScrollTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
```

### BAD: unnecessary `derivedStateOf`

```kotlin
// BAD — canSave is cheap; derived from non-Compose state; belongs in ViewModel
val canSave by remember { derivedStateOf { name.isNotBlank() && email.isNotBlank() } }
// ✅ Just use: val canSave = state.canSave (computed property on UiState)
```

## Deferred Reads Through Call Chain

Pass state as a lambda parameter to defer the read to the frame phase where it's actually used:

```kotlin
// Pattern 1: defer via lambda parameter
@Composable
fun Title(snack: Snack, scrollProvider: () -> Int) {
    Column(
        modifier = Modifier.offset { IntOffset(x = 0, y = scrollProvider()) }
    ) { /* ... */ }
}
// Caller passes lambda — read deferred to Layout phase
Title(snack) { scrollState.value }

// Pattern 2: drawBehind for rapidly changing draw-only state
val color by animateColorBetween(Color.Cyan, Color.Magenta)
Box(
    Modifier.fillMaxSize().drawBehind {
        drawRect(color)   // read in Draw phase — skips Composition and Layout
    }
)
```

## Backwards Writes — Strict Prohibition

Never write to state that was already read in the same composition — causes infinite recomposition loops.

```kotlin
// ❌ Backwards write — infinite loop
@Composable
fun Bad() {
    var count by remember { mutableIntStateOf(0) }
    Text("$count")
    count++   // write after read in same composition
}

// ✅ Only write in event handlers
Button(onClick = { count++ }) { Text("Increment") }
```

## Compiler and Build Optimizations

**Compose Compiler Reports** — use the new `composeCompiler {}` DSL (not the old `freeCompilerArgs` approach). Audit `restartable`/`skippable` on release builds:

```kotlin
// build.gradle.kts (module level)
android {
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}
```

Three files generated in `reportsDestination`:
- `{module}-classes.txt` — stability of classes
- `{module}-composables.txt` — restartable/skippable per composable
- `{module}-composables.csv` — for spreadsheet analysis

Example of a **non-skippable** composable (to fix):

```
restartable scheme("[...]") fun CustomerList(
  stable modifier: Modifier
  unstable customers: List<Customer>   ← causes non-skippable
)
```

Fix by wrapping in `@Immutable` or using `ImmutableList` from `kotlinx.collections.immutable`.

**Strong Skipping Mode** — allows composables with unstable params to skip based on instance equality (`===`):

```kotlin
android {
    composeCompiler {
        featureFlags = setOf(ComposeFeatureFlag.StrongSkipping)
    }
}
```

**Stability config file** — mark external stable classes without modifying their source:

```
// stability_config.conf
com.ampairs.common.model.*
kotlinx.datetime.Instant
kotlinx.collections.immutable.ImmutableList
```

```kotlin
android {
    composeCompiler {
        stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("stability_config.conf"))
    }
}
```
