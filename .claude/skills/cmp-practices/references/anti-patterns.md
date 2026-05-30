# Anti-Patterns — Cross-Cutting

Quick-reference table of patterns that hurt MVI Compose Multiplatform codebases.

## Cross-Cutting Anti-Patterns

| Anti-pattern | Why it is harmful | Better replacement |
|---|---|---|
| Business logic inside composables | forks source of truth, reruns during composition | move logic into ViewModel/domain |
| Giant god-ViewModel | blast radius too large, hard ownership | one ViewModel per screen or flow |
| Scattered `updateState`/`sendEffect` with no structure | state transitions hard to trace | disciplined `onEvent()` single entry point |
| Unstable state models (mutable collections, lambdas in state) | defeats Compose skipping | immutable data classes, `@Immutable` collections |
| Duplicated derived data (`total`, `formattedTotal`, `hasTotal` all stored) | bugs from drift, harder transitions | keep canonical value + computed property |
| Broad state reads in parent composables | recomposition cascades to all children | slice state, pass only required props |
| Mutable state passed deep into tree | hidden writes, unpredictable flow | explicit props + callbacks |
| One-off events stored as consumable state (`showSnackbarOnce = true`) | event replay on config change | separate `Effect` via `Channel` |
| `collectAsState()` instead of `collectAsStateWithLifecycle()` | emits while app is backgrounded | `collectAsStateWithLifecycle()` |
| Full-screen loading wipes existing content | bad UX, layout jumps | keep old content + inline refresh indicator |
| ViewModel doing platform work directly (share, analytics) | breaks testability | emit effects, handle in route composable |
| Animation state in ViewModel (`shakeCount`, `alpha`) | pollutes business state | local composable animation state |
| Display strings stored in ViewModel (pre-baked formatted text) | locale inflexibility | keep canonical values, resolve in UI |
| Poor lazy list keys (no key or index-based) | state jumps between rows, broken animations | stable key by domain UID |
| Too many trivial composables (wrappers around single `Text`) | fragmentation | extract only meaningful boundaries |
| `LocalAppGraph.current` inside any composable | breaks Metro DI contract | Metro-injected ViewModel only |
| `LocalContext.current` in commonMain | Android-only, breaks iOS/Desktop | expect/actual or param injection |
| `Dispatchers.IO` in commonMain | crashes on iOS (Kotlin/Native) | `Dispatchers.Default` or `DispatcherProvider.io` |
| Hardcoded UI strings in Kotlin source | non-localizable, violates KMP rule | `stringResource(Res.string.xxx)` |
| `java.*` / `android.*` imports in commonMain | compile failure on iOS/Wasm | KMP-compatible alternatives |

## Code Examples

### Business logic inside composables

```kotlin
// BAD — logic in composable; untestable, reruns on every recomposition
@Composable
fun CheckoutScreen(viewModel: CheckoutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val total = state.items.sumOf { it.price * it.qty }   // business logic here
    val tax = total * 0.08
    Text("Total: $total  Tax: $tax")
}

// GOOD — derive in ViewModel/state, composable only renders
data class CheckoutState(
    val items: List<LineItem> = emptyList(),
    val total: Double = 0.0,
    val tax: Double = 0.0,
)

@Composable
fun CheckoutScreen(state: CheckoutState, onEvent: (CheckoutEvent) -> Unit) {
    Text("Total: ${state.total}  Tax: ${state.tax}")
}
```

### One-off events as consumable state booleans

```kotlin
// BAD — event replays on config change, race between read and reset
data class UiState(val showSnackbar: Boolean = false)

LaunchedEffect(state.showSnackbar) {
    if (state.showSnackbar) {
        snackbarHostState.showSnackbar("Saved")
        viewModel.onEvent(DismissSnackbar)   // consumer must remember to reset
    }
}

// GOOD — Channel delivers exactly once
sealed interface Effect { data class ShowSnackbar(val msg: String) : Effect }

LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
        when (effect) {
            is Effect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.msg)
        }
    }
}
```

### Unstable state models

```kotlin
// BAD — MutableList and lambda in data class defeat Compose skipping
data class CustomerRowState(
    val uid: String,
    val name: String,
    val tags: MutableList<String>,   // unstable
    val onClick: () -> Unit,         // lambda in data class
)

// GOOD
@Immutable
data class CustomerRowUi(val uid: String, val name: String, val tags: List<String>)

@Composable
fun CustomerList(items: List<CustomerRowUi>, onItemClick: (String) -> Unit) {
    LazyColumn {
        items(items, key = { it.uid }) { item ->
            val onClick = remember(item.uid, onItemClick) { { onItemClick(item.uid) } }
            CustomerRow(item = item, onClick = onClick)
        }
    }
}
```
