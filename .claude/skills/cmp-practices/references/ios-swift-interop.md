# iOS Swift Interop

> **Project entry point:** `shared/src/iosMain/kotlin/MainViewController.kt` → `ComposeUIViewController { App() }`. SKIE recommended for new interop.

## Kotlin → Swift Naming

| Kotlin | Swift |
|---|---|
| Top-level function `fun foo()` in `Bar.kt` | `BarKt.foo()` |
| `object AppInit` | `AppInit.shared` |
| `companion object` member | Direct: `MyClass.value` |
| `sealed class UiState` | Class hierarchy (or SKIE exhaustive enum) |
| `suspend fun load()` | SKIE: `async func load()` |

## Nullability & Type Bridging

| Kotlin | Swift | Notes |
|---|---|---|
| `String` | `String` | Non-null direct |
| `String?` | `String?` | Optional direct |
| `Int` / `Long` | `Int32` / `Int64` | Not Swift `Int` — explicit cast required |
| `Unit` | `KotlinUnit` | Avoid in public iOS API |
| `List<T>` | `[T]` | Read-only copy — mutability lost at boundary |

Pass collections sparingly across the boundary — batch, don't iterate.

## Coroutines → Swift Async (SKIE)

SKIE converts `suspend` functions to Swift `async` automatically:

```kotlin
// commonMain / iosMain
suspend fun loadCustomers(): List<Customer> = repository.getAll()
```
```swift
let customers = try await viewModel.loadCustomers()   // SKIE-generated async bridge
```

## Flow → Swift Observation

### SKIE: Flow → AsyncSequence (recommended)

```swift
func observeState() async {
    for await state in viewModel.state {
        self.uiState = state
    }
}
```

### Manual StateFlow wrapper (without SKIE)

```kotlin
// iosMain
class IosStateCollector<T>(private val flow: StateFlow<T>, private val scope: CoroutineScope) {
    private var job: Job? = null
    fun observe(onChange: (T) -> Unit): () -> Unit {
        job = scope.launch(Dispatchers.Main) { flow.collect { onChange(it) } }
        return { job?.cancel() }
    }
}
```

Swift holds the returned cancel closure and invokes it in `deinit`.

## Sealed Classes in Swift

### Without SKIE — non-exhaustive (silent bugs)

```swift
if let success = state as? UiState.Success { render(success.items) }
// No compiler error if new sealed subclass added
```

### With SKIE — exhaustive (recommended)

```swift
switch onEnum(of: state) {
case .loading: showSpinner()
case .success(let s): render(items: s.items)
case .error(let e): showError(e.message)
}   // Compiler error if new sealed subclass added
```

### Edge cases

- **Generic sealed classes** — SKIE can't convert generics to Swift enums; use concrete types at the iOS boundary (e.g., `CustomerListResult` not `Result<List<Customer>>`)
- **Nested sealed hierarchies** — SKIE flattens names: `UiState.Error.Network` → `.errorNetwork`

## Compose in SwiftUI App

### Classic approach — `ComposeUIViewController`

```kotlin
// iosMain — entry point
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
```

```swift
// SwiftUI bridge via UIViewControllerRepresentable
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) { }
}
```

### CMP 1.11+ — `ComposeUIView` (new)

`ComposeUIView` embeds a Compose surface directly as a `UIView` — no `UIViewController` wrapper needed. Use this when you need to embed Compose inside an existing UIKit view hierarchy without adding a child view controller:

```kotlin
// iosMain
val composeView = ComposeUIView(content = { CustomerListScreen() })
parentView.addSubview(composeView)
```

> Use `ComposeUIViewController` for full-screen/navigation contexts. Use `ComposeUIView` for inline embedding within existing `UIView` hierarchies.

**CMP 1.11 also added:**
- Native iOS text input for `BasicTextField` — magnifier, native selection gestures, autofill
- Auto Layout sizing for `UIKitView` with remeasurement
- iOS: Dialog/Popup positioning now relative to view controller hierarchy (may shift existing UIs)

## Native iOS Views in Compose

```kotlin
// Embed UIKit view inside Compose (e.g., MKMapView, WKWebView)
UIKitView(
    factory = { MKMapView() },
    modifier = Modifier.fillMaxWidth().height(300.dp),
    update = { mapView -> mapView.setRegion(region, animated = true) }
)
// factory runs ONCE; put state-dependent updates in update, not factory
```

## iOS API Design Rules

- Use `internal` visibility + `@HiddenFromObjC` to keep Kotlin internals out of the generated ObjC header
- Avoid generics in public iOS-facing API — ObjC/Swift erases or boxes them
- Prefer data classes over deep class hierarchies at the boundary
- Minimize Kotlin↔Swift boundary crossings in hot paths — batch data, don't iterate

## Anti-Patterns

```kotlin
// ❌ Generic Result type exposed to Swift — SKIE can't convert
sealed class Result<T>   // → use concrete CustomerResult, OrderResult, etc.

// ❌ suspend fun returning Unit — becomes KotlinUnit in Swift
suspend fun refresh(): Unit   // avoid in public iOS API; return meaningful type or use callback

// ❌ Mutating Kotlin collections from Swift — mutations don't reflect
// ✅ Return immutable snapshots
```

```swift
// ❌ No cancellation cleanup — memory leak when VC deallocated
viewModel.state.collect { ... }

// ✅ Store cancel closure, call in deinit
private var cancel: (() -> Void)?
override func viewWillDisappear(_ animated: Bool) { cancel?() }
```
