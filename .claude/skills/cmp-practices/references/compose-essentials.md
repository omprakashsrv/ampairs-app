# Compose Essentials

## Three Phases Model

Every frame: **Composition → Layout → Drawing**

1. **Composition** — executes composable functions, evaluates state reads. State reads trigger recomposition of the entire scope.
2. **Layout** — calculates size and position. Can read state without triggering composition recomposition.
3. **Drawing** — emits draw ops, runs `Canvas`. Can read state without composition.

```kotlin
// ❌ Reads in Composition — triggers recomposition on every scroll change
Box(modifier = Modifier.offset(scrollOffset.dp, 0.dp))

// ✅ Reads in Layout phase — skips Composition
Box(modifier = Modifier.offset { IntOffset(scrollOffset.value.toInt(), 0) })

// ✅ Reads in Drawing phase — skips both
Box(modifier = Modifier.graphicsLayer { alpha = animatedAlpha.value })
```

## State Primitives

```kotlin
// Avoid boxing — use type-specific holders
val count = mutableIntStateOf(0)       // no boxing
val progress = mutableFloatStateOf(0f) // no boxing
val name = mutableStateOf("Alice")     // general-purpose

// SnapshotStateList — observable collection for UI-local state only
val items = remember { mutableStateListOf<Item>() }
items.add(Item(1, "First"))   // triggers recomposition
items[0].name = "Updated"     // does NOT trigger recomposition (in-place mutation)
```

In MVI, prefer immutable `List<T>` in state models. `mutableStateListOf` only for UI-local state.

## Side Effects

### LaunchedEffect

```kotlin
// Key = Unit: runs once when composable enters composition
LaunchedEffect(Unit) { viewModel.loadInitialData() }

// Key = specific value: reruns when value changes
LaunchedEffect(customerId) { viewModel.loadCustomer(customerId) }

// Multiple keys: reruns if ANY key changes
LaunchedEffect(workspaceId, customerId) { viewModel.loadCustomer(customerId) }
```

In MVI, `LaunchedEffect` belongs at the route level for collecting UI effects, not in leaf composables.

### DisposableEffect

```kotlin
DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event -> /* handle */ }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }  // always pair
}
```

### rememberCoroutineScope

```kotlin
val scope = rememberCoroutineScope()
Button(onClick = { scope.launch { scrollState.animateScrollTo(0) } }) { Text("Top") }
```

In MVI, prefer dispatching events to the ViewModel. `rememberCoroutineScope` only for UI-local async work (scroll animation, snackbar).

### rememberUpdatedState — Prevent Effect Restart on Value Change

When a long-lived effect uses a value that changes but shouldn't restart the effect:

```kotlin
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Always refers to latest onTimeout without restarting the delay
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(true) {   // Unit key — runs once for SplashScreen's lifetime
        delay(3000L)
        currentOnTimeout()   // latest lambda, no stale capture
    }
}
```

Use `rememberUpdatedState` when the effect is expensive to restart but must always call the latest version of a callback.

### collectAsStateWithLifecycle

```kotlin
// ✅ Lifecycle-aware — stops collecting when app is backgrounded
// Requires: androidx.lifecycle:lifecycle-runtime-compose:2.10.0+
val state by viewModel.state.collectAsStateWithLifecycle()

// ❌ Not lifecycle-aware — collects even when backgrounded
val state by viewModel.state.collectAsState()
```

### Side Effects API Summary

| API | Purpose | Restart on key change | Cleanup |
|---|---|---|---|
| `LaunchedEffect(key)` | Suspend work tied to composition | Yes | Auto-cancel |
| `rememberCoroutineScope()` | Manual coroutine from event handler | No (manual) | Auto-cancel on leave |
| `rememberUpdatedState(value)` | Latest value without restarting effect | No restart | — |
| `DisposableEffect(key)` | Register/unregister listeners | Yes | Required `onDispose` |
| `SideEffect { }` | Sync Compose state to non-Compose objects | Every composition | — |
| `produceState(initial)` | Convert external callback/flow to Compose state | Yes | `awaitDispose {}` |
| `snapshotFlow { }` | Convert Compose state reads to Flow | Emits on state change | Cold flow |

## Modifier Ordering

Modifiers apply left-to-right. Order changes the visual result.

```kotlin
// Red background wraps padded 100dp box
Modifier.background(Color.Red).padding(16.dp).size(100.dp)

// Padding is inside sized box, then background wraps
Modifier.size(100.dp).padding(16.dp).background(Color.Red)

// ✅ clickable before padding — larger touch target
Modifier.clickable { }.padding(16.dp)

// ❌ padding before clickable — only padded area responds
Modifier.padding(16.dp).clickable { }
```

### Always accept Modifier parameter

```kotlin
// ✅ Caller controls layout/sizing
@Composable
fun CustomerCard(customer: Customer, modifier: Modifier = Modifier) {
    Card(modifier = modifier) { /* ... */ }
}
```

## Slot Pattern

```kotlin
// ✅ Flexible, composable-aware slots
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            title()
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

SectionCard(
    title = { Text("Customer Info", style = MaterialTheme.typography.titleMedium) },
    content = { CustomerInfoForm(state, onEvent) },
)
```

## CompositionLocal — When to Use

**Use for:**
- Theming (`MaterialTheme`, colors, typography)
- Platform integration (`LocalDensity`, `LocalLifecycleOwner`)
- Infrequently changing cross-cutting concerns (`LocalFocusManager`)

**Do NOT use for:**
- Feature state — flows ViewModel → screen → leaves via explicit params
- Frequently changing values — causes widespread recomposition
- Values only 1–2 levels deep — pass directly

```kotlin
// ✅ Standard usage
val density = LocalDensity.current
val focusManager = LocalFocusManager.current

// ❌ Custom CompositionLocal for feature state — use explicit params
val LocalCustomerState = staticCompositionLocalOf<CustomerState> { error("Not provided") }
```

## Composable Extraction Guidelines

| Signal | Prefer |
|---|---|
| Reused in multiple places | Extract |
| Single clear visual/behavioral responsibility | Extract |
| Independent recomposition skipping helps performance | Extract |
| One-line wrapper around single `Text`/`Icon` | Don't extract |
| More parameters than inline clarity | Don't extract |
| Tightly coupled, reads clearer inline | Don't extract |
