# Navigation 3

> **Project usage:** Navigation3 alpha06 (`1.0.0-alpha06`). Routes implement `NavKey` via `@Serializable sealed interface`. Entry providers in `shared/src/commonMain/kotlin/com/ampairs/navigation/providers/`.

Reference: [Android Nav 3 docs](https://developer.android.com/guide/navigation/navigation-3)

## Core Architecture

Nav 3 has four building blocks:

1. **Keys** — `@Serializable` types identifying destinations
2. **Back stack** — a `SnapshotStateList` you own and mutate directly
3. **NavEntry** — wraps a key with composable content
4. **NavDisplay** — observes back stack, resolves keys via entry provider, renders

## Route Definition — Project Pattern

```kotlin
// Top-level routes (shared/src/commonMain/.../navigation/Routes.kt)
@Serializable
sealed interface Route : NavKey {
    @Serializable data object Workspace : Route
    @Serializable data object Customer : Route
    @Serializable data object Product : Route
    @Serializable data class FormConfig(val entityType: String = "") : Route
}

// Per-feature sub-routes
@Serializable
sealed interface CustomerRoute : NavKey {
    @Serializable data object CustomerList : CustomerRoute
    @Serializable data class CustomerDetail(val uid: String) : CustomerRoute
    @Serializable data object CustomerCreate : CustomerRoute
}
```

## Entry Provider Pattern — Project Pattern

```kotlin
// feature/customer/src/commonMain/.../CustomerEntryProvider.kt
class CustomerEntryProvider : NavEntryProvider {
    override fun NavEntry.Companion.provideEntry(key: NavKey): NavEntry? = when (key) {
        is CustomerRoute.CustomerList -> NavEntry(key) { CustomerListScreen() }
        is CustomerRoute.CustomerDetail -> NavEntry(key) { CustomerDetailScreen(uid = key.uid) }
        is CustomerRoute.CustomerCreate -> NavEntry(key) { CustomerCreateScreen() }
        else -> null
    }
}

// shared/src/commonMain/.../navigation/providers/CombinedEntryProvider.kt
val combinedEntryProvider = CustomerEntryProvider()
    .then(ProductEntryProvider())
    .then(WorkspaceEntryProvider())
    // ... other feature providers
```

**DO NOT pass ViewModels from entry providers.** Metro creates ViewModels — screens declare `viewModel: XxxViewModel = metroViewModel()`.

## Back Stack Manipulation

```kotlin
val backStack = rememberNavBackStack(Route.Workspace)

// Navigate forward
backStack.add(CustomerRoute.CustomerList)
backStack.add(CustomerRoute.CustomerDetail(uid = "CUS20240101..."))

// Navigate back
backStack.removeLastOrNull()

// Deep link — build synthetic back stack
backStack.clear()
backStack.addAll(listOf(Route.Workspace, CustomerRoute.CustomerList, CustomerRoute.CustomerDetail(uid)))

// Tabs — pop to root, swap root key
while (backStack.size > 1) backStack.removeLast()
backStack[0] = targetKey

// Replace current entry (e.g., redirect after auth)
backStack.removeLastOrNull()
backStack.add(Route.Workspace)
```

## ViewModel Entry Decorators

Always include both decorators in `NavDisplay`:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),   // preserves rememberSaveable while on stack
        rememberViewModelStoreNavEntryDecorator(),         // per-entry ViewModelStoreOwner
    ),
    entryProvider = combinedEntryProvider,
)
```

ViewModels are created when entry is added, cleared when popped.

## Scenes and Adaptive Layouts

### Dialog

```kotlin
entry<ConfirmDialog>(metadata = DialogSceneStrategy.dialog()) { key ->
    AlertDialog(onDismissRequest = { backStack.removeLastOrNull() }, /* ... */)
}
```

### Bottom Sheet

```kotlin
entry<FilterSheet>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
    FilterContent(onApply = { backStack.removeLastOrNull() })
}
```

### Material 3 Adaptive List-Detail

```kotlin
val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

NavDisplay(
    sceneStrategy = listDetailStrategy,
    entryProvider = entryProvider {
        entry<CustomerRoute.CustomerList>(
            metadata = ListDetailSceneStrategy.listPane(
                detailPlaceholder = { Text("Select a customer") }
            )
        ) { CustomerListScreen(onSelect = { backStack.add(CustomerRoute.CustomerDetail(it)) }) }

        entry<CustomerRoute.CustomerDetail>(
            metadata = ListDetailSceneStrategy.detailPane()
        ) { key ->
            CustomerDetailScreen(uid = key.uid)
        }
    },
)
```

### Chaining strategies

```kotlin
val strategy = dialogStrategy then bottomSheetStrategy then listDetailStrategy
// First match wins. SinglePaneSceneStrategy is always implicit fallback.
```

## Transitions

```kotlin
// Global on NavDisplay
NavDisplay(
    transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
    popTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
    // ...
)

// Per-entry override via metadata
entry<ModalRoute>(
    metadata = NavDisplay.transitionSpec {
        slideInVertically { it } togetherWith ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec {
        EnterTransition.None togetherWith slideOutVertically { it }
    }
) { ModalScreen() }
```

## Tab Navigation

```kotlin
@Stable
class NavigationState(val backStack: SnapshotStateList<NavKey>, val topLevelKeys: Set<NavKey>) {
    val currentKey: NavKey get() = backStack.last()
}

class Navigator(private val state: NavigationState) {
    fun navigate(key: NavKey) {
        if (key in state.topLevelKeys) {
            while (state.backStack.size > 1) state.backStack.removeLast()
            if (state.backStack.lastOrNull() != key) state.backStack[0] = key
        } else {
            state.backStack.add(key)
        }
    }
    fun goBack() { state.backStack.removeLastOrNull() }
}
```

## Deep Links

Nav 3 does not parse deep links — you own back stack construction.

```kotlin
// Android Activity or CMP entry point
LaunchedEffect(deepLinkUid) {
    if (deepLinkUid != null) {
        backStack.clear()
        backStack.addAll(listOf(Route.Workspace, CustomerRoute.CustomerList, CustomerRoute.CustomerDetail(deepLinkUid)))
    }
}
```

Register intent filters in `AndroidManifest.xml`; URL handlers in iOS `App.swift`.

## Anti-Patterns

```kotlin
// ❌ Old NavController API — does not exist in Navigation3
navController.navigate(CustomerRoute.CustomerList)
navController.popBackStack()

// ❌ Passing ViewModel from entry provider
NavEntry(key) { vm ->   // Metro auto-creates ViewModels
    CustomerListScreen(viewModel = vm)
}

// ❌ Not restoring back stack across config changes
val backStack = remember { mutableStateListOf<Any>(Route.Workspace) }   // lost on config change
// ✅
val backStack = rememberNavBackStack(Route.Workspace)
```
